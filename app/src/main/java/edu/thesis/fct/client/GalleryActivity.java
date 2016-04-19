package edu.thesis.fct.client;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryActivity extends Activity {

    List<Object> ids;
    RecyclerView recyclerView;
    Activity activity;
    String imagesURL;
    GalleryAdapter mAdapter;
    int trainWidth = 0; int trainHeight = 0;
    final static String lineEnd = "\r\n";
    final static String twoHyphens = "--";
    final static String boundary = "*****";
    final String mimeType = "multipart/form-data;boundary=" + boundary;
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Gallery");
        Intent intent = getIntent();
        String value = intent.getStringExtra("face");
        activity = this;
        setContentView(R.layout.gallery_layout);

        Bundle b=this.getIntent().getExtras();
        String[] array=b.getStringArray("images_path");
        List<File> takenPhotos = new ArrayList<>();
        if (array != null){
            for (String s : array){
                takenPhotos.add(new File(s));
            }
        }

        NetworkInfoHolder nih = NetworkInfoHolder.getInstance();
        Log.d("NIH", nih.getHost() + "");
        if (nih.getHost() != null){
            imagesURL = "http://" + nih.getHost().getHostAddress()  + ":" + nih.getPort()  + "/hyrax-server/rest/search/";
        }

        recyclerView = (RecyclerView) findViewById(R.id.image_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, 3));
        recyclerView.setHasFixedSize(true); // Helps improve performance
        mAdapter = new GalleryAdapter(activity,imagesURL,takenPhotos);
        recyclerView.setAdapter(mAdapter);
        progressDialog = new ProgressDialog(this);
        //searchMyFace2(imagesURL);
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

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("ZEBUM");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("ZEBUM");
                    }
                }){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "multipart/form-data");
                return headers;
            }

            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                if (trainHeight == 0 || trainWidth == 0){
                    File firstImage = (new File (Environment.getExternalStorageDirectory() + "/trainpaio")).listFiles()[0];
                    Bitmap b = BitmapFactory.decodeFile(firstImage.getAbsolutePath());
                    trainWidth = b.getWidth();
                    trainHeight = b.getHeight();
                }
                params.put("train_width", String.valueOf(trainWidth));
                params.put("train_height", String.valueOf(trainHeight));
                return params;
            }

        };

        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
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
