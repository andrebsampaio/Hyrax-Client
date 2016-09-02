package edu.thesis.fct.bluedirect;

import android.app.Activity;
import android.app.admin.SystemUpdatePolicy;
import android.content.Context;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    public static final int FANOUT = 3;
    public static final long INTERVAL = 200;
    public static final boolean REBROADCAST = true ;
    public static final boolean BROADCAST = false ;

    private static String fromMapToString(Map<String,Boolean> m){
        String result = "";
        int count = 0;
        for (Map.Entry e : m.entrySet()){
            if (count == m.size() - 1){
                result += e.getKey() + "," + e.getValue() + " ";
            } else {
                result += e.getKey() + "," + e.getValue();
            }
        }
        return result;
    }

    private static Map<String, Boolean> fromStringToMap(String s){
        String [] tmp = s.split(" ");
        Map<String,Boolean> result = new HashMap();
        for (String pair : tmp){
            String [] aux = pair.split(",");
            result.put(aux[0], Boolean.valueOf(aux[1]));
        }
        return result;
    }

    public static void broadcastQuery(String msg,File template, Packet p, Context activity, int fanout, boolean rebroadcast){
        if (!BluedirectActivity.fallback){
            //Map<String, Boolean> visited;
            Random r = new Random();
            int packetId = r.nextInt();
            //String query;
            if (!rebroadcast){
                //query = msg;
                /*visited = new HashMap<>();
                for (String k : MeshNetworkManager.routingTable.keySet()){
                    if (k.equals(MeshNetworkManager.getSelf().getMac()))
                        visited.put(k, true);
                    else {
                        visited.put(k,false);
                    }
                }*/
            } else {
                if (p.getTtl() == 0) return;
                /*String [] listAndQuery = new String(p.getData()).split("\\-");
                visited = fromStringToMap(listAndQuery[0]);
                query = listAndQuery[1];*/
            }

            //float probability = MeshNetworkManager.calculateProbability(pbias, visited);
            List<AllEncompasingP2PClient> toSend = new ArrayList<>();
            for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
               // if (visited.get(c.getMac()) != null && visited.get(c.getMac())) continue;

                /*if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()))
                    visited.put(c.getMac(), true);*/

                if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()) || (p != null && p.getSenderMac().equals(c.getMac())))
                    continue;

                if (c.getGroupID().equals(MeshNetworkManager.getSelf().getGroupID())
                        || ((c.getBridge() != null && MeshNetworkManager.getSelf().getBridge() != null)
                        && (MeshNetworkManager.getSelf().getBridge().getGID().equals(c.getGroupID())))){

                    toSend.add(c);
                }

                /*if (r.nextFloat() < probability && (!visited.get(c.getMac()) || !visited.containsKey(c.getMac()))){
                    toSend.add(c);
                    visited.put(c.getMac(),true);
                } else {
                    visited.put(c.getMac(),false);
                }*/
            }

            int remaining;
            if (toSend.size() < fanout){
                remaining = toSend.size();
            } else {
                remaining = fanout;
            }

            while (remaining > 0){
                AllEncompasingP2PClient rclient = toSend.get(r.nextInt(remaining));
                if (!rebroadcast){
                    /*Sender.queuePacket(new Packet(packetId,Packet.TYPE.QUERY, (fromMapToString(visited) + "-" + query).getBytes(), rclient.getMac(),
                            WiFiDirectBroadcastReceiver.MAC, rclient.getBtmac(), Configuration.getBluetoothSelfMac(activity)));*/
                    Sender.queuePacket(new Packet(packetId,Packet.TYPE.QUERY, bindUserAndTemplate(msg,template), rclient.getMac(),
                            WiFiDirectBroadcastReceiver.MAC, rclient.getBtmac(), Configuration.getBluetoothSelfMac(activity)));
                } else {
                    p.setBtRMac(rclient.getBtmac());
                    p.setMac(rclient.getMac());
                    int ttl = p.getTtl();
                    ttl--;
                    p.setTtl(ttl);
                    Sender.queuePacket(p);
                }
                toSend.remove(rclient);
                remaining--;
            }
            Receiver.seenIDs.add(packetId);

        } else {
            FBSender.queuePacket(new Packet(Packet.NEW_ID,Packet.TYPE.FB_QUERY,msg.getBytes(),null, Configuration.getFallbackId((Activity)activity),null,null),null);
        }

    }

    private static byte[] bindUserAndTemplate(String user, File template){
        try {
            FileInputStream fis = new FileInputStream(template);
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            IOUtils.copy(fis,bao);
            byte [] data = bao.toByteArray();
            byte [] u = user.getBytes();
            byte [] tmp = new byte [8 + u.length + data.length ];
            System.arraycopy(intToByte(u.length),0,tmp,0,4);
            System.arraycopy(intToByte(data.length),0,tmp,4,4);
            int offset = 8;
            System.arraycopy(u,0,tmp,offset,u.length);
            offset += u.length;
            System.arraycopy(data,0, tmp, offset, data.length);
            return tmp;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte [] intToByte(int x){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(x);

        return b.array();
    }

    public static void broadcastFile(byte[] file, Context activity){

        for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
            if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()))
                continue;
            Sender.queuePacket(new Packet(Packet.NEW_ID,Packet.TYPE.FILE, file, c.getMac(),
                    WiFiDirectBroadcastReceiver.MAC, c.getBtmac(), Configuration.getBluetoothSelfMac(activity)));
        }
    }

    public static void sendToClient(byte [] data, String mac,String btmac, Packet.TYPE type, Context activity){
        if (!BluedirectActivity.fallback){
            Sender.queuePacket(new Packet(Packet.NEW_ID,type,data,mac,WiFiDirectBroadcastReceiver.MAC,btmac,Configuration.getBluetoothSelfMac(activity)));
        } else {
            FBSender.queuePacket(new Packet(Packet.NEW_ID,Packet.TYPE.FB_QUERY,data,mac, Configuration.getFallbackId((Activity)activity),null,null),null);
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
