package edu.thesis.fct.client;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.Duration;

public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int RESULT_OK = 0;
    private static final int MIN_PHOTOS = 1;
    private Context context;
    private static EditText username;
    private PhotosObserver photoObserver  = new PhotosObserver();
    private List<File> takenPhotos = new ArrayList<File>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        context = this;
        Button login = (Button) this.findViewById(R.id.loginButton);
        Button register = (Button) this.findViewById(R.id.registerButton);
        username = (EditText) this.findViewById(R.id.username);

        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(context, MainActivity.class);
                myIntent.putExtra("username", username.getText());
                startActivity(myIntent);
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               openCamera();
            }
        });

        this.getApplicationContext()
                .getContentResolver()
                .registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false,
                        photoObserver);
        Log.d("INSTANT", "registered content observer");
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
                    int i = 0;
                    for (File f : takenPhotos){
                       photosPath[i] = takenPhotos.get(i).getAbsolutePath();
                       i++;
                    }
                    b.putStringArray("images_path", photosPath);
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
            takenPhotos.add(media.getFile());
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


}
