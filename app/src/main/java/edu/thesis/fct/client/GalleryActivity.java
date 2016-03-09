package edu.thesis.fct.client;

import android.app.Activity;
import android.app.Fragment;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends Activity {

    List<Integer> ids;
    RecyclerView recyclerView;
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Gallery");
        Intent intent = getIntent();
        String value = intent.getStringExtra("face");
        activity = this;
        setContentView(R.layout.gallery_layout);
        String url = "http://192.168.1.243:8080/hyrax-server/rest/images";
        getImages(url);
    }

    private void getImages(String url){
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ids = new ArrayList<>();
                            JSONArray ja = response.getJSONArray("imageDAO");
                            for (int i = 0; i < ja.length(); i++){
                                JSONObject jsonObject = ja.getJSONObject(i);
                                ids.add(Integer.parseInt(jsonObject.optString("id").toString()));
                            }
                            recyclerView = (RecyclerView) findViewById(R.id.image_grid);
                            recyclerView.setLayoutManager(new GridLayoutManager(activity, 3));
                            recyclerView.setHasFixedSize(true); // Helps improve performance
                            GalleryAdapter mAdapter = new GalleryAdapter(activity, ids);
                            recyclerView.setAdapter(mAdapter);

                        } catch (JSONException e) {
                            e.printStackTrace();
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


}
