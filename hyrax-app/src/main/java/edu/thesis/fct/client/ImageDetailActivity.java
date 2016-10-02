package edu.thesis.fct.client;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import edu.thesis.fct.client.config.Configurations;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by abs on 28-04-2016.
 */
public class ImageDetailActivity extends AppCompatActivity {
    String imagePath;
    String location;
    String time;
    ProgressDialog progressDialog;
    String untagURL;
    String username;
    int imageId;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_layout);
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarImageDetail);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);

        context = this;

        imagePath = getIntent().getStringExtra("image");
        File file = new File(imagePath);
        username = Configurations.getUsername(context);

        toolbar.setTitle(file.getName().split("\\.")[0]);

        Glide.with(this).load(new File(imagePath))
                .thumbnail(0.5f)
                .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into((PhotoView) this.findViewById(R.id.iv_photo));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.downloadPicture:
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Download in progress");
                progressDialog.show();
                String path = Environment.getExternalStorageDirectory() + File.separator + "Hyrax" +
                        File.separator + location + time + File.separator + location + time + ".jpg";
                new DownloadFilesTask().execute(new File(path), imagePath, this);
                break;
            case R.id.notMe:
                new AlertDialog.Builder(this)
                        .setTitle("It's not you?")
                        .setMessage("Are you sure it is not you in the picture?\nThis picture will not appear in your searches again")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                untagMe();

                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .show();
                break;
        }
        return true;
    }

    private void untagMe(){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Configurations.getActionURL(Configurations.ACTION.UNTAG),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (Boolean.valueOf(response)){
                            Toast.makeText(context, "Tag removed", Toast.LENGTH_LONG ).show();
                        } else {
                            Toast.makeText(context, "An error ocurred, try again", Toast.LENGTH_LONG ).show();
                        }
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
                params.put("username", username);
                params.put("picture_id", String.valueOf(imageId) );
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
    }

    private boolean saveImage(File path, String urlPath){
        if (path.exists()) return false;
        if (!path.getParentFile().exists()) {
            path.getParentFile().mkdirs();
        }
        try{
            int count;
            URL url = new URL(urlPath);
            url.openConnection().connect();
            InputStream input = new BufferedInputStream(url.openStream());

            // Output stream
            OutputStream output = new FileOutputStream(path);

            byte data[] = new byte[1024];

            while ((count = input.read(data)) != -1) {
                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();


        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private class DownloadFilesTask extends AsyncTask<Object, Integer, Boolean> {
        File path;
        String url;
        Context context;


        protected Boolean doInBackground(Object... obj) {
            path = (File)obj[0];
            url = (String) obj[1];
            context = (Context) obj[2];

            return saveImage(path, url);

        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss();
            if (result){
                Toast.makeText(context, "Image stored in " + url, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Image already downloaded", Toast.LENGTH_LONG).show();
            }


        }


    }


}