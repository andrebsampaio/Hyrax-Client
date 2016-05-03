package edu.thesis.fct.client;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by abs on 28-04-2016.
 */
public class ImageDetailActivity extends AppCompatActivity {
    String imageURL;
    String location;
    String time;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_layout);
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarImageDetail);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);

        imageURL = getIntent().getStringExtra("image");
        location = getIntent().getStringExtra("location");
        time = getIntent().getStringExtra("time");

        toolbar.setTitle(location + time);


        Glide.with(this).load(imageURL)
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
                new DownloadFilesTask().execute(new File(path), imageURL, this);
                break;
        }
        return true;
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