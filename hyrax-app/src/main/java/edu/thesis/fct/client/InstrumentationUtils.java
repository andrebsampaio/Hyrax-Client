package edu.thesis.fct.client;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.SystemClock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Created by abs on 08-06-2016.
 */
public class InstrumentationUtils {

    Context context;
    int appId;
    public static final int WIFI_DATA = 0;
    public static final int MOBILE_DATA = 1;
    public static final int BT = 2;
    public static final int WD = 3;
    public static final int IMG_LIST_RQ = 4;
    public static final int ADD_INDEX_RQ = 5;
    public static final int P2P_TRANSFER = 6;
    public static final int BYTES_C2C = 7;
    public static final int BYTES_C2S = 8;
    public static final int PACKETS_C2C = 9;
    public static final int PACKETS_C2S = 10;
    public static final int RX = 11;
    public static final int TX = 12;
    public static final int RECOG = 13;
    static final String SEPARATOR = ",";
    int transferProtocol;
    int networkType;

    float battery;
    long totalBytesRx;
    long totalBytesTx;
    long totalPacketsRx;
    long totalPacketsTx;
    long tmpBytesRx;
    long tmpBytesTx;
    long tmpPacketsRx;
    long tmpPacketsTx;
    long packetsC2CRx;
    long packetsC2SRx;
    long packetsC2CTx;
    long packetsC2STx;
    long bytesC2CRx;
    long bytesC2SRx;
    long bytesC2CTx;
    long bytesC2STx;
    long totalTransferTime;
    long imagesRqLatency;
    long addIndexRqLatency;
    long p2pTransferLatency;
    long recogLatency;

    int testSession;

    public InstrumentationUtils(Context context){
        this.context = context;
        testSession = context.getSharedPreferences("MyPref", context.MODE_PRIVATE).getInt("testSession", -1);
        System.out.println(testSession);
        if (testSession == -1){
          testSession = this.newTestSession(context);
        }

        try {
            this.appId = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void registerBytes(int RxOrTx){
        if (RxOrTx == RX){
            tmpBytesRx = getBytesReceived();
        } else {
            tmpBytesTx = getBytesSent();
        }
    }

    public void registerPackets(int RxOrTx){
        if (RxOrTx == RX){
            tmpPacketsRx = getPacketsReceived();
        } else {
            tmpPacketsTx = getPacketsSent();
        }
    }

    public void calculatePackets(int actors, int RxOrTx){
        if (RxOrTx == RX){
            if (actors == PACKETS_C2C){
                packetsC2CRx += getPacketsReceived() - tmpPacketsRx;
            } else if (actors == PACKETS_C2S){
                packetsC2SRx += getPacketsReceived() - tmpPacketsRx;
            }
        } else {
            if (actors == PACKETS_C2C){
                packetsC2CTx = getPacketsSent() - tmpPacketsTx;
            } else if (actors == PACKETS_C2S){
                packetsC2STx = getPacketsSent() - tmpPacketsTx;
            }
        }
    }

    public void calculateBytes(int actors, int RxOrTx){
        if (RxOrTx == RX){
            if (actors == BYTES_C2C){
                bytesC2CRx += getBytesReceived() - tmpBytesRx;
            } else if (actors == BYTES_C2S){
                bytesC2SRx += getBytesReceived() - tmpBytesRx;
            }
        } else {
            if (actors == BYTES_C2C){
                bytesC2CTx = getBytesSent() - tmpBytesTx;
            } else if (actors == BYTES_C2S){
                bytesC2STx = getBytesSent() - tmpBytesTx;
            }
        }
    }

    public void registerLatency(int request){
        if (request == IMG_LIST_RQ){
            imagesRqLatency = System.currentTimeMillis();
        } else if (request == ADD_INDEX_RQ) {
            addIndexRqLatency = System.currentTimeMillis();
        } else if (request == P2P_TRANSFER) {
            p2pTransferLatency = System.currentTimeMillis();
        } else if (request == RECOG){
            recogLatency = System.currentTimeMillis();
        }
    }

    public void calculateLatency(int request){
        if (request == IMG_LIST_RQ){
            imagesRqLatency = System.currentTimeMillis() - imagesRqLatency;
        } else if (request == ADD_INDEX_RQ) {
            addIndexRqLatency = System.currentTimeMillis() - addIndexRqLatency;
        } else if (request == P2P_TRANSFER) {
            p2pTransferLatency = System.currentTimeMillis() - p2pTransferLatency;
        } else if (request == RECOG){
            recogLatency = System.currentTimeMillis() - recogLatency;
        }
    }

    public void setNetworkTestType(int type){
        networkType = type;
    }

    public void setTransferProtocol(int type){
        transferProtocol = type;
    }

    private float currentBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float)scale;
    }

