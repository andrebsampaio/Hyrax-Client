package edu.thesis.fct.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class WebImageView extends ImageView {

    private Drawable placeholder, image;

    public WebImageView(Context context) {
        super(context);
    }
    public WebImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public WebImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); //Snap to width
    }

    public void setPlaceholderImage(Drawable drawable) {
        placeholder = drawable;
        if (image == null) {
            setImageDrawable(placeholder);
        }
    }
    public void setPlaceholderImage(int resid) {
        placeholder = getResources().getDrawable(resid);
        if (image == null) {
            setImageDrawable(placeholder);
        }
    }

    public void setImageUrl(String url) {
        DownloadTask task = new DownloadTask();
        task.execute(url);
    }

    private class DownloadTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            System.out.println(url);
            try {
                URLConnection conn = (new URL(url)).openConnection();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                //We create an array of bytes
                byte[] data = new byte[50];
                int current = 0;

                while((current = bis.read(data,0,data.length)) != -1){
                    buffer.write(data, 0, current);
                }

                byte[] imageData = buffer.toByteArray();
                return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            image = new BitmapDrawable(getContext().getResources(), result);
            if (image != null) {
                setImageDrawable(image);
            }
        }
    }
}
