package edu.thesis.fct.client;


import android.content.Context;

/*import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.alignment.RotateScaleAligner;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.face.recognition.FaceRecognitionEngine;
import org.openimaj.image.processing.face.recognition.FisherFaceRecogniser;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.util.pair.IndependentPair;*/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class FaceProcessing {

    private Context context;

    public FaceProcessing(Context context) {

        this.context = context;
    }

    /*public static List<ScoredAnnotation<String>> recognizeFacesRevised(File toInspect, FaceRecognitionEngine<KEDetectedFace, String> engine){

        List<ScoredAnnotation<String>> detectedPeople = new ArrayList<ScoredAnnotation<String>>();

        try{
            List<IndependentPair<KEDetectedFace, ScoredAnnotation<String>>> results = engine.recogniseBest(ImageUtilities.readF(toInspect));
            System.out.println("Number of people " + results.size());
            int fCount = 0;
            for (IndependentPair<KEDetectedFace, ScoredAnnotation<String>> pair: results) {
                KEDetectedFace face = pair.firstObject();
                ScoredAnnotation<String> annotation = pair.secondObject();
                saveFace((DetectedFace)face, toInspect.getParentFile(), String.valueOf(fCount++));
                detectedPeople.add(annotation);
            }
            writeNames(detectedPeople, toInspect.getParentFile());


        } catch (IOException e){
            e.printStackTrace();
        }

        return detectedPeople;

    }

    private static void writeNames(List<ScoredAnnotation<String>> people, File file){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file.getAbsolutePath() + File.separator + "people.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < people.size(); i++){
            writer.println(people.get(i));
        }
        writer.close();
    }

    public FaceRecognitionEngine createAndTrainRecognitionEngine(GroupedDataset dataset) {
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
        FaceDetector<DetectedFace, FImage> haar = new HaarCascadeDetector(mCascadeFile.getAbsolutePath());
        FKEFaceDetector faceDetector = new FKEFaceDetector(haar);
        FisherFaceRecogniser<KEDetectedFace, String> recognizer = FisherFaceRecogniser.create(18, new RotateScaleAligner(), 1, DoubleFVComparison.CORRELATION, 0.7f);
        FaceRecognitionEngine engine = FaceRecognitionEngine.create(faceDetector,recognizer);
        engine.train(dataset);
        return engine;
    }

    private Bitmap superHack(Bitmap bitmap, int cameraLens){
        //SUPER HACK
        if (Build.MANUFACTURER.contains("samsung")) {
            if (CameraCharacteristics.LENS_FACING_BACK == cameraLens) {
                bitmap = rotate(bitmap, 90);
            } else {
                bitmap = rotate(bitmap, -90);
            }

        }

        return bitmap;
    }

    private static void saveFace(DetectedFace face, File folder, String name) throws FileNotFoundException {
        File f = new File(folder.getAbsolutePath() + "/faces");
        if (!f.exists()){
            f.mkdirs();
        }
        try {
            File faceFile = new File(f.getAbsolutePath() + File.separator + name + ".jpg");
            ImageUtilities.write(face.getFacePatch(), faceFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static GroupedDataset getGroupedDataset(File folder){
        try {

            VFSGroupDataset<FImage> groupedFaces =
                    new VFSGroupDataset<FImage>(folder.getAbsolutePath(), ImageUtilities.FIMAGE_READER);
            System.out.println("train images size: " + groupedFaces.size());
            return groupedFaces;
        } catch (FileSystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }*/
}

