package edu.thesis.fct.client;


import android.content.Context;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;


public class FaceProcessing {

    private Context context;

    public FaceProcessing(Context context) {

        this.context = context;
    }

    public static FaceRecognizer train(String train){
        String trainingDir = train;
        File root = new File(trainingDir);

        List<File> tmp = new ArrayList<>();

        for (File outer : root.listFiles()){
            for (File inner : outer.listFiles()){
                tmp.add(inner);
            }
        }

        File [] imageFiles = Arrays.copyOf(tmp.toArray(), tmp.size(), File[].class);

        MatVector images = new MatVector(imageFiles.length);

        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        String [] names = new String[imageFiles.length];

        int counter = 0;

        for (File image : imageFiles) {
            Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);

            String name = image.getName().split("_")[0] + "_" + image.getName().split("_")[1] ;

            images.put(counter, img);

            names[counter] = name;

            labelsBuf.put(counter, counter);

            counter++;
        }

        //FaceRecognizer faceRecognizer = createFisherFaceRecognizer();
        // FaceRecognizer faceRecognizer = createEigenFaceRecognizer();
         FaceRecognizer faceRecognizer = createLBPHFaceRecognizer();

        faceRecognizer.train(images, labels);
        for (int i = 0; i < names.length; i++){
            faceRecognizer.setLabelInfo(i,names[i]);
        }

        return faceRecognizer;
    }

    public static void recognize(String recogImage, FaceRecognizer faceRecognizer){

        Mat testImage = imread(recogImage, CV_LOAD_IMAGE_GRAYSCALE);

        int predictedLabel = faceRecognizer.predict(testImage);

        System.out.println("Predicted label: " + faceRecognizer.getLabelInfo(predictedLabel));
    }
}

