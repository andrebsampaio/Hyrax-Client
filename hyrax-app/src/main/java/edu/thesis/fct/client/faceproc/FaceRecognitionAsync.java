package edu.thesis.fct.client.faceproc;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;

import org.bytedeco.javacpp.opencv_face;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.thesis.fct.client.config.Configurations;
import edu.thesis.fct.client.RecognitionEngineHolder;

public class FaceRecognitionAsync extends AsyncTask<Object, Void, Void> {

    File mFile;
    Activity activity;

    public FaceRecognitionAsync(File file, Activity activity){
        mFile = file;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Object... params) {
        if (!new File(Configurations.TRAIN_PATH).exists()) return null;
        if ((boolean)params[0]){
            opencv_face.FaceRecognizer faceRecognizer;
            if (new File(Configurations.SAVE_PATH).exists()){
               faceRecognizer = FaceProcessing.loadEngineFromFile(Configurations.SAVE_PATH);
            } else {
                faceRecognizer = FaceProcessing.trainAndSave(Configurations.TRAIN_PATH, Configurations.SAVE_PATH_TMP,activity);
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

    private static void testRecog(Activity activity){
        int threshold = 2600;
        for (int i = 0; i < 3; i++){
            int positive = 0; int negative = 0; int misId = 0; int error = 0;
            for (File f : new File(Environment.getExternalStorageDirectory(),"testImages").listFiles()){
                double [] d = FaceProcessing.recognize(f.getAbsolutePath(),RecognitionEngineHolder.getInstance().getEngine(),activity);
                if (d == null){
                    error++;
                    f.delete();
                    continue;
                }
                if (d[0] == 0) continue;
                if (d[0] < threshold){
                    if(f.getName().contains("George_")){
                        positive++;
                    } else {
                        misId++;
                    }
                } else {
                    if(f.getName().contains("George_")){
                        misId++;
                    } else {
                        negative++;
                    }
                }
            }
            try
            {
                String filename= Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "rates.txt";
                FileWriter fw = new FileWriter(filename,true); //the true will append the new data
                fw.write("Threshold: " + threshold + " -" + "Positive: " + positive + "," + " Negative: " + negative + "," + "MisId: " + misId + "," + "Error: " + error + "\n");//appends the string to the file
                fw.close();
            }
            catch(IOException ioe)
            {
                System.err.println("IOException: " + ioe.getMessage());
            }
            System.out.println("Finished " + i);
            threshold += 100;
        }
    }
}

