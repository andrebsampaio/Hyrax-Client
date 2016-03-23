package edu.thesis.fct.client;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Environment;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

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


public class FaceProcessing {

    private Context context;
    static final int CV_LOAD_IMAGE_GRAYSCALE = 0;
    File mCascadeFile;
    CascadeClassifier cascadeClassifier;

    public FaceProcessing(Context context) {
        this.context = context;
    }

    public File [] detectFaces(File f, int cameraLens) {
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();

        Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());

        //SUPER HACK
        if (Build.MANUFACTURER.contains("samsung")) {
            if (CameraCharacteristics.LENS_FACING_BACK == cameraLens) {
                bitmap = rotate(bitmap, 90);
            } else {
                bitmap = rotate(bitmap, -90);
            }

        }

        saveFace(bitmap, Environment.getExternalStorageDirectory() + "/potasçdasd.jpg");
        SparseArray<Face> mFaces = new SparseArray<Face>();
        if (!detector.isOperational()) {
            System.out.println("RECOG IS NOT OPERATIONAL");
        } else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            mFaces = detector.detect(frame);
            detector.release();
        }

        System.out.println(mFaces.size());

        File [] facesFiles = new File [mFaces.size()];

        if (mFaces.size() > 0) {
            File faces = new File(f.getParentFile().getAbsolutePath() + "/faces");
            faces.mkdir();


            for (int i = 0; i < mFaces.size(); i++) {
                Face face = mFaces.valueAt(i);
                String faceLocation = faces.getAbsolutePath() + "/" + f.getName()  + "_face" + i + ".jpg";
                saveFace(cropFace(bitmap, face), faceLocation);
                facesFiles[i] = new File (faceLocation);
            }
        }

        return facesFiles;
    }


    private Bitmap cropFace(Bitmap b, Face face) {
        System.out.println("X: " + (int) face.getPosition().x + " Y: " + (int) face.getPosition().y );
        Bitmap cutBitmap = Bitmap.createBitmap(b, (int) face.getPosition().x, (int) face.getPosition().y, (int) face.getWidth(), (int) face.getHeight());
        return cutBitmap;

    }

    private void saveFace(Bitmap bitmap, String name) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(name);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void detectFacesOpenCV(File f, int cameraLens) {
        try {
            Mat image = imread(f.getAbsolutePath());
            //SUPER HACK
            if (Build.MANUFACTURER.contains("samsung")) {
                if (CameraCharacteristics.LENS_FACING_BACK == cameraLens) {
                    rotate90CV(image, 1);
                } else {
                    rotate90CV(image, 2);
                }

            }

            imwrite(Environment.getExternalStorageDirectory() + "/potasçdasd.jpg", image);
            System.out.println(Environment.getExternalStorageDirectory() + "/potasçdasd.jpg");

            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
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

            if (faceDetections.size() > 0) {
                File faces = new File(f.getParentFile().getAbsolutePath() + "/faces");
                faces.mkdir();

                for (int i = 0; i < faceDetections.size(); i++) {
                    Rect aux = faceDetections.get(i);
                    Mat image_roi = new Mat(image, aux);
                    imwrite(faces.getAbsolutePath() + "/face_" + i + ".jpg", image_roi);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void faceRecognition(String trainingDir, String facePath) {

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

            if (first) {
                trainSize = imgAux.size();
                first = false;
            }

            if (imgAux.size() != trainSize) {
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
        faceRecognizer.save(Environment.getExternalStorageDirectory() + "/andre_eigenface.xml");

        double[] prediction = new double[1];
        int[] predictionImageLabel = new int[1];
        faceRecognizer.predict(image, predictionImageLabel, prediction);

        System.out.println("Predicted label: " + predictionImageLabel[0] + " Confidence:" + prediction[0]);

    }

    private void rotate90CV(Mat image, int rotFlag) {
        if (rotFlag == 1) {
            transpose(image, image);
            flip(image, image, 1); //transpose+flip(1)=CW
        } else if (rotFlag == 2) {
            transpose(image, image);
            flip(image, image, 0); //transpose+flip(0)=CCW
        } else if (rotFlag == 3) {
            flip(image, image, -1);    //flip(-1)=180
        } else
            System.out.println("UNKNOWN FLAG");
    }

    private Bitmap rotate(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();

        matrix.postRotate(angle);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        return rotatedBitmap;

    }
}

