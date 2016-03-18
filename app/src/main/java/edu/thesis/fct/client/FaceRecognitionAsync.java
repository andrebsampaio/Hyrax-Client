package edu.thesis.fct.client;

import android.content.Context;
import android.os.AsyncTask;

public class FaceRecognitionAsync extends AsyncTask<Object, Void, Void> {

    Context context;

    @Override
    protected Void doInBackground(Object... params) {
        context = (Context) params[0];
        FaceProcessing f = new FaceProcessing(context);
        f.faceRecognition((String)params[1], (String)params[2]);
        return null;
    }
}
