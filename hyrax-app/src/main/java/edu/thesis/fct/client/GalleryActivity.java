package edu.thesis.fct.client;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.thesis.fct.bluedirect.BluedirectAPI;
import edu.thesis.fct.client.face_processing.FaceRecognitionAsync;

public class GalleryActivity extends AppCompatActivity {

    private static final long MAX_FILE_SIZE = Long.MAX_VALUE ;
    public static final String HYRAX_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Hyrax";
    List<Object> ids = new ArrayList<>();
    RecyclerView recyclerView;
    Activity activity;
    String searchURL;
    String imagesURL;
    String registrationURL;
    static GalleryAdapter mAdapter;
    List<File> takenPhotos;
    boolean registration;
    SharedPreferences pref;
    Context context;
    int trainWidth = 0; int trainHeight = 0;
    final static String lineEnd = "\r\n";
    final static String twoHyphens = "--";
    final static String boundary = "*****";
    AlertDialog dialog;
    final String mimeType = "multipart/form-data;boundary=" + boundary;
    public static ProgressDialog progressDialog;
    String user;
    private static final int MIN_PHOTOS = 8;
    static boolean isVisible = false;
    public static InstrumentationUtils utils;

    public static Handler UIHandler;

    static
    {
        UIHandler = new Handler(Looper.getMainLooper());
    }
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Gallery");
        Bundle b=this.getIntent().getExtras();
        registration = b.getBoolean("isRegistration");
        activity = this;
        setContentView(R.layout.gallery_layout);
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarGallery);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
        isVisible = true;
        context = this;

        if (registration){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage("Our face recognition algorithm needs to learn your face, " +
                    "in order to provide a reliable facial identification.\nPlease choose photos with different face poses")
                    .setTitle("Please choose 8 photos");

            dialog = builder.create();

            dialog.show();
            takenPhotos = new ArrayList<>();
            if (registration){
                String[] array=b.getStringArray("images_path");
                if (array != null){
                    for (String s : array){
                        takenPhotos.add(new File(s));
                    }
                }
            }

            user = b.getString("username");
        } else {
            pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
            user = pref.getString("username", null);
        }

        NetworkInfoHolder nih = NetworkInfoHolder.getInstance();
        Log.d("NIH", nih.getHost() + "");
        if (nih.getHost() != null){
            searchURL = "http://" + nih.getHost().getHostAddress()  + ":" + nih.getPort()  + "/hyrax-server/rest/search/";
            registrationURL = "http://" + nih.getHost().getHostAddress()  + ":" + nih.getPort()  + "/hyrax-server/rest/register/";
            imagesURL = "http://" + nih.getHost().getHostAddress()  + ":" + nih.getPort()  + "/hyrax-server/rest/images/";
        }

        recyclerView = (RecyclerView) findViewById(R.id.image_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, 3));
        recyclerView.setHasFixedSize(true); // Helps improve performance

        if (registration){
            mAdapter = new GalleryAdapter(activity,null,takenPhotos, true);
        } else{
            mAdapter = new GalleryAdapter(activity, null,getHyraxPhotos(), false);
        }

        recyclerView.setAdapter(mAdapter);
    }

    public static boolean isVisible(){
        return isVisible;
    }

    public static void visible(boolean v){
        isVisible = v;
    }

    public static void updateGallery(File f){
        mAdapter.addData(f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (registration){
            getMenuInflater().inflate(R.menu.menu_gallery_registration, menu);
        } else getMenuInflater().inflate(R.menu.menu_gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.allSelected:
                if (mAdapter.getSelectedImages().size() == MIN_PHOTOS){
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setCancelable(false);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setMessage("Processing your face :)");
                    progressDialog.show();
                    sendRegistration(registrationURL);
                } else {
                    Toast.makeText(this, "Please select " + (MIN_PHOTOS - mAdapter.getSelectedImages().size()) + " more photos", Toast.LENGTH_LONG).show();
                }
                System.out.println(mAdapter.getSelectedImages().size());
                return true;
            case R.id.recognitionExplanation:
                dialog.show();
                return true;
            case R.id.searchPictures:
                this.searchMyFace();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        GalleryActivity.visible(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GalleryActivity.visible(true);
    }

    private void sendRegistration(String url) {
        List<File> images = mAdapter.getSelectedImages();
        File zippedImages = zip(images, Environment.getExternalStorageDirectory() + File.separator +  user + ".zip");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            buildPart(dos, read(zippedImages),  user + ".zip");
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            byte [] multipartBody = bos.toByteArray();

            MultipartRequest multipartRequest = new MultipartRequest(url, null, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    Toast.makeText(getParent(), "Processing done, enjoy!", Toast.LENGTH_LONG).show();
                    System.out.println(new String (response.data));
                    //Intent myIntent = new Intent(getParent(), MainActivity.class);
                    pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("username", user);
                    editor.commit();
                    progressDialog.dismiss();
                    //startActivity(myIntent);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println(error.toString());
                }
            });

            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(180000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            MySingleton.getInstance(this).addToRequestQueue(multipartRequest);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public byte[] read(File file) throws Exception {
        if (file.length() > MAX_FILE_SIZE) {
            throw new Exception();
        }

        byte[] buffer = new byte[(int) file.length()];
        InputStream ios = null;
        try {
            ios = new FileInputStream(file);
            if (ios.read(buffer) == -1) {
                throw new IOException(
                        "EOF reached while trying to read the whole file");
            }
        } finally {
            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return buffer;
    }


    private static File zip(List<File> files, String filename) {
        File zipfile = new File(filename);
        // Create a buffer for reading the files
        byte[] buf = new byte[1024];
        try {
            // create the ZIP file
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
            // compress the files
            for(int i=0; i<files.size(); i++) {
                FileInputStream in = new FileInputStream(files.get(i));
                // add ZIP entry to output stream
                out.putNextEntry(new ZipEntry(files.get(i).getName()));
                // transfer bytes from the file to the ZIP file
                int len;
                while((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                // complete the entry
                out.closeEntry();
                in.close();
            }
            // complete the ZIP file
            out.close();
            return zipfile;
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return null;
    }

    private void searchMyFace(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Searching for your photos");
        progressDialog.show();
        new GetImagesTask().execute();
    }


    private List<File> getHyraxPhotos(){
        File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "Hyrax");
        file.mkdir();
        List<File> res = new ArrayList<>();
        for (File f : file.listFiles()){
            if (f.getName().equals("listing")) continue;
            res.add(new File(f.getAbsolutePath() + File.separator + f.getName() + ".jpg"));
        }
        return res;
    }

    private class GetImagesTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... s) {
            utils = new InstrumentationUtils(context);
            utils.startTest();
            BluedirectAPI.broadcastQuery(user,new File(FaceRecognitionAsync.SAVE_PATH),null,context,BluedirectAPI.FANOUT, BluedirectAPI.BROADCAST);
            return null;
        }

        protected void onProgressUpdate(Void... progress) {

        }
    }

    private void buildPart(DataOutputStream dataOutputStream, byte[] fileData, String fileName) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\""
                + fileName + "\"" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
        int bytesAvailable = fileInputStream.available();

        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        // read file and write it into form...
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        dataOutputStream.writeBytes(lineEnd);
    }

}
