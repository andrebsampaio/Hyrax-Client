package edu.thesis.fct.client;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothServerService extends Service {

    final static String TAG = "BT SERVER SERVICE";
    BluetoothAdapter mBluetoothAdapter;
    private static final UUID UUID_KEY = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private boolean hasWD;
    private boolean occupied = false;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "Service onStartCommand");

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("MyPref", this.MODE_PRIVATE);
        this.hasWD = pref.getBoolean("haswd", false);

        turnBluetoothOn(this);

        return Service.START_STICKY;
    }

    private void turnBluetoothOn(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "This device does not support bluetooth!!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(btIntent);
        } else {
            Log.d("BT DEBUG", "im server");
            Thread server = new ListeningThread(this);
            server.start();
        }
    }

    private class ListeningThread extends Thread {
        private final BluetoothServerSocket mServerSocket;
        Context context;

        public ListeningThread(Context context) {
            BluetoothServerSocket temp = null;
            this.context = context;
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
                    if (socket != null) {
                        final String name = socket.getRemoteDevice().getName();

                        ConnectedThread connected = new ConnectedThread(socket, context);
                        connected.start();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        private class ConnectedThread extends Thread {
            BluetoothSocket mSocket;
            DataInputStream input;
            DataOutputStream output;
            Context context;

            public ConnectedThread(BluetoothSocket socket, Context context) {
                this.context = context;
                mSocket = socket;
                DataInputStream tmpIn = null;
                DataOutputStream tmpOut = null;
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
                boolean finished = false;
                Thread wifiTransfer = null;
                try {
                    output.writeBoolean(hasWD);
                    otherWD = input.readBoolean();
                    if (hasWD && otherWD){
                       wifiTransfer = new Thread() {
                            public void run() {
                                WifiDirectTransfer wd = new WifiDirectTransfer(context, false, null, null,null, null, null );
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
                        BufferedInputStream bis;
                        Log.d("BT DEBUG", "TRYING TO RECEIVE NUMBER");
                        int numberImages = input.readInt();
                        Log.d("BT DEBUG", "RECEIVED NUMBER OF IMAGES " + numberImages);
                        List<String> imageNames = new ArrayList<>();
                        for (int i = 0; i < numberImages; i++) {
                            String name = input.readUTF();
                            imageNames.add(name);
                            System.out.println(name);
                        }

                        File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "Hyrax");
                        List<File> toSend = new ArrayList<>();

                        for (String name : imageNames) {
                            for (File f : file.listFiles()) {
                                if (f.getName().equals(name)) {
                                    toSend.add(new File(f.getAbsolutePath() + file.separator + f.getName() + ".jpg"));
                                }
                            }
                        }

                        output.writeInt(toSend.size());

                        for (File f : toSend) {
                            bis = new BufferedInputStream(new FileInputStream(f));

                            output.writeUTF(f.getName());
                            output.writeLong(f.length());
                            copyStream(bis, output, f.length());
                            bis.close();
                            final String sentMsg = "File sent to " + mSocket.getRemoteDevice().getName();
                            Log.d(TAG, sentMsg);
                        }

                        finished = input.readBoolean();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally{
                    if (finished){
                        if (mSocket != null){
                            try {
                                mSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

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
}
