package edu.thesis.fct.client;


import android.content.Context;
import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.getRotationMatrix2D;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;
import static org.bytedeco.javacpp.opencv_objdetect.CASCADE_FIND_BIGGEST_OBJECT;
import static org.bytedeco.javacpp.opencv_objdetect.CASCADE_SCALE_IMAGE;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import static org.bytedeco.javacpp.opencv_core.RectVector;
import static org.bytedeco.javacpp.opencv_core.Size;


public class FaceProcessing {

    public static Size trainSize;
    public static CascadeClassifier cascadeClassifier;
    public static CascadeClassifier eyeClassifier;

    public static FaceRecognizer trainAndSave(String train, String saveDir, Context context){
        String trainingDir = train;
        File root = new File(trainingDir);

        List<File> tmp = new ArrayList<>();

        for (File outer : root.listFiles()){
            File faces = new File(outer, "faces");
            if (faces.exists()){
                tmp.addAll(Arrays.asList(faces.listFiles()));
            }
            else {
                faces.mkdirs();
                for (File inner : outer.listFiles()){
                    if (inner.getName().equals("faces")) continue;
                    File f = detectFacesOpenCV(inner,true,context);
                    if(f != null) tmp.add(f);
                }
            }
        }

        File [] imageFiles = Arrays.copyOf(tmp.toArray(), tmp.size(), File[].class);

        MatVector images = new MatVector(imageFiles.length);

        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        String [] names = new String[imageFiles.length];

        int counter = 0;
        trainSize = imread(imageFiles[0].getAbsolutePath()).size();


        for (File image : imageFiles) {
            Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);

            if (img.size() != trainSize) {
                img = resizeImage(img,trainSize);
            }

            String name = image.getName().split("_")[0] + "_" + image.getName().split("_")[1] + "_" + image.getName().split("_")[2] ;

            images.put(counter, img);

            names[counter] = name;

            labelsBuf.put(counter, counter);

            counter++;
        }

        FaceRecognizer faceRecognizer = createFisherFaceRecognizer();
        // FaceRecognizer faceRecognizer = createEigenFaceRecognizer();
         //FaceRecognizer faceRecognizer = createLBPHFaceRecognizer();

        faceRecognizer.train(images, labels);
        for (int i = 0; i < names.length; i++){
            faceRecognizer.setLabelInfo(i,names[i]);
        }

        File saveDirFile = new File(saveDir);
        saveDirFile.getParentFile().mkdirs();
        faceRecognizer.save(saveDir);
        gzipIt(saveDir,FaceRecognitionAsync.SAVE_PATH);
        saveDirFile.delete();


