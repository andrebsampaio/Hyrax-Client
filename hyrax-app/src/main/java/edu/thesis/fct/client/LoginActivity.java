package edu.thesis.fct.client;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.yalantis.cameramodule.ManagerInitializer;
import com.yalantis.cameramodule.activity.CameraActivity;
import com.yalantis.cameramodule.interfaces.PhotoSavedListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


import edu.thesis.fct.bluedirect.BluedirectActivity;
import edu.thesis.fct.bluedirect.BluedirectAPI;
import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.bluedirect.router.MeshNetworkManager;
import edu.thesis.fct.client.config.Configurations;

import static org.bytedeco.javacpp.flandmark.flandmark_init;

public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int RESULT_OK = 0;
    private static final int MIN_PHOTOS = 1;
    private Context context;
    private static EditText username;
    private PhotosObserver photoObserver  = new PhotosObserver();
    private List<File> takenPhotos = new ArrayList<File>();
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.ResolveListener mResolveListener;
    NsdManager mNsdManager;
    int port;
    InetAddress host;
    String loginURL;
    String user;
    boolean isRemote = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        context = this;
        Button register = (Button) this.findViewById(R.id.registerButton);
        Button newSession = (Button) this.findViewById(R.id.newsession);
        Button login = (Button) this.findViewById(R.id.loginButton);
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
                        mNsdManager.discoverServices(Configurations.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
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

        BluedirectAPI.setOnGroupJoinedListener(new MeshNetworkManager.onGroupJoinedListener() {
            @Override
            public void onGroupJoined() {
                advanceFromLogin(context);
            }
        });

        CameraActivity.setOnGalleryClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(context, GalleryActivity.class);
                Bundle b=new Bundle();
                b.putBoolean(Configurations.KEYS.REGISTRATION.toString(), false);
                myIntent.putExtras(b);
                startActivity(myIntent);
            }
        });

        CameraActivity.addPhotoSavedListener(new PhotoSavedListener() {
            @Override
            public void photoSaved(String s, String s1) {
                //new FaceRecognitionAsync(new File(s),(Activity)context).execute();
            }
        });


        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isRemote) setLoginURL();
                String name = username.getText().toString();
                if (name.equals("")){
                    Toast.makeText(context, "Please insert a username", Toast.LENGTH_LONG ).show();
                }  else {
                    saveUsername(name);
                   // checkUsername(name.toLowerCase(), loginURL, true);

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
                    //checkUsername(name.toLowerCase(), loginURL, false);
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

    public static void advanceFromLogin(Context context){

        //new FaceRecognitionAsync(new File(Environment.getExternalStorageDirectory(), "HyraxTrain"),(Activity)context).execute(true);

        loadImagesFromStorage();

        loadCamera(context);
    }

    private static void loadImagesFromStorage() {
        Thread t = new Thread() {
            public void run() {
                try {
                    Map<Integer,ImageModel> tmp = new HashMap<>();
                    Scanner scanner = new Scanner(ListingSingleton.listing);
                    while (scanner.hasNext()){
                        String s = scanner.next();
                        ImageModel i = ImageModel.fromString(s);
                        tmp.put(i.getId(), i);
                    }
                    ListingSingleton.getInstance().setImages(tmp);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();

    }

    private static void loadCamera(Context context){
        ManagerInitializer.i.init(context);
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra(CameraActivity.PATH, Environment.getExternalStorageDirectory().getPath());
        intent.putExtra(CameraActivity.OPEN_PHOTO_PREVIEW, false);
        intent.putExtra(CameraActivity.USE_FRONT_CAMERA, false);
        context.startActivity(intent);
    }

    private void setLoginURL(){
        String ip = ((EditText) this.findViewById(R.id.remoteip)).getText().toString();
        String port = ((EditText) this.findViewById(R.id.remoteport)).getText().toString();

        try {
            InetAddress address = InetAddress.getByName(ip);
            NetworkInfoHolder.getInstance().setData(address);
            NetworkInfoHolder.getInstance().setPort(Integer.parseInt(port));
            loginURL = Configurations.getActionURL(Configurations.ACTION.LOGIN);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }



    }

    private void saveUsername(final String text){
        Intent myIntent = new Intent(context, BluedirectActivity.class);
        Configurations.setUsername(context,text);
        startActivity(myIntent);
    }

    private void openCamera(){
        Intent intent = new Intent(
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        startActivityForResult(intent, REQUEST_CAMERA);
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
                    for (int i = 0; i < takenPhotos.size(); i++){
                        photosPath[i] = takenPhotos.get(i).getAbsolutePath();
                        i++;
                    }
                    b.putBoolean(Configurations.KEYS.REGISTRATION.toString(), true);
                    b.putStringArray(Configurations.KEYS.IMAGE_PATH.toString(), photosPath);
                    b.putString(Configurations.KEYS.USERNAME.toString(), user);
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
                Log.d(Configurations.NSD_TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(Configurations.NSD_TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(Configurations.SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(Configurations.NSD_TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(Configurations.SERVICE_NAME)) {
                    mNsdManager.resolveService(service, mResolveListener);
                    Log.d(Configurations.NSD_TAG, "Same machine: " + Configurations.SERVICE_NAME);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(Configurations.NSD_TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(Configurations.NSD_TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(Configurations.NSD_TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(Configurations.NSD_TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(Configurations.NSD_TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(Configurations.NSD_TAG, "Resolve Succeeded. " + serviceInfo);
                port = serviceInfo.getPort();
                host = serviceInfo.getHost();
                NetworkInfoHolder.getInstance().setData(host);
                NetworkInfoHolder.getInstance().setPort(port);
                Log.d(Configurations.NSD_TAG,  NetworkInfoHolder.getInstance().getHost().getHostAddress());
                Configurations.storeURL(context,Configurations.buildURL());
                Toast.makeText(context,"Server detected",Toast.LENGTH_SHORT).show();

                mNsdManager.stopServiceDiscovery(mDiscoveryListener);

                if (serviceInfo.getServiceName().equals(Configurations.SERVICE_NAME)) {
                    Log.d(Configurations.NSD_TAG, "Same IP.");
                    return;
                }
            }
        };
    }



}
