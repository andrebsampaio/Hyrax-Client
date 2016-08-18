package edu.thesis.fct.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Camera;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.yalantis.cameramodule.ManagerInitializer;
import com.yalantis.cameramodule.activity.CameraActivity;
import com.yalantis.cameramodule.interfaces.PhotoSavedListener;

import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;

public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int RESULT_OK = 0;
    private static final int MIN_PHOTOS = 1;
    private Context context;
    private static EditText username;
    private PhotosObserver photoObserver  = new PhotosObserver();
    private List<File> takenPhotos = new ArrayList<File>();
    static final String TAG = "NSD_DISCOVER";
    static final String SERVICE_TYPE = "_http._tcp.";
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.ResolveListener mResolveListener;
    NsdManager mNsdManager;
    int port;
    InetAddress host;
    String mServiceName = "hyrax";
    String loginURL;
    String user;
    boolean isRemote = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        context = this;
        Button login = (Button) this.findViewById(R.id.loginButton);
        Button register = (Button) this.findViewById(R.id.registerButton);
        Button newSession = (Button) this.findViewById(R.id.newsession);
        username = (EditText) this.findViewById(R.id.username);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        new AlertDialog.Builder(context)
                .setTitle("Local or Remote Server")
                .setMessage("If the server is remote, you have to insert the IP and Port")
                .setPositiveButton("Local", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
                        initializeResolveListener();
                        initializeDiscoveryListener();
                        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                    }
                })
                .setNegativeButton("Remote", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        isRemote = true;
                        LoginActivity.this.findViewById(R.id.remoteip).setVisibility(View.VISIBLE);
                        LoginActivity.this.findViewById(R.id.remoteport).setVisibility(View.VISIBLE);
                    }
                })
                .show();

        newSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int session = InstrumentationUtils.newTestSession(context);
                Toast.makeText(context, "Started log in file " + session, Toast.LENGTH_LONG ).show();

            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isRemote) setLoginURL();
                String name = username.getText().toString();
                if (name.equals("")){
                    Toast.makeText(context, "Please insert a username", Toast.LENGTH_LONG ).show();
                }  else {
                    checkUsername(name.toLowerCase(), loginURL, true);
                }
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isRemote) setLoginURL();
                String name = username.getText().toString();
                if (name.equals("")){
                    Toast.makeText(context, "Please insert a username", Toast.LENGTH_LONG ).show();
                }  else {
                    checkUsername(name.toLowerCase(), loginURL, false);
                }
            }
        });

        CameraActivity.setOnGalleryClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(context, GalleryActivity.class);
                Bundle b=new Bundle();
                b.putBoolean("isRegistration", false);
                myIntent.putExtras(b);
                startActivity(myIntent);
            }
        });

        CameraActivity.addPhotoSavedListener(new PhotoSavedListener() {
            @Override
            public void photoSaved(String s, String s1) {
                if(CameraActivity.autoUploadStatus){
                    Log.d("UPLOAD", "PROCESSING IMAGE AND UPLOAD");
                    new UploadImageTask().execute(getCallingActivity(), new File(s));
                }
            }
        });

        CameraActivity.setOnAutoUploadClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView indicator = (TextView)v.findViewById(R.id.uploadIndicator);
                if (indicator.getText().equals("AUTO")){
                    CameraActivity.autoUploadStatus = false;
                    indicator.setText("OFF");
                } else {
                    CameraActivity.autoUploadStatus = true;
                    indicator.setText("AUTO");
                }
            }
        });

        this.getApplicationContext()
                .getContentResolver()
                .registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false,
                        photoObserver);
        Log.d("INSTANT", "registered content observer");
    }

    private void openCameraReg(){
        Intent intent = new Intent(
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    class UploadAsync implements Runnable {
        File name;
        Activity act;
        UploadAsync(Activity act, File name){
            this.name = name;
            this.act = act;
        }
        public void run(){

        }
    }

    private void setLoginURL(){
        String ip = ((EditText) this.findViewById(R.id.remoteip)).getText().toString();
        String port = ((EditText) this.findViewById(R.id.remoteport)).getText().toString();

        try {
            InetAddress address = InetAddress.getByName(ip);
            NetworkInfoHolder.getInstance().setData(address);
            NetworkInfoHolder.getInstance().setPort(Integer.parseInt(port));
            loginURL = "http://" + address.getHostAddress()  + ":" + port  + "/hyrax-server/rest/checkuser/";
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }



    }

    private void checkUsername(final String text, String url, final boolean isLogin) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        if (loginURL == null){
            Toast.makeText(context, "There was a problem connecting the servers, try again later", Toast.LENGTH_LONG ).show();
        } else {
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if (Boolean.valueOf(response)){
                                if (isLogin){
                                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putString("username", text);
                                    editor.commit();
                                    openCamera();
                                } else {
                                    Toast.makeText(context, "User already exists", Toast.LENGTH_LONG ).show();
                                }
                            } else {
                                if (isLogin){
                                    Toast.makeText(context, "User does not exist", Toast.LENGTH_LONG ).show();
                                } else {
                                    user = text;
                                    openCameraReg();
                                }
                            }

                            progressDialog.dismiss();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println(error.toString());
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("username", text);
                    return params;
                }
            };
            Volley.newRequestQueue(this).add(stringRequest);

            progressDialog.setMessage("Processing...");
            progressDialog.show();
        }

    }

    private void openCamera(){
        ManagerInitializer.i.init(context);
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra(CameraActivity.PATH, Environment.getExternalStorageDirectory().getPath());
        intent.putExtra(CameraActivity.OPEN_PHOTO_PREVIEW, false);
        intent.putExtra(CameraActivity.USE_FRONT_CAMERA, false);
        intent.putExtra(CameraActivity.UPLOAD, false);
        context.startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getApplicationContext().getContentResolver()
                .unregisterContentObserver(photoObserver);
        Log.d("INSTANT", "unregistered content observer");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                if (takenPhotos.size() < MIN_PHOTOS){
                    Toast.makeText(this, "Please take at least " +  String.valueOf(MIN_PHOTOS - takenPhotos.size())  + " more photos" , Toast.LENGTH_LONG).show();
                    openCamera();
                } else {
                    Intent myIntent = new Intent(context, GalleryActivity.class);
                    Bundle b=new Bundle();
                    String [] photosPath = new String [takenPhotos.size()];
                    int i = 0;
                    for (File f : takenPhotos){
                        photosPath[i] = takenPhotos.get(i).getAbsolutePath();
                        i++;
                    }
                    b.putBoolean("isRegistration", true);
                    b.putStringArray("images_path", photosPath);
                    b.putString("username", user);
                    myIntent.putExtras(b);
                    startActivity(myIntent);
                }
            }
        }
    }

    private class PhotosObserver extends ContentObserver {

        public PhotosObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Media media = readFromMediaStore(getApplicationContext(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (!takenPhotos.contains(media.getFile())){
                takenPhotos.add(media.getFile());
            }
            Log.d("INSTANT", "detected picture " + takenPhotos.size());
        }
    }

    private Media readFromMediaStore(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null,
                null, "date_added DESC");
        Media media = null;
        if (cursor.moveToNext()) {
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            String filePath = cursor.getString(dataColumn);
            int mimeTypeColumn = cursor
                    .getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            String mimeType = cursor.getString(mimeTypeColumn);
            media = new Media(new File(filePath), mimeType);
        }
        cursor.close();
        return media;
    }

    private class Media {
        private File file;
        @SuppressWarnings("unused")
        private String type;

        public Media(File file, String type) {
            this.file = file;
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public File getFile() {
            return file;
        }
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    mNsdManager.resolveService(service, mResolveListener);
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("hyrax")){
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                port = serviceInfo.getPort();
                host = serviceInfo.getHost();
                NetworkInfoHolder.getInstance().setData(host);
                NetworkInfoHolder.getInstance().setPort(port);
                loginURL = "http://" + host.getHostAddress()  + ":" + port  + "/hyrax-server/rest/checkuser/";
                Log.d(TAG,  NetworkInfoHolder.getInstance().getHost().getHostAddress());
                CharSequence detected =  "Server detected";
                Toast toast = Toast.makeText(context,detected,Toast.LENGTH_SHORT);
                toast.show();

                mNsdManager.stopServiceDiscovery(mDiscoveryListener);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
            }
        };
    }



}
