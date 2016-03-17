package edu.thesis.fct.client;


import android.content.Context;
import android.os.Build;
import android.os.Environment;

import static org.bytedeco.javacpp.opencv_face.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;


public class FaceDetection {

    private Context context;
    static final int CV_LOAD_IMAGE_GRAYSCALE = 0;
    File mCascadeFile;
    CascadeClassifier cascadeClassifier;

    public FaceDetection(Context context){
        this.context = context;
    }

    public void detectFaces( File f) {
        try {
            Mat image = imread(f.getAbsolutePath());
            //SUPER HACK
            if (Build.MANUFACTURER.contains("samsung")){
                image = image.t().asMat();
            }
            imwrite(Environment.getExternalStorageDirectory() + "/potasçdasd.jpg", image);
            System.out.println(Environment.getExternalStorageDirectory() + "/potasçdasd.jpg");
            System.out.println("CARALHO");

            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt_tree);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt_tree.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());

            RectVector faceDetections = new RectVector();
            cascadeClassifier.detectMultiScale(image, faceDetections);

            System.out.println(String.format("Detected %s faces", faceDetections.size()));

            if (faceDetections.size() > 0){
                File faces = new File(f.getParentFile().getAbsolutePath() + "/faces");
                faces.mkdir();

                for (int i = 0; i < faceDetections.size(); i++){
                    Rect aux = faceDetections.get(i);
                    Mat image_roi = new Mat(image, aux);
                    imwrite(faces.getAbsolutePath() + "/face_" + i + ".jpg", image_roi);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void faceRecognition(String trainingDir, String facePath ){

        Mat image = imread(facePath, CV_LOAD_IMAGE_GRAYSCALE);

        File root = new File(trainingDir);

        FilenameFilter imgFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };

        File[] imageFiles = root.listFiles(imgFilter);

        MatVector images = new MatVector(imageFiles.length);

        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.getIntBuffer();

        int counter = 0;

        Size trainSize = null;
        boolean first = true;

        for (File img : imageFiles) {
            Mat imgAux = imread(img.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);

            if (first){
                trainSize = imgAux.size();
                first = false;
            }

            if (imgAux.size() != trainSize){
                Mat resizedImage = new Mat();
                org.bytedeco.javacpp.opencv_imgproc.resize(imgAux, resizedImage, trainSize);
                imgAux = resizedImage;
            }

            int label = Integer.parseInt(img.getName().split("\\.")[1]);

            images.put(counter, imgAux);

            labelsBuf.put(counter, label);

            counter++;
        }

        FaceRecognizer faceRecognizer = createEigenFaceRecognizer();

        Mat resizedImage = new Mat();
        org.bytedeco.javacpp.opencv_imgproc.resize(image, resizedImage, trainSize);
        image = resizedImage;

        faceRecognizer.train(images, labels);

        double[] prediction = new double[1];
        int[] predictionImageLabel = new int[1];
        faceRecognizer.predict(image, predictionImageLabel, prediction);

        System.out.println("Predicted label: " + predictionImageLabel[0] + " Confidence:" + prediction[0]);

    }



}

