package edu.thesis.fct.bluedirect.fallback;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.client.ImageModel;
import edu.thesis.fct.client.MultipartRequest;
import edu.thesis.fct.client.MySingleton;

/**
 * Created by abs on 13-08-2016.
 */
public class FileSender {

    final static String lineEnd = "\r\n";
    final static String twoHyphens = "--";
    final static String boundary = "*****";
    final static String mimeType = "multipart/form-data;boundary=" + boundary;
    final static String TOKEN_PARAM = "token";
    final static String USERNAME_PARAM = "username";
    final static String DETAILS = "details";

    static onFileReceivedListener listener;

    public static void setOnFileReceivedListener(onFileReceivedListener l){
        listener = l;
    }

    public static void sendFile(String destToken, File f, String url, Context context, final ImageModel img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        buildTextPart(dos,TOKEN_PARAM, destToken);
        buildTextPart(dos,DETAILS, img.toString());
        buildPart(dos, IOUtils.toByteArray(new FileInputStream(f)), f.getName());
        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
        byte[] multipartBody = bos.toByteArray();

        MultipartRequest multipartRequest = new MultipartRequest(url, null, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                //listener.onFileReceived(response, img);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        });

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(180000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(context).addToRequestQueue(multipartRequest);
    }

    public static void sendQueryFile(File f, String url, Context context) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        buildTextPart(dos,TOKEN_PARAM, Configuration.getFallbackId((Activity)context));
        SharedPreferences pref = context.getSharedPreferences("MyPref", context.MODE_PRIVATE);
        String username = pref.getString("username",null);
        buildTextPart(dos,USERNAME_PARAM,username);
        buildPart(dos, IOUtils.toByteArray(new FileInputStream(f)), f.getName());
        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
        byte[] multipartBody = bos.toByteArray();

        MultipartRequest multipartRequest = new MultipartRequest(url, null, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        });

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(180000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(context).addToRequestQueue(multipartRequest);
    }



    private static void buildPart(DataOutputStream dataOutputStream, byte[] fileData, String fileName) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\""
                + fileName + "\"" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
        int bytesAvailable = fileInputStream.available();

        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        // read file and write it into form...
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        dataOutputStream.writeBytes(lineEnd);
    }

    public interface onFileReceivedListener{
        public abstract void onFileReceived(NetworkResponse response, ImageModel img);
    }

    private static void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + lineEnd);
        dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);
        dataOutputStream.writeBytes(parameterValue + lineEnd);
    }


}
