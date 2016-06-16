package edu.thesis.fct.client;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by abs on 20-05-2016.
 */
public class BluetoothClient {

    private static final String TAG = "BluetoothClient";

    private boolean isRunning  = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID UUID_KEY = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    BluetoothAdapter mBluetoothAdapter;
    IntentFilter filter;
    List<ImageModel> images;
    Map<UserDevice, List<ImageModel>> deviceImageIndex = new HashMap<>();
    Context context;
    private boolean isRequesting;
    private ProgressDialog progressDialog;
    GalleryAdapter mAdapter;
    String mMacBT;
    String mMacWD;
    boolean hasWD;
    InstrumentationUtils iu;
    final String addDeviceURL;

    public BluetoothClient(Context context, ProgressDialog progressDialog, GalleryAdapter mAdapter, InstrumentationUtils iu){
        this.context = context;
        this.mAdapter = mAdapter;
        this.progressDialog = progressDialog;
        this.iu = iu;

        NetworkInfoHolder nih = NetworkInfoHolder.getInstance();
        Log.d("NIH", nih.getHost() + "");
        if (nih.getHost() != null){
            addDeviceURL = "http://" + nih.getHost().getHostAddress()  + ":" + nih.getPort()  + "/hyrax-server/rest/adddevice/";
        } else addDeviceURL = null;

        SharedPreferences pref = context.getApplicationContext().getSharedPreferences("MyPref", context.MODE_PRIVATE);
        this.mMacBT = pref.getString("macbt", null);
        this.mMacWD = pref.getString("macwd", null);
        this.hasWD = pref.getBoolean("haswd", false);
    }

    public void startConnection(String url, String username){
        turnBluetoothOn(context);
        getImages(url, username);
    }

    private class IndexProcess extends AsyncTask<Void, Void, Void> {

        String response;

        public IndexProcess(String response){
            this.response = response;
        }

        protected Void doInBackground(Void... s) {
            if (response.equals("null")){
                //Toast.makeText(context, "No pictures found of you", Toast.LENGTH_LONG).show();
            } else {
                images = new ArrayList<>();
                deviceImageIndex = new HashMap<>();
                JSONObject JSONresp;
                try {
                    JSONresp = new JSONObject(response);
                    JSONObject object = JSONresp.getJSONObject("imageDAO");
                    processImageJSON(object);
                } catch (JSONException e1) {
                    try {
                        JSONresp = new JSONObject(response);
                        JSONArray ja = JSONresp.getJSONArray("imageDAO");
                        for (int i = 0; i < ja.length(); i++) {
                            JSONObject jsonObject = ja.getJSONObject(i);
                            processImageJSON(jsonObject);
                        }

                    } catch (JSONException e2) {
                        e2.printStackTrace();
                    }
                }
                retrievePhotosFromDevices();
            }
            return null;
        }

        protected void onProgressUpdate(Void... progress) {

        }
    }

