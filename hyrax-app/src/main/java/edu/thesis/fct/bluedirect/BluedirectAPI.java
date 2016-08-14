package edu.thesis.fct.bluedirect;

import android.app.Activity;
import android.content.Context;

import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.bluedirect.fallback.FBSender;
import edu.thesis.fct.bluedirect.fallback.GCMSender;
import edu.thesis.fct.bluedirect.router.AllEncompasingP2PClient;
import edu.thesis.fct.bluedirect.router.MeshNetworkManager;
import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.bluedirect.router.Receiver;
import edu.thesis.fct.bluedirect.router.Sender;
import edu.thesis.fct.bluedirect.wifi.WiFiDirectBroadcastReceiver;

/**
 * Created by abs on 10-07-2016.
 */
public class BluedirectAPI {

    public static void broadcastQuery(String msg, Context activity){
        // Send to other clients as a group chat message
        if (!BluedirectActivity.fallback){
            for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
                if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()))
                    continue;
                Sender.queuePacket(new Packet(Packet.TYPE.QUERY, msg.getBytes(), c.getMac(),
                        WiFiDirectBroadcastReceiver.MAC, c.getBtmac(), Configuration.getBluetoothSelfMac(activity)));
            }
        } else {
            FBSender.queuePacket(new Packet(Packet.TYPE.FB_QUERY,msg.getBytes(),null, Configuration.getFallbackId((Activity)activity),null,null),null);
        }

    }

    public static void broadcastFile(byte[] file, Context activity){

        for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
            if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()))
                continue;
            Sender.queuePacket(new Packet(Packet.TYPE.FILE, file, c.getMac(),
                    WiFiDirectBroadcastReceiver.MAC, c.getBtmac(), Configuration.getBluetoothSelfMac(activity)));
        }
    }

    public static void sendToClient(byte [] data, String mac,String btmac, Packet.TYPE type, Context activity){
        if (!BluedirectActivity.fallback){
            Sender.queuePacket(new Packet(type,data,mac,WiFiDirectBroadcastReceiver.MAC,btmac,Configuration.getBluetoothSelfMac(activity)));
        } else {
            FBSender.queuePacket(new Packet(Packet.TYPE.FB_QUERY,data,mac, Configuration.getFallbackId((Activity)activity),null,null),null);
        }
    }

    public static void setOnPacketReceivedListener(Receiver.onPacketReceivedListener listener){
        Receiver.setListener(listener);
    }

    public static void setOnGroupJoinedListener(MeshNetworkManager.onGroupJoinedListener listener){
        MeshNetworkManager.setJoinListener(listener);
    }

    public static void setOnGroupLeaveListener(MeshNetworkManager.onGroupLeaveListener listener){
        MeshNetworkManager.setLeaveListener(listener);
    }






}
