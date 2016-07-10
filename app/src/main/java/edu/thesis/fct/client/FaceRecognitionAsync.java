package edu.thesis.fct.client;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.camera2.params.Face;
import android.os.AsyncTask;

import java.io.File;

public class FaceRecognitionAsync extends AsyncTask<Object, Void, Void> {

    File mFile;

    public FaceRecognitionAsync(File file){
        mFile = file;
    }

    @Override
    protected Void doInBackground(Object... params) {
        FaceProcessing.recognizeFacesRevised(mFile, RecognitionEngineHolder.getInstance().getEngine());
        return null;
    }
}
