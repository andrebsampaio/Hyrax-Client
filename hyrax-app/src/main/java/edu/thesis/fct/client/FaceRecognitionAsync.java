package edu.thesis.fct.client;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.camera2.params.Face;
import android.os.AsyncTask;

import java.io.File;

public class FaceRecognitionAsync extends AsyncTask<Object, Void, Void> {

    File mFile;
    Activity activity;

    public FaceRecognitionAsync(File file, Activity activity){
        mFile = file;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Object... params) {
        if (RecognitionEngineHolder.getInstance().getEngine() == null){
            RecognitionEngineHolder.getInstance().setEngine(FaceProcessing.train(mFile.getAbsolutePath()));
        } else {
            FaceProcessing.recognize(mFile.getAbsolutePath(),RecognitionEngineHolder.getInstance().getEngine());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
