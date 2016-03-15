package edu.thesis.fct.client;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by abs on 15-03-2016.
 */
public class FaceRecognitionAsync extends AsyncTask<Object, Void, Void> {

    Context context;

    @Override
    protected Void doInBackground(Object... params) {
        context = (Context) params[0];
        FaceDetection f = new FaceDetection(context);
        f.faceRecognition((String)params[1], (String)params[2]);
        return null;
    }
}
