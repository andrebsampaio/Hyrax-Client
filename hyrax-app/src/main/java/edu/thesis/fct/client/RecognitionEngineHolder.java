package edu.thesis.fct.client;

//import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
//import org.openimaj.image.processing.face.recognition.FaceRecognitionEngine;

import org.bytedeco.javacpp.opencv_face;

/**
 * Created by abs on 10-07-2016.
 */
public class RecognitionEngineHolder {

    private opencv_face.FaceRecognizer engine;

    public opencv_face.FaceRecognizer getEngine(){return engine;}

    public void setEngine(opencv_face.FaceRecognizer e){engine = e;}

    private static final RecognitionEngineHolder holder = new RecognitionEngineHolder();
    public static RecognitionEngineHolder getInstance() {return holder;}


}
