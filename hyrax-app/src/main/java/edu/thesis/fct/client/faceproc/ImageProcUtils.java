package edu.thesis.fct.client.faceproc;


import static org.bytedeco.javacpp.opencv_core.CV_MINMAX;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvConvertScale;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvNormalize;
import static org.bytedeco.javacpp.opencv_core.cvPow;
import static org.bytedeco.javacpp.opencv_core.cvSub;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_GAUSSIAN;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;
import static org.bytedeco.javacpp.opencv_core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.helper.opencv_core.CvArr;

import android.content.Context;
import android.graphics.Bitmap;


public class ImageProcUtils {

    static IplImage  b1, b2, gf, tr;
    static  final int              WIDTH = 100;
    static  final int              HEIGHT = 100;
    

    public static void gammar(IplImage img, IplImage dist, IplImage temp, double correction) {
        cvConvertScale(img, temp, 1.0 / 255, 0);
        cvPow(temp, temp, correction);
        cvConvertScale(temp, dist, 255, 0);

    }

    public static IplImage process(Mat matimg) {

        IplImage img = MatToIplImage(matimg);
        //gr = cvCreateImage(cvGetSize(img), 8, 1);
        tr = cvCreateImage(cvGetSize(img), 8, 1);

        b1 = cvCreateImage(cvGetSize(img), 32, 1);
        b2 = cvCreateImage(cvGetSize(img), 32, 1);
        gf = cvCreateImage(cvGetSize(img), 32, 1);
        CvArr mask = IplImage.create(0, 0, 32, 1);
        //cvCvtColor(img, gr, CV_BGR2GRAY);
        //gammar(gr, gr, gf, 0.99);
        gammar(img, img, gf, 0.99);

        cvSmooth(gf, b1, CV_GAUSSIAN, 3, 0, 0, 0);
        cvSmooth(gf, b2, CV_GAUSSIAN, 7, 0, 0, 0);
        cvSub(b1, b2, b2, mask);

        //cvConvertScale(b2, gr, 128, 128);
        //cvEqualizeHist(gr, gr);
        //cvNormalize(gr, gr, 0, 255, CV_MINMAX, null);

        cvConvertScale(b2, img, 128, 128);
        //cvEqualizeHist(img, img);
        cvNormalize(img, img, 0, 255, CV_MINMAX, null);

        return img;
    }


    public static IplImage process(IplImage img) {

        //IplImage img = MatToIplImage(matimg);
        //gr = cvCreateImage(cvGetSize(img), 8, 1);
        tr = cvCreateImage(cvGetSize(img), 8, 1);

        b1 = cvCreateImage(cvGetSize(img), 32, 1);
        b2 = cvCreateImage(cvGetSize(img), 32, 1);
        gf = cvCreateImage(cvGetSize(img), 32, 1);
        CvArr mask = IplImage.create(0, 0, 32, 1);
        //cvCvtColor(img, gr, CV_BGR2GRAY);
        //gammar(gr, gr, gf, 0.99);
        gammar(img, img, gf, 0.99);

        cvSmooth(gf, b1, CV_GAUSSIAN, 3, 0, 0, 0);
        cvSmooth(gf, b2, CV_GAUSSIAN, 7, 0, 0, 0);
        cvSub(b1, b2, b2, mask);

        //cvConvertScale(b2, gr, 128, 128);
        //cvEqualizeHist(gr, gr);
        //cvNormalize(gr, gr, 0, 255, CV_MINMAX, null);

        cvConvertScale(b2, img, 128, 128);
        //cvEqualizeHist(img, img);
        cvNormalize(img, img, 0, 255, CV_MINMAX, null);

        return img;
    }

    public static IplImage MatToIplImage(Mat m)
    {
        return ImageProcUtils.MatToIplImage(m);
    }

    public static IplImage BitmapToIplImage(Bitmap bmp, int width, int height) {

        if ((width != -1) || (height != -1)) {
            Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, width, height, false);
            bmp = bmp2;
        }

        IplImage image = IplImage.create(bmp.getWidth(), bmp.getHeight(),
                IPL_DEPTH_8U, 4);

        bmp.copyPixelsToBuffer(image.getByteBuffer());

        IplImage grayImg = IplImage.create(image.width(), image.height(),
                IPL_DEPTH_8U, 1);

        cvCvtColor(image, grayImg, CV_BGR2GRAY);

        return grayImg;
    }
    
    public static File getfileFromResources(Context mContex,int resID,String filename) throws Exception
    {
        InputStream is = mContex.getResources().openRawResource(resID);
        File fileDir = mContex.getDir("cascade", Context.MODE_PRIVATE);
        File filetoreturn = new File(fileDir,filename);
        FileOutputStream os = new FileOutputStream(filetoreturn);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();

        return filetoreturn;
    }

}
