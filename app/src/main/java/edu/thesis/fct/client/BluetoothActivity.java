package edu.thesis.fct.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by abs on 11-05-2016.
 */
public class BluetoothActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID UUID_KEY = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    BluetoothAdapter mBluetoothAdapter;
    IntentFilter filter;
    List<ImageModel> images;
    Map<String, List<ImageModel>> deviceImageIndex = new HashMap<>();

    private void getImages(String url, final String username){

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
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
                            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            List<ImageModel> imageList = checkDownloaded(deviceImageIndex.get("40:C6:2A:C9:C1:C7"));
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("40:C6:2A:C9:C1:C7");
                            if (imageList != null && imageList.size() > 0) {
                                Log.d("BT DEBUG", "im client");
                                Thread connect = new ConnectThread(device, imageList);
                                connect.start();
                            }
                        }

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

        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
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

    private void addToDeviceIndex(Map<String, List<ImageModel>> map, List<UserDevice> devices, ImageModel image){
        for (UserDevice u : devices){
            List<ImageModel> tmp = map.get(u.getMacBT());
            if (tmp == null){
                tmp = new ArrayList<>();
                tmp.add(image);
                map.put(u.getMacBT(), tmp);
            } else {
                tmp.add(image);
                map.put(u.getMacBT(), tmp);
            }

        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                List<ImageModel> imageList = checkDownloaded(deviceImageIndex.get(device.getAddress()));
                if (imageList != null && imageList.size() > 0){
                    Log.d("BT DEBUG", "im client");
                    Thread connect = new ConnectThread(device, imageList);
                    connect.start();
                } else {
                    Log.d("BT DEBUG", "All files already downloaded, searching again");
                    searchForBluetoothDevices();
                }

                Log.e("Bluetooth Device", device.getName() + " " + device.getAddress());
            }
        }
    };


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, BluetoothServerService.class);
        startService(intent);

        if (!Build.MODEL.equals("m2")){
            BluetoothClient bc = new BluetoothClient(this,"http://192.168.1.243:8080/hyrax-server/rest/search", "george_clooney" );
        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            searchForBluetoothDevices();
        }
    }

    private void searchForBluetoothDevices() {
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
            startActivity(intent);
        }
        boolean discovery = mBluetoothAdapter.startDiscovery();
        if(discovery) {
            filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        } else {
            Toast.makeText(this, "Error in starting discovery", Toast.LENGTH_LONG).show();
        }
    }

    private void copyStream(InputStream input, OutputStream output, Long fileSize)
            throws IOException
    {
        int bytesRead = 0;
        try {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            // we need to know how may bytes were read to write them to the byteBuffer
            Long len = fileSize;

            while (len > 0) {
                int bytes = input.read(buffer, 0, buffer.length);
                bytesRead += bytes;
                len -= bytes;
                output.write(buffer, 0, bytes);
            }
            Log.d("BT DEBUG", bytesRead+"");
        } finally {
            Log.d("BT DEBUG", bytesRead+"");
            output.flush();
            //output.close();
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;
        private final List<ImageModel> imageNames;

        public ConnectThread(BluetoothDevice device, List<ImageModel> imageNames)
        {
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
                ConnectedThread connected = new ConnectedThread(bluetoothSocket, imageNames);
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

        public ConnectedThread(BluetoothSocket socket, List<ImageModel> imageNames) {
            mSocket = socket;
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
            Context context = getApplicationContext();
            Intent intent = new Intent(context, WiFiDirectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        }
        /*public void run() {
            try {
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
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    copyStream(input, bos,fileSize);
                }

                final String sentMsg = "File received";
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), sentMsg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {

                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), eMsg, Toast.LENGTH_LONG).show();
                    }
                });

            } finally {
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }*/
    }
}
