package edu.thesis.fct.client;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by abs on 07-03-2016.
 */
public class UploadImageTask extends AsyncTask<Object, Void, Void> {

    HttpURLConnection httpUrlConnection;
    final static String crlf = "\r\n";
    final static String twoHyphens = "--";
    final static String boundary = "*****";

    private File [] processImage(Context context, File name, int cameraLens){
        FaceProcessing f = new FaceProcessing(context);
        //f.detectFaces(name, cameraLens);
        return f.detectFaces(name, cameraLens);
    }

    protected Void doInBackground(Object... params) {

        String details = "details";
        Context context = (Context) params[0];
        File name = (File) params[1];
        int cameraLens = (int)params[2];

        //File [] faces = processImage(context, name, cameraLens);

        NetworkInfoHolder nih = NetworkInfoHolder.getInstance();
        HttpURLConnection httpUrlConnection = null;
        URL url = null;
        if (nih.getHost() != null) {
            try {
                url = new URL("http://" + nih.getHost().getHostAddress()  + ":" + nih.getPort()  + "/hyrax-server/rest/upload");
                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoOutput(true);


                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                httpUrlConnection.setRequestProperty(
                        "Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(
                        httpUrlConnection.getOutputStream());

                request.writeBytes(twoHyphens + boundary + crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"" + details + "\"" + crlf);
                request.writeBytes(crlf);
                request.writeBytes("{ \"location\" : \"potato\", \"time\" :" + System.currentTimeMillis() + "}" + crlf);

                request.writeBytes(crlf);

                sendFile(request, "image", name);

                /*for (File f : faces){
                    request.writeBytes(crlf);
                    sendFile(request, "face", f);
                }*/

                request.writeBytes(twoHyphens + boundary +
                        twoHyphens + crlf);

                request.flush();
                request.close();

                InputStream responseStream = null;
                try {
                    responseStream = new
                            BufferedInputStream(httpUrlConnection.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                BufferedReader responseStreamReader =
                        new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                try {
                    while ((line = responseStreamReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }

                    responseStreamReader.close();

                    String response = stringBuilder.toString();

                    Log.d("UPLOAD", response);

                    responseStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d("UPLOAD", "SUPER POTATO");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void sendFile(DataOutputStream request, String field, File f){
        try {
            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    field + "\";filename=\"" +
                    f.getName() + "\"" + crlf);
            request.writeBytes("Content-Type: image/jpeg" + crlf);

            request.writeBytes(crlf);

            FileInputStream fileInputStream = new FileInputStream(f);
            byte[] buffer = new byte[1024*1024];
            int bytesRead = 0;

            while((bytesRead = fileInputStream.read(buffer))>0)
            {
                request.write(buffer, 0, bytesRead);
            }

            fileInputStream.close();

            request.writeBytes(crlf);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
