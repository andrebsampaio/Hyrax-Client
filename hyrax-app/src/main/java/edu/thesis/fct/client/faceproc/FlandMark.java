package edu.thesis.fct.client.faceproc;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;

import edu.thesis.fct.client.config.Configurations;

import static org.bytedeco.javacpp.flandmark.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;


public class FlandMark {

	enum landmark_pos {
		FACE_CENTER(0),
		LEFT_EYE_INNER(1) ,
		RIGHT_EYE_INNER(2),
		MOUTH_LEFT(3),
		MOUTH_RIGHT(4),
		LEFT_EYE_OUTER(5),
		RIGHT_EYE_OUTER(6),
		NOSE_CENTER(7),
		LEFT_EYE_ALIGN(8),
		RIGHT_EYE_ALIGN(9);

		int value;

		private landmark_pos(int value) {
			this.value = value;
		}



	};

	/** detect biggest face */
	private static RectVector detect_faces(Mat image, CascadeClassifier cascade){
		final double scale_factor = 1.1;
		final int min_neighbours = 2;
		final int flags = 2;
		final Size min_size = new Size(48, 48);
		final Size max_size = new Size();

		if(cascade.empty() || image.empty()){
			return new RectVector();
		}

		RectVector faces = new RectVector();
		cascade.detectMultiScale(image, faces);

		if (faces.size() == 0)
			return new RectVector();

		return faces;
	}

	/** rotate points based on rot_mat */
	private static void get_rotated_points(ArrayList<Point> points, ArrayList<Point>dst_points, Mat rot_mat){

		for(int i=0; i < points.size(); i++)
		{
			Mat point_original = new Mat(3, 1, opencv_core.CV_64FC1);
			DoubleIndexer indexer = point_original.createIndexer();
			indexer.put(0,0,points.get(i).x());
			indexer.put(1,0,points.get(i).y());
			indexer.put(2, 0, 1);

			Mat result = new Mat(2, 1, opencv_core.CV_64FC1);
			opencv_core.gemm(rot_mat,point_original, 1.0, new Mat(), 0.0, result);
			DoubleIndexer indexer1 = result.createIndexer();
			Point point_result = new Point((int)Math.round(indexer1.get(0,0)),(int)Math.round(indexer1.get(0,0)));

			// Point point_result(cvRound(result.at<double>(0,0)),cvRound(result.at<double>(1,0)));

			dst_points.add(point_result);
		}
	}

	/** show landmarks in an image */
	private static File show_landmarks(final ArrayList<Point> landmarks, final Mat image, final String filename,
									   CascadeClassifier eyecascade, boolean isTraining)throws  Exception{

		Mat result  = new Mat();
		opencv_imgproc.cvtColor( image, result, opencv_imgproc.COLOR_GRAY2BGR  );

	 
	      /*  for(int i=0; i<landmarks.size()-2; i++){
	                Core.circle(result,landmarks.get(i), 1, new Scalar(255,0,0),1);
	        }
	 
	        Core.circle(result, landmarks.get(landmark_pos.LEFT_EYE_ALIGN.value), 1, new Scalar(255, 255, 0), 1);
	        Core.circle(result, landmarks.get(landmark_pos.RIGHT_EYE_ALIGN.value), 1, new Scalar(255, 255, 0), 1);*/

		//Highgui.(named_window,result);
		if (is_face_present(result,eyecascade)){
			File tmp;
			if (isTraining){
				File f = new File(new File(Configurations.TRAIN_PATH).listFiles()[0],"faces");
				f.mkdirs();
				tmp = new File(f.getAbsolutePath() + File.separator + new File(filename).getName()+ "aligned" + Configurations.JPG);
			} else {
				tmp = new File(filename + "aligned.jpg");
			}
			imwrite(tmp.getAbsolutePath(),result);
			return tmp;
		}
		else
			throw new Exception("Cannot save this image");
	}


	/** aligns the face based on the recalculated positions of the eyes and aligns also the landmarks*/
	private static File align(final Mat image, Mat dst_image, ArrayList<Point>landmarks, ArrayList<Point>dst_landmarks,
								String mPath,String filename,CascadeClassifier eyecascade, boolean isTraining)throws Exception{
		final double DESIRED_LEFT_EYE_X = 0.16;     // Controls how much of the face is visible after preprocessing.
		final double DESIRED_LEFT_EYE_Y = 0.4;

		// Use square faces.
		int desiredFaceWidth = 100;
		int desiredFaceHeight = desiredFaceWidth;

		Point leftEye = landmarks.get(landmark_pos.LEFT_EYE_ALIGN.value);
		Point rightEye = landmarks.get(landmark_pos.RIGHT_EYE_ALIGN.value);

		// Get the center between the 2 eyes.
		Point2f eyesCenter =new Point2f(((leftEye.x() + rightEye.x()) * 0.5f), ((leftEye.y() + rightEye.y()) * 0.5f) );
		// Get the angle between the 2 eyes.
		double dy = (rightEye.y() - leftEye.y());
		double dx = (rightEye.x() - leftEye.x());
		double len = Math.sqrt(dx*dx + dy*dy);
		double angle = Math.atan2(dy, dx) * 180.0/Math.PI; // Convert from radians to degrees.

		// Hand measurements shown that the left eye center should ideally be at roughly (0.19, 0.14) of a scaled face image.
		final double DESIRED_RIGHT_EYE_X = (1.0f - DESIRED_LEFT_EYE_X);
		// Get the amount we need to scale the image to be the desired fixed size we want.
		double desiredLen = (DESIRED_RIGHT_EYE_X - DESIRED_LEFT_EYE_X) * desiredFaceWidth;
		double scale = desiredLen / len;
		// Get the transformation matrix for rotating and scaling the face to the desired angle & size.
		Mat rot_mat = opencv_imgproc.getRotationMatrix2D(eyesCenter, angle, scale);
		DoubleIndexer indexer = rot_mat.createIndexer();
		// Shift the center of the eyes to be the desired center between the eyes.
		double x_data = indexer.get(0, 2) + desiredFaceWidth * 0.5f - eyesCenter.x();
		double y_data =  indexer.get(1, 2)  + desiredFaceHeight * DESIRED_LEFT_EYE_Y - eyesCenter.y();

		indexer.put(0, 2, x_data);
		indexer.put(1, 2, y_data);


		// Rotate and scale and translate the image to the desired angle & size & position!
		// Note that we use 'w' for the height instead of 'h', because the input face has 1:1 aspect ratio.
		dst_image = new Mat(desiredFaceHeight, desiredFaceWidth, opencv_core.CV_8U, new Scalar(128)); // Clear the output image to a default grey.
		opencv_imgproc.warpAffine(image, dst_image, rot_mat, dst_image.size());

		//rotate landmarks
		get_rotated_points(landmarks,dst_landmarks, rot_mat);

		if(!dst_image.empty()){
			return show_landmarks(dst_landmarks,dst_image,filename,eyecascade,isTraining);
		} else return null;

	}