        return faceRecognizer;
    }

    public static double[] recognize(String recogImage, FaceRecognizer faceRecognizer, Context context){

        File faces = detectFacesOpenCV(new File(recogImage),false,context);
        double [] result = new double[faces.listFiles().length];
        int count = 0;
        for (File f : faces.listFiles()){
            Mat image = imread(f.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
            image = resizeImage(image,trainSize);
            double[] prediction = new double[1];
            int[] predictionImageLabel = new int[1];
            /*try {
                Mat tmp = AlignEyes(image,context);
                if (tmp != null){
                    image = tmp;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }*/
            faceRecognizer.predict(image, predictionImageLabel, prediction);
            System.out.println("Predicted label: " + faceRecognizer.getLabelInfo(predictionImageLabel[0]).getString() + " Confidence:" + prediction[0]);
            result[count] = prediction[0];
            count++;

        }
        try {
            FileUtils.deleteDirectory(faces);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static File detectFacesOpenCV(File f, boolean train, Context context) {
        try {
            Mat image = imread(f.getAbsolutePath());
            if (cascadeClassifier == null){
                InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
                File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt");
                FileOutputStream os = new FileOutputStream(mCascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();

                cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            }

            RectVector faceDetections = new RectVector();
            cascadeClassifier.detectMultiScale(image, faceDetections);

            if (train && faceDetections.size() > 1) return null;

            System.out.println(String.format("Detected %s faces", faceDetections.size()));
            File faces;
            if (train) faces = new File(f.getParentFile().getAbsolutePath(), "faces");
            else{
                faces = new File(Environment.getExternalStorageDirectory(),"hyrax_tmp");
                faces.mkdirs();
            }
            if (faceDetections.size() > 0) {
                faces.mkdir();

                for (int i = 0; i < faceDetections.size(); i++) {
                    opencv_core.Rect aux = faceDetections.get(i);
                    Mat image_roi = new Mat(image, aux);
                   /* Mat tmp = AlignEyes(image_roi,context);
                    if (tmp != null){
                        image_roi = tmp;
                    }*/
                    String path = faces.getAbsolutePath() + File.separator + i + f.getName();
                    imwrite(path, image_roi);
                    if (train) return new File(path);
                }
                return faces;
            }
            return null;


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<ImageModel> recognizeInPath(List<ImageModel> images, FaceRecognizer recognizer, Context context){
        List<ImageModel> result = new ArrayList<>();
        for (ImageModel outter : images){
            File image = new File(GalleryActivity.HYRAX_PATH, outter.getPhotoName() + File.separator + outter.getPhotoName() + ".jpg");
            double [] euclidean = recognize(image.getAbsolutePath(),recognizer,context);
            for (double d : euclidean){
                if (d < 3000){
                    result.add(outter);
                    break;
                }
            }

        }
        return result;
    }

    public static FaceRecognizer loadEngineFromFile(String path){
        FaceRecognizer faceRecognizer = createFisherFaceRecognizer();
        String storePath;
        if (path.contains("my_template")){
            storePath = FaceRecognitionAsync.SAVE_PATH_TMP;
        } else {
            storePath = FaceRecognitionAsync.RECOG_PATH + new File(path).getName() + ".yml";
        }
        gunzipIt(path, storePath);
        faceRecognizer.load(storePath);
        RecognitionEngineHolder.getInstance().setEngine(faceRecognizer);
        new File(storePath).delete();
        return faceRecognizer;
    }

    private static Mat resizeImage(Mat img, Size trainSize){
        Mat resizedImage = new Mat();
        org.bytedeco.javacpp.opencv_imgproc.resize(img, resizedImage, trainSize);
        return resizedImage;
    }

    public static void gzipIt(String input, String output){

        byte[] buffer = new byte[1024];

        try{

            GZIPOutputStream gzos =
                    new GZIPOutputStream(new FileOutputStream(output));

            FileInputStream in =
                    new FileInputStream(input);

            int len;
            while ((len = in.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }

            in.close();

            gzos.finish();
            gzos.close();

            System.out.println("Done");

        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public static void gunzipIt(String input, String output){

        byte[] buffer = new byte[1024];

        try{

            GZIPInputStream gzis =
                    new GZIPInputStream(new FileInputStream(input));

            FileOutputStream out =
                    new FileOutputStream(output);

            int len;
            while ((len = gzis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gzis.close();
            out.close();

            System.out.println("Done");

        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public static Mat alignEyes(Mat image, Context context) throws IOException {
        if (eyeClassifier == null){
            InputStream is = context.getResources().openRawResource(R.raw.lefteye);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lefteye");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            eyeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        }

        RectVector eyes = new RectVector();
        eyeClassifier.detectMultiScale(image, eyes, 1.15, 2,
                CASCADE_FIND_BIGGEST_OBJECT
                        | CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        System.out.println(String.format("Detected %s eyes", eyes.size()));

        if (eyes.size() != 2) return null;
        eyes = sortEyesVector(eyes);
        Mat result = image;
        int deltaY = (eyes.get(1).y() + eyes.get(1).height()/2) - (eyes.get(0).y() + eyes.get(0).height()/2);
        int  deltaX = (eyes.get(1).x() + eyes.get(1).width()/2) - (eyes.get(0).x() + eyes.get(0).width()/2);
        double degrees = Math.atan2(deltaY, deltaX)*180/Math.PI;
        if (Math.abs(degrees) < 35)
        {
            result = rotate(image,-degrees);
        }
        return result;
    }


    private static RectVector sortEyesVector(RectVector r){
        RectVector tmp = new RectVector();
        if (r.get(0).x() > r.get(1).x()){
            tmp.put(0,r.get(1));
            tmp.put(1,r.get(0));
        } else tmp = r;
        return tmp;

    }

    private static Mat rotate(Mat src, double angle) {
        opencv_core.Point2f src_center = new opencv_core.Point2f(src.cols()/2.0F, src.rows()/2.0F);
        Mat rot_mat = getRotationMatrix2D(src_center, angle, 1.0);
        Mat dst = new Mat();
        warpAffine(src, dst, rot_mat, src.size());
        return dst;
    }



}

