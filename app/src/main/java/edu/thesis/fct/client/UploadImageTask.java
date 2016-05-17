package edu.thesis.fct.client;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by abs on 07-03-2016.
 */
public class UploadImageTask extends AsyncTask<Object, Void, Void> {
    HttpURLConnection httpUrlConnection;
    final static String crlf = "\r\n";
    final static String twoHyphens = "--";
    final static String boundary = "*****";

    protected Void doInBackground(Object... params) {
        Context context = (Context) params[0];
        String url = (String) params[1];
        String location = (String) params[2];
        String time = (String) params[3];
        File f = (File) params[4];

        WifiManager wifiMan = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String macAddressWD = wifiInf.getMacAddress();
        String macaddressBT = BluetoothAdapter.getDefaultAdapter().getAddress();

        sendIndex(f, url, location, time, macAddressWD, macaddressBT);

        return null;
    }

    private void sendIndex(File f ,String urlUpload, String location, String time, String macAddressWD, String macAddressBT){
        URL url;
        try {
            url = new URL(urlUpload);
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
            request.writeBytes("Content-Disposition: form-data; name=\"" + "details" + "\"" + crlf);
            request.writeBytes(crlf);
            request.writeBytes("{\"location\" :\"" + location + "\"," + "\"time\" :\"" + time + "\"," + "\"deviceWD\" :\"" + macAddressWD + "\"," + "\"deviceBT\" :\"" + macAddressBT + "\"}" + crlf);

            request.writeBytes(crlf);

            sendFile(request, "image", f);

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
