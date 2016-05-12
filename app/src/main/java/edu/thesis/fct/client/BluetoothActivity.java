package edu.thesis.fct.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by abs on 11-05-2016.
 */
public class BluetoothActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID UUID_KEY = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mDevicesArrayAdapter = new ArrayList<>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mDevicesArrayAdapter.add(device);
                if (!device.getName().contains("GT")){
                    Log.d("BT DEBUG", "im client");
                    Thread connect = new ConnectThread(device);
                    connect.start();
                }

                Log.e("Bluetooth Device", device.getName() + " " + device.getAddress());
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        turnBluetoothOn();
    }

    private void turnBluetoothOn() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "This device does not support bluetooth!!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (!Build.MODEL.contains("GT")){
                Log.d("BT DEBUG", "im server");
                Thread server = new ListeningThread();
                server.start();
            }
            searchForBluetoothDevices();
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
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
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
            output.close();
            input.close();
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device)
        {
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
                Log.d("BT DEBUG", "connected to:" + bluetoothDevice.getName());
            } catch (IOException connectException) {
                connectException.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }
            ConnectedThread connected = new ConnectedThread(bluetoothSocket);
            connected.start();
            /*try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
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

        public ConnectedThread(BluetoothSocket socket)
        {
            mSocket = socket;
        }
        public void run()
        {
            File filePath = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM" + File.separator + "Camera");
            File file = new File(filePath.listFiles()[0].getAbsolutePath());

            BufferedInputStream bis;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                DataOutputStream os = new DataOutputStream(mSocket.getOutputStream());
                os.writeUTF(file.getName());
                os.writeLong(file.length());
                copyStream(bis, os, file.length());
                final String sentMsg = "File sent to " +mSocket.getRemoteDevice().getName();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), sentMsg,Toast.LENGTH_LONG).show();
                    }
                });
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }


        }




    }

    private class ListeningThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public ListeningThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(getString(R.string.app_name), UUID_KEY);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mServerSocket = temp;
        }

        public void run() {
            BluetoothSocket socket;

            while (true) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    final String name = socket.getRemoteDevice().getName();

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            Toast.makeText(getApplicationContext(), "A connection has been accepted from " + name, Toast.LENGTH_LONG).show();
                        }
                    });
                    ConnectedThread connected = new ConnectedThread(socket);
                    connected.start();
                    /*try {
                        mServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                    break;
                }
            }
        }


        private class ConnectedThread extends Thread {
            BluetoothSocket mSocket;
            DataInputStream input;

            public ConnectedThread(BluetoothSocket socket) {
                mSocket = socket;
                DataInputStream tmpIn = null;
                try {
                    tmpIn = new DataInputStream(mSocket.getInputStream());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                input = tmpIn;
            }

            public void run() {
                try {
                    String name = input.readUTF();
                    Long fileSize = input.readLong();

                    File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + name + ".jpg");

                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    copyStream(input, bos,fileSize);
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
            }
        }
    }
}