    public long getBytesReceived(){
        return TrafficStats.getUidRxBytes(appId);
    }

    private long getBytesSent(){
        return TrafficStats.getUidTxBytes(appId);
    }

    private long getPacketsReceived(){
        return TrafficStats.getUidRxPackets(appId);
    }

    private long getPacketsSent(){
        return TrafficStats.getUidTxPackets(appId);
    }

    private void storeToFile(float battery, long bytesRx, long bytesTx, long packetsRx, long packetsTx, long time){
        PrintWriter writer = null;
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "HyraxTests" + File.separator + testSession + ".txt" );

            if (!file.exists()) file.getParentFile().mkdirs();

            boolean existedFile = file.exists();

            writer = new PrintWriter(new FileWriter(file,true));

            if (!existedFile){
                writer.println("battery | totalBytesRx | totalBytesTx | totalPacketsRx | totalPacketsTx | C2CBytesRx | C2CBytesTx |  C2SBytesRx | C2SBytesTx | C2CPacketsRx | C2CPacketsTx |  C2SPacketsRx | C2SPacketsTx | time(ms) | transferBy | Images RQ (ms) | Add Index RQ (ms) | P2P transfer (ms)");
            }
            writer.println(battery + SEPARATOR + bytesRx + SEPARATOR + bytesTx +
                    SEPARATOR + packetsRx + SEPARATOR + packetsTx + SEPARATOR + bytesC2CRx
                    + SEPARATOR + bytesC2CTx + SEPARATOR + bytesC2SRx
                    + SEPARATOR + bytesC2STx
                    + SEPARATOR + packetsC2CRx + SEPARATOR + packetsC2CTx + SEPARATOR + packetsC2SRx
                    + SEPARATOR + packetsC2STx + SEPARATOR + time
                    + SEPARATOR + protocolToString(transferProtocol) + SEPARATOR + imagesRqLatency
                    + SEPARATOR + addIndexRqLatency + SEPARATOR + p2pTransferLatency
                    + SEPARATOR + recogLatency);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            writer.close();
        }
    }

    public void startTest(){
        totalTransferTime = System.currentTimeMillis();
        battery = this.currentBattery();
        totalBytesRx = this.getBytesReceived();
        totalBytesTx = this.getBytesSent();
        totalPacketsRx = this.getPacketsReceived();
        totalPacketsTx = this.getPacketsSent();
    }

    public void endTest(){
        totalTransferTime = System.currentTimeMillis() - totalTransferTime;
        battery = battery - this.currentBattery();
        totalBytesRx = this.getBytesReceived() - totalBytesRx;
        totalBytesTx = this.getBytesSent() - totalBytesTx;
        totalPacketsRx = this.getPacketsReceived() - totalPacketsRx;
        totalPacketsTx = this.getPacketsSent() - totalPacketsTx;

        this.storeToFile(battery , totalBytesRx, totalBytesTx, totalPacketsRx, totalPacketsTx, totalTransferTime );
    }

    public static int newTestSession(Context context){

        SharedPreferences.Editor editor = context.getSharedPreferences("MyPref", context.MODE_PRIVATE).edit();
        int testSession = -1;
        int randomN = getRandomNumberInRange(0,1000);
        File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "HyraxTests" + File.separator + randomN + ".txt" );
        if (!file.exists()) file.getParentFile().mkdirs();
        while (file.exists()){
            randomN = getRandomNumberInRange(0,1000);
            file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "HyraxTests" + File.separator + randomN + ".txt" );
        }
        if (randomN != -1){
            testSession = randomN;
        }

        editor.putInt("testSession", testSession);

        editor.commit();

        return testSession;
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    private String protocolToString(int protocol){
        if (protocol == BT) return "BT";
        return "WD";
    }




}
