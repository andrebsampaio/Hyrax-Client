package edu.thesis.fct.bluedirect.fallback;

import android.content.SharedPreferences;
import android.os.Bundle;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.client.ImageModel;

/**
 * Created by abs on 11-08-2016.
 */
public class GCMSender {

    public static final String API_KEY = "AIzaSyBGOuE4LF4mUInjqq3EwjKlTfLJCny0Tno";

    public static void sendPacket(Packet p, ImageModel i){
        try {
            // Prepare JSON containing the GCM message content. What to send and where to send.
            JSONObject jGcmData = new JSONObject();
            JSONObject jData = new JSONObject();
            jData.put("my_id", p.getSenderMac());
            jData.put("type", p.getType().toString());
            if (p.getType().equals(Packet.TYPE.FB_COUNT)){
                int val = ByteBuffer.wrap(p.getData()).getInt();
                jData.put("message", val);
            } else {
                jData.put("message", new String(p.getData()));
            }

            if (p.getType().equals(Packet.TYPE.FB_DATA)){
                jData.put("image_info", i.toString());
            }
            // Where to send GCM message.
            if (!p.getMac().equals("00:00:00:00:00:00")) {
                jGcmData.put("to", p.getMac());
            } else {
                jGcmData.put("to", "/topics/global");
            }
            // What to send in GCM message.
            jGcmData.put("data", jData);

            // Create connection to send GCM Message request.
            URL url = new URL("https://android.googleapis.com/gcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "key=" + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Send GCM message content.
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(jGcmData.toString().getBytes());

            // Read GCM response.
            InputStream inputStream = conn.getInputStream();
            String resp = IOUtils.toString(inputStream);
            System.out.println(resp);
            System.out.println("Check your device/emulator for notification or logcat for " +
                    "confirmation of the receipt of the GCM message.");
        } catch (IOException e) {
            System.out.println("Unable to send GCM message.");
            System.out.println("Please ensure that API_KEY has been replaced by the server " +
                    "API key, and that the device's registration token is correct (if specified).");
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}