    private void getImages(String url, final String username){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        iu.calculateLatency(InstrumentationUtils.IMG_LIST_RQ);
                        iu.calculateBytes(InstrumentationUtils.BYTES_C2S, InstrumentationUtils.RX);
                        iu.calculateBytes(InstrumentationUtils.BYTES_C2S, InstrumentationUtils.TX);
                        iu.calculatePackets(InstrumentationUtils.PACKETS_C2S, InstrumentationUtils.RX);
                        iu.calculatePackets(InstrumentationUtils.PACKETS_C2S, InstrumentationUtils.TX);
                        new IndexProcess(response).execute();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error.networkResponse.toString());
                    }
                }){

            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("person_name", username);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }

        };

        MySingleton.getInstance(context).addToRequestQueue(stringRequest);
        iu.registerBytes(InstrumentationUtils.RX);
        iu.registerBytes(InstrumentationUtils.TX);
        iu.registerPackets(InstrumentationUtils.RX);
        iu.registerPackets(InstrumentationUtils.TX);
        iu.registerLatency(InstrumentationUtils.IMG_LIST_RQ);
    }

    private void processImageJSON(JSONObject object){
        List<UserDevice> devices;
        devices = JSONtoDeviceList(object);
        ImageModel image = JSONtoImage(object,devices);
        addToDeviceIndex(deviceImageIndex,devices,image);
        images.add(image);
    }

    private ImageModel JSONtoImage(JSONObject jsonObject, List<UserDevice> devices){
        int id = jsonObject.optInt("id");
        String location = jsonObject.optString("location");
        String time = jsonObject.optString("time");
        return new ImageModel(id,location,time,devices);
    }

    private List<UserDevice> JSONtoDeviceList(JSONObject json){
        Object deviceList;
        List<UserDevice> devices = new ArrayList<>();
        try{
            deviceList = json.getJSONObject("devices");
            String dbt = ((JSONObject) deviceList).optString("deviceBT");
            String dwd = ((JSONObject) deviceList).optString("deviceWD");
            devices.add(new UserDevice(dbt,dwd));

        } catch (JSONException je){
            try {
                deviceList = json.getJSONArray("devices");
                for (int i = 0; i < ((JSONArray)deviceList).length(); i++){
                    JSONObject d = null;
                    try {
                        d = ((JSONArray)deviceList).getJSONObject(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String dbt = d.optString("deviceBT");
                    String dwd = d.optString("deviceWD");
                    devices.add(new UserDevice(dbt,dwd));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return devices;
    }

    private void addToDeviceIndex(Map<UserDevice, List<ImageModel>> map, List<UserDevice> devices, ImageModel image){
        for (UserDevice u : devices){
            List<ImageModel> tmp = map.get(u);
            if (tmp == null){
                tmp = new ArrayList<>();
                tmp.add(image);
                map.put(u, tmp);
            } else {
                tmp.add(image);
                map.put(u, tmp);
            }

        }
    }

    private List<ImageModel> checkDownloaded(List<ImageModel> toDownload){
        if (toDownload == null) return null;
        List<ImageModel> filtered = new ArrayList<>();
        File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "Hyrax");
        for (ImageModel i : toDownload){
            File tmp = new File(file.getAbsolutePath() + File.separator + i.getPhotoName());
            if (!tmp.exists()) filtered.add(i);
        }
        return filtered;
    }

    private void turnBluetoothOn(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(context, "This device does not support bluetooth!!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(btIntent);
        }
    }

    private void retrievePhotosFromDevices(){
        iu.registerLatency(InstrumentationUtils.P2P_TRANSFER);
        for (UserDevice u : deviceImageIndex.keySet()){
            if (!u.getMacBT().equals(mMacBT) && !u.getMacWD().equals(mMacWD)){
                List<ImageModel> imageList = checkDownloaded(deviceImageIndex.get(u));
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(u.getMacBT());
                if (imageList != null && imageList.size() > 0) {
                    Log.d("BT DEBUG", "im client");
                    Thread connect = new ConnectThread(device, imageList, u.getMacWD());
                    connect.start();
                    try {
                        connect.join();
                        System.out.println("I FINISHED");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;
        private final List<ImageModel> imageNames;
        private final String wifiMac;

        public ConnectThread(BluetoothDevice device, List<ImageModel> imageNames, String wifiMac)
        {
            this.wifiMac = wifiMac;
            this.imageNames = imageNames;
            BluetoothSocket temp = null;
            bluetoothDevice = device;
            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(UUID_KEY);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = temp;
        }
        public void run()
        {
            if(mBluetoothAdapter.isDiscovering())
            {
                mBluetoothAdapter.cancelDiscovery();
            }
            try {
                bluetoothSocket.connect();
                ConnectedThread connected = new ConnectedThread(bluetoothSocket, imageNames, wifiMac);
                connected.start();
                Log.d("BT DEBUG", "connected to:" + bluetoothDevice.getName());
            } catch (IOException connectException) {
                connectException.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }
        }
        @SuppressWarnings("unused")
        public void cancel()
        {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class ConnectedThread extends Thread {
        BluetoothSocket mSocket;
        DataInputStream input;
        DataOutputStream output;
        List<ImageModel> imageNames;
        String wifiMac;

        public ConnectedThread(BluetoothSocket socket, List<ImageModel> imageNames, String wifiMac) {
            mSocket = socket;
            this.wifiMac = wifiMac;
            DataInputStream tmpIn = null;
            DataOutputStream tmpOut = null;
            this.imageNames = imageNames;
            try {
                tmpIn = new DataInputStream(mSocket.getInputStream());
                tmpOut = new DataOutputStream(mSocket.getOutputStream());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            input = tmpIn;
            output = tmpOut;

        }
        public void run(){
            boolean otherWD;
            try {
                otherWD = input.readBoolean();
                output.writeBoolean(hasWD);
                if (hasWD && otherWD){
                    Thread wifiTransfer = new Thread() {
                        public void run() {
                            iu.setTransferProtocol(InstrumentationUtils.WD);
                            WifiDirectTransfer wd = new WifiDirectTransfer(context, true, wifiMac, imageNames, progressDialog, mAdapter, iu);
                        }
                    };

                    wifiTransfer.start();
                    try {
                        wifiTransfer.join();
                        System.out.println("WD finished");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    iu.setTransferProtocol(InstrumentationUtils.BT);
                    output.writeInt(imageNames.size());
                    Log.d("BT SEND FILE", "POTATO MIX FIRST STAGE");
                    for (ImageModel name : imageNames){
                        output.writeUTF(name.getPhotoName());
                    }

                    int numberFiles = input.readInt();
                    Log.d("BT SEND FILE", "Number of files to send: " + numberFiles);
                    for (int i = 0; i < numberFiles; i++){
                        String name = input.readUTF();
                        String [] nameSplit = name.split("\\.");
                        Long fileSize = input.readLong();

                        File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "Hyrax" + File.separator + nameSplit[0] + File.separator + name);
                        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

                        FileOutputStream fos = new FileOutputStream(file);
                        OutputStream bos = new BufferedOutputStream(fos);

                        copyStream(input, bos,fileSize);
                        bos.close();

                        if (i == numberFiles-1){
                            sendDeviceToIndex(context, getImageModelByName(nameSplit[0],imageNames).getId(), mMacBT, mMacWD, addDeviceURL,iu);
                        } else {
                            sendDeviceToIndex(context, getImageModelByName(nameSplit[0],imageNames).getId(), mMacBT, mMacWD, addDeviceURL,null);
                        }
                    }

                    output.writeBoolean(true);

                    final String sentMsg = "File received";
                    Log.d(TAG, sentMsg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }

    private static void sendDeviceToIndex(final Context context, final int id, final String macBT, final String macWD, String url, final InstrumentationUtils iu){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (iu != null){
                            iu.calculateLatency(InstrumentationUtils.ADD_INDEX_RQ);
                            iu.endTest();
                        }
                        //Toast.makeText(context, "Device added to index" ,Toast.LENGTH_SHORT).show();
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
                params.put("imageid", String.valueOf(id));
                params.put("macwd", macWD);
                params.put("macbt", macBT);
                return params;
            }
        };
        Volley.newRequestQueue(context).add(stringRequest);
        if (iu != null){
            iu.registerLatency(InstrumentationUtils.ADD_INDEX_RQ);
        }

    }

    private static void copyStream(InputStream input, OutputStream output, Long fileSize)
            throws IOException {
        int bytesRead = 0;
        try {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            // we need to know how may bytes were read to write them to the byteBuffer
            Long len = fileSize;

            while (len > 0) {
                int bytes = input.read(buffer, 0, (int)Math.min(buffer.length,len));
                bytesRead += bytes;
                len -= bytes;
                output.write(buffer, 0, bytes);
            }
            Log.d("BT DEBUG", bytesRead + "");
        } finally {
            Log.d("BT DEBUG", bytesRead + "");
            output.flush();
            //output.close();
        }
    }

    private static ImageModel getImageModelByName(String name, List<ImageModel> images){
        for (ImageModel i : images){
            if (i.getPhotoName().equals(name)) return i;
        }
        return null;
    }


}