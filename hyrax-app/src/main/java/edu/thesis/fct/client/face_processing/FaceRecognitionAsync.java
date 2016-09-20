package edu.thesis.fct.client.face_processing;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;

import org.bytedeco.javacpp.opencv_face;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.thesis.fct.client.RecognitionEngineHolder;

public class FaceRecognitionAsync extends AsyncTask<Object, Void, Void> {

    File mFile;
    Activity activity;
    final static String SAVE_PATH_TMP = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxRecog" + File.separator + "my_template.yml";
    public final static String RECOG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxRecog" + File.separator;
    public final static String SAVE_PATH =  RECOG_PATH + "my_template.gz";
    public final static String TRAIN_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxTrain";

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

        //FaceProcessing.recognize(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Download" + File.separator + "dicap.jpg",RecognitionEngineHolder.getInstance().getEngine(),activity );
        //FaceProcessing.recognize(new File(TRAIN_PATH + File.separator + "Adrien_Brody").listFiles()[0].getAbsolutePath(),RecognitionEngineHolder.getInstance().getEngine(),activity );
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

