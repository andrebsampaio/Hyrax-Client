package edu.thesis.fct.client;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
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
public class UploadImageTask extends AsyncTask<byte [], Void, Void> {

    HttpURLConnection httpUrlConnection;

    protected Void doInBackground(byte []... arg0) {
        String attachmentName = "image";
        String attachmentFileName = "bitmap.jpg";
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        String details = "details";


        HttpURLConnection httpUrlConnection = null;
        URL url = null;
        try {
            url = new URL("http://192.168.1.243:8080/hyrax-server/rest/upload");
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
            request.writeBytes("{\"id\" : \"balake\", \"location\" : \"potato\", \"time\" :" + System.currentTimeMillis() + "}" + crlf);

            request.writeBytes(crlf);

            request.writeBytes(twoHyphens + boundary + crlf);

            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    attachmentName + "\";filename=\"" +
                    attachmentFileName + "\"" + crlf);
            request.writeBytes("Content-Type: image/jpeg" + crlf);

            request.writeBytes(crlf);

            request.write(arg0[0]);

            request.writeBytes(crlf);

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
        return null;
    }
}