	/** detects landmarks using flandmakrs and add two more landmakrs to be used to alignt the face*/
	private static ArrayList<Point> detectLandmarks(FLANDMARK_Model model, final Mat  image,final opencv_core.IplImage gray_image_iipl, final Rect  face){

		ArrayList<Point> landmarks = new ArrayList<Point>();

		// 1. get landmarks (using flandmarks)
		int bbox [] = { face.x(), face.y(), face.x() + face.width(), face.y() + face.height() };
		double [] points = new double [2 * model.data().options().M()];

		if( flandmark_detect(gray_image_iipl, bbox, model,points) > 0){
			return landmarks;
		}

		for (int i = 0; i < model.data().options().M(); i++) {
			landmarks.add(new Point((int)points[2 * i], (int)points[2 * i + 1]));
		}

		// 2. get a linar regresion using the four points of the eyes
		LinearRegression lr = new LinearRegression();
		lr.addPoint(landmarks.get(landmark_pos.LEFT_EYE_OUTER.value).x(),landmarks.get(landmark_pos.LEFT_EYE_OUTER.value).y());
		lr.addPoint(landmarks.get(landmark_pos.LEFT_EYE_INNER.value).x(),landmarks.get(landmark_pos.LEFT_EYE_INNER.value).y());
		lr.addPoint(landmarks.get(landmark_pos.RIGHT_EYE_INNER.value).x(),landmarks.get(landmark_pos.RIGHT_EYE_INNER.value).y());
		lr.addPoint(landmarks.get(landmark_pos.RIGHT_EYE_OUTER.value).x(),landmarks.get(landmark_pos.RIGHT_EYE_OUTER.value).y());

		double coef_determination = lr.getCoefDeterm();
		double coef_correlation = lr.getCoefCorrel();
		double standar_error_estimate = lr.getStdErrorEst();

		double a = lr.getA();
		double b = lr.getB();

		// 3. get two more landmarks based on this linear regresion to be used to align the face
		Point pp1 = new Point(landmarks.get(landmark_pos.LEFT_EYE_OUTER.value).x(),
				(int)(landmarks.get(landmark_pos.LEFT_EYE_OUTER.value).x()*b+a));
		Point pp2 = new Point(landmarks.get(landmark_pos.RIGHT_EYE_OUTER.value).x(),
				(int)(landmarks.get(landmark_pos.RIGHT_EYE_OUTER.value).x()*b+a));

		landmarks.add(pp1);
		landmarks.add(pp2);
	 
	       /* delete[] points;
	        points = 0;*/
		return landmarks;
	}


	public static List<File> alignImage(String mPath , Mat gray_image , opencv_core.IplImage gray_image_iipl,
								  CascadeClassifier cascade ,
								  FLANDMARK_Model model,String filetosave,CascadeClassifier eyecascade, boolean isTraining )throws Exception
	{
		// final FLANDMARK_Model model = loadFLandmarkModel(flandmarkModelFile);
		RectVector vect = detect_faces(gray_image,cascade);
		List<File> res = new ArrayList<>();
		for (int i = 0; i < vect.size(); i++){
			Rect r = vect.get(i);
			Mat aligned_image = new Mat();
			ArrayList<Point> aligned_landmarks = new ArrayList<Point>();

			// Detect landmarks
			ArrayList<Point> landmarks = detectLandmarks(model, gray_image,gray_image_iipl,new Rect(r.x(),r.y(),r.width(),r.height()));

			if(landmarks.size() == 0){
				throw new Exception("No Face detetced");

			}

			//align the face
			File aligned = align(gray_image,aligned_image,landmarks,aligned_landmarks,mPath,filetosave,eyecascade, isTraining);
			if (aligned != null) res.add(aligned);
		}
		return res;

	}



	private static boolean is_face_present(Mat image , CascadeClassifier cascade)
	{

		int mAbsoluteFaceSize =0;


		int height = image.rows();
		if (Math.round(height * 0.9f) > 0) {
			mAbsoluteFaceSize = Math.round(height * 0.9f);
		}
		//mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);


		//Start

		final double scale_factor = 1.05;
		final int min_neighbours = 2;
		final int flags = 2;
		final Size min_size = new Size(10, 10);
		final Size max_size = new Size();



		RectVector faces = new RectVector();
		cascade.detectMultiScale(image, faces, scale_factor, min_neighbours, flags, min_size, max_size);

		if (faces.size() == 0)
			return false;
		else
			return true;


	}





}
