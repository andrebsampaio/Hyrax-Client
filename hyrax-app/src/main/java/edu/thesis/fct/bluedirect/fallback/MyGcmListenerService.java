

package edu.thesis.fct.bluedirect.fallback;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.bluedirect.router.Receiver;
import edu.thesis.fct.bluedirect.router.Sender;
import edu.thesis.fct.bluedirect.wifi.WiFiDirectBroadcastReceiver;
import edu.thesis.fct.client.LoginActivity;
import edu.thesis.fct.client.R;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";



    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From: " + from);

        if (WiFiDirectBroadcastReceiver.r == null)
            WiFiDirectBroadcastReceiver.startReceiverAndSender();
        Packet.TYPE type = Packet.TYPE.valueOf(data.getString("type"));
        String id = data.getString("my_id");

        Receiver.packetQueue.add(new Packet(type,data.getString("message").getBytes(), null, id,null,null));

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(data.getString("message"));
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        System.out.println(message);
    }
}
