package edu.thesis.fct.client;

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

    protected Void doInBackground(Object... params) {
        Context context = (Context) params[0];
        String url = (String) params[1];
        String photoName = (String) params[2];
        String username = (String) params[3];

        WifiManager wifiMan = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String macAddress = wifiInf.getMacAddress();

        sendIndex(context, url, photoName, username, macAddress);

        return null;
    }

    private void sendIndex(Context context, String url, final String photoName, final String username, final String macAddress){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                params.put("photo_name", photoName);
                params.put("mac_address", macAddress);
                return params;
            }
        };
        Volley.newRequestQueue(context).add(stringRequest);
    }
}
