package edu.thesis.fct.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Environment;

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
    static final int WIFI_DATA = 0;
    static final int MOBILE_DATA = 1;
    static final int IMG_LIST_RQ = 4;
    static final int S2P_TRANSFER = 5;
    static final int DOWNLOAD_RQ = 6;
    static final String SEPARATOR = ",";
    int transferProtocol;
    int networkType;

    double battery;
    long totalBytesRx;
    long totalBytesTx;
    long totalPacketsRx;
    long totalPacketsTx;
    long totalTransferTime;
    long imagesRqLatency;
    long DownloadRqLatency;
    long s2pTransferLatency;

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

    public void registerLatency(int request){
        if (request == IMG_LIST_RQ){
            imagesRqLatency = System.currentTimeMillis();
        } else if (request == DOWNLOAD_RQ) {
            DownloadRqLatency = System.currentTimeMillis();
        } else if (request == S2P_TRANSFER) {
            s2pTransferLatency = System.currentTimeMillis();
        }
    }

    public void calculateLatency(int request){
        if (request == IMG_LIST_RQ){
            imagesRqLatency = System.currentTimeMillis() - imagesRqLatency;
        } else if (request == DOWNLOAD_RQ) {
            DownloadRqLatency = System.currentTimeMillis() - DownloadRqLatency;
        } else if (request == S2P_TRANSFER) {
            s2pTransferLatency = System.currentTimeMillis() - s2pTransferLatency;
        }
    }

    public void setNetworkTestType(int type){
        networkType = type;
    }

    private double currentBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return (level*100) / (double)scale;
    }



    private long getBytesReceived(){
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

    private void storeToFile(double battery, long bytesRx, long bytesTx, long packetsRx, long packetsTx, long time){
        PrintWriter writer = null;
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "HyraxTests" + File.separator + testSession + ".txt" );

            if (!file.exists()) file.getParentFile().mkdirs();

            boolean existedFile = file.exists();

            writer = new PrintWriter(new FileWriter(file,true));

            if (!existedFile){
                writer.println("battery | totalBytesRx | totalBytesTx | totalPacketsRx | totalPacketsTx | time(ms) | Images RQ (ms) | Images Download Latency (ms)");
            }
            writer.println(battery + SEPARATOR + bytesRx + SEPARATOR + bytesTx +
                    SEPARATOR + packetsRx + SEPARATOR + packetsTx + SEPARATOR + time
                    + SEPARATOR + imagesRqLatency
                    + SEPARATOR + DownloadRqLatency);
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
}
