package edu.thesis.fct.client;

import android.app.Activity;
import android.hardware.camera2.params.Face;
import android.os.AsyncTask;
import android.os.Environment;

import org.bytedeco.javacpp.opencv_face;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.io.File;
import java.io.IOException;

public class FaceRecognitionAsync extends AsyncTask<Object, Void, Void> {

    File mFile;
    Activity activity;
    final static String SAVE_PATH_TMP = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxRecog" + File.separator + "my_template.yml";
    public final static String RECOG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxRecog" + File.separator;
    final static String SAVE_PATH =  RECOG_PATH + "my_template.gz";
    final static String TRAIN_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxTrain";

    public FaceRecognitionAsync(File file, Activity activity){
        mFile = file;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Object... params) {
        if (!new File(TRAIN_PATH).exists()) return null;
        if ((boolean)params[0]){
            opencv_face.FaceRecognizer faceRecognizer;
            if (new File(SAVE_PATH).exists()){
               faceRecognizer = FaceProcessing.loadEngineFromFile(SAVE_PATH);
            } else {
                faceRecognizer = FaceProcessing.trainAndSave(TRAIN_PATH, SAVE_PATH_TMP,activity);
            }
            RecognitionEngineHolder.getInstance().setEngine(faceRecognizer);
        } else {
            FaceProcessing.recognize(mFile.getAbsolutePath(),RecognitionEngineHolder.getInstance().getEngine(),activity);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
