package edu.thesis.fct.client;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GalleryActivity extends AppCompatActivity {

    private static final long MAX_FILE_SIZE = Long.MAX_VALUE ;
    List<Object> ids = new ArrayList<>();
    RecyclerView recyclerView;
    Activity activity;
    String searchURL;
    String imagesURL;
    String registrationURL;
    GalleryAdapter mAdapter;
    List<File> takenPhotos;
    boolean registration;
    SharedPreferences pref;
    int trainWidth = 0; int trainHeight = 0;
    final static String lineEnd = "\r\n";
    final static String twoHyphens = "--";
    final static String boundary = "*****";
    AlertDialog dialog;
    final String mimeType = "multipart/form-data;boundary=" + boundary;
    ProgressDialog progressDialog;
    String user;
    InstrumentationUtils iu;
    boolean first = true;
    private static final int MIN_PHOTOS = 8;


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
            mAdapter = new GalleryAdapter(activity,null,takenPhotos,iu);
        } else{
            iu = new InstrumentationUtils(this);
            mAdapter = new GalleryAdapter(activity, imagesURL,null,iu);
            iu.startTest();
            searchMyFace(searchURL);
        }

        recyclerView.setAdapter(mAdapter);
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
                this.searchMyFace(searchURL);
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    Intent myIntent = new Intent(getParent(), MainActivity.class);
                    pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("username", user);
                    editor.commit();
                    progressDialog.dismiss();
                    startActivity(myIntent);
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

    private void searchMyFace2(String url){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            if (trainHeight == 0 || trainWidth == 0){
                File firstImage = (new File (Environment.getExternalStorageDirectory() + "/trainpaio")).listFiles()[0];
                Bitmap b = BitmapFactory.decodeFile(firstImage.getAbsolutePath());
                trainWidth = b.getWidth();
                trainHeight = b.getHeight();
            }
            buildTextPart(dos, "train_width", String.valueOf(trainWidth));
            buildTextPart(dos, "train_height", String.valueOf(trainHeight));
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            byte [] multipartBody = bos.toByteArray();

            MultipartRequest multipartRequest = new MultipartRequest(url, null, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    if (response == null){
                        Toast.makeText(getParent(), "No pictures found of you", Toast.LENGTH_LONG).show();
                    } else {
                        System.out.println(new String (response.data));
                    }
                    progressDialog.dismiss();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println(error.toString());
                }
            });

            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(180000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            MySingleton.getInstance(this).addToRequestQueue(multipartRequest);
            progressDialog.setMessage("Searching for your photos");
            progressDialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void searchMyFace(String url){
        if (first){
            first = false;
        } else {
            iu = new InstrumentationUtils(this);
            mAdapter.setIU(iu);
            iu.startTest();
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Searching for your photos");
        progressDialog.show();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        iu.calculateLatency(InstrumentationUtils.IMG_LIST_RQ);
                        if (response == null){
                            Toast.makeText(getParent(), "No pictures found of you", Toast.LENGTH_LONG).show();
                        } else {
                            JSONObject JSONresp;
                            ids = new ArrayList<>();
                            try {

                                JSONresp = new JSONObject(response);
                                JSONObject object = JSONresp.getJSONObject("imageDAO");
                                int id = object.optInt("id");
                                String location = object.optString("location");
                                String time = object.optString("time");
                                ids.add(new ImageModel(id, location, time));
                                mAdapter.setData(ids);
                            } catch (JSONException e1) {
                                try {
                                    JSONresp = new JSONObject(response);
                                    JSONArray ja = JSONresp.getJSONArray("imageDAO");
                                    for (int i = 0; i < ja.length(); i++) {
                                        JSONObject jsonObject = ja.getJSONObject(i);
                                        int id = jsonObject.optInt("id");
                                        String location = jsonObject.optString("location");
                                        String time = jsonObject.optString("time");
                                        ids.add(new ImageModel(id, location, time));
                                    }
                                    mAdapter.setData(ids);

                                } catch (JSONException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                        progressDialog.dismiss();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error.networkResponse.toString());
                    }
                }){

            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("person_name", user);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }

        };

        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
        iu.registerLatency(InstrumentationUtils.IMG_LIST_RQ);
    }

    private void getImages(String url){
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        ids = new ArrayList<Object>();
                        try {
                            JSONObject object = response.getJSONObject("imageDAO");
                            ids.add(object.optInt("id"));
                            mAdapter.setData(ids);
                        }
                        catch (JSONException e1) {
                            try {
                                JSONArray ja = response.getJSONArray("imageDAO");
                                for (int i = 0; i < ja.length(); i++){
                                    JSONObject jsonObject = ja.getJSONObject(i);
                                    ids.add(Integer.parseInt(jsonObject.optString("id").toString()));
                                }
                                mAdapter.setData(ids);

                            } catch (JSONException e2) {
                                e2.printStackTrace();
                            }
                        }

                        System.out.println("Response: " + response.toString());

                    }




                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });

        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }


    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + lineEnd);
        dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);
        dataOutputStream.writeBytes(parameterValue + lineEnd);
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
