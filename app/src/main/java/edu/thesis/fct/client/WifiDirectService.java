package edu.thesis.fct.client;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abs on 21-05-2016.
 */
public class WifiDirectService {

    boolean connected = false;
    static final String TAG = "WD Service";
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WifiP2pManager.PeerListListener mPeerListListener;
    WifiP2pManager.ConnectionInfoListener mConnectionListener;
    Context context;
    String macAddress;
    boolean isRequesting;
    List<ImageModel> images;



    private boolean checkAddress(String address1, String address2){
        return address1.substring(2).equals(address2.substring(2));
    }

    public WifiDirectService(final Context context, final boolean isRequesting, final String macAddress, final List<ImageModel> images) {
        this.context = context;
        this.isRequesting = isRequesting;
        this.macAddress = macAddress;
        this.images = images;
        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, context.getMainLooper(), null);
        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                List<WifiP2pDevice> peersList = new ArrayList<>(peers.getDeviceList());
                WifiP2pDevice d = null;
                for (WifiP2pDevice device : peersList) {
                    String address = macAddress.toLowerCase();
                    if (checkAddress(device.deviceAddress,address)){
                        d = device;
                        break;
                    }
                }

                if (d != null) {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = d.deviceAddress;
                /*TODO - Descobrir maneira de enviar critérios de selecao do host e do client seja por bluetooth or something*/
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            //Connecting...
                        }

                        @Override
                        public void onFailure(int reason) {
                            switch (reason) {
                                case WifiP2pManager.P2P_UNSUPPORTED:
                                    Log.d(TAG, "P2P isn't supported on this device.");
                                    break;
                                case WifiP2pManager.BUSY:
                                    Log.d(TAG, "Too busy for request");
                                    break;
                                case WifiP2pManager.ERROR:
                                    Log.d(TAG, "An error ocurred");
                                    break;
                                default:
                                    break;

                            }
                        }
                    });
                }

            }
        };

        mConnectionListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (!connected) {
                    if (info.groupFormed && info.isGroupOwner && isRequesting){
                        mManager.removeGroup(mChannel,null);
                        return;
                    }
                    if (info.groupFormed && info.isGroupOwner) {
                        Log.d("TAG", "is GO");
                        new FileServerAsyncTask(mChannel, mManager).execute();
                    } else if (info.groupFormed) {
                        Log.d("TAG", "is Client");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        new FileClientAsyncTask(info.groupOwnerAddress, 9991, images).execute();
                    }
                }
                connected = true;
            }
        };


        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, mPeerListListener, mConnectionListener);

        mIntentFilter = new IntentFilter();
        if (isRequesting){
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        }
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        context.registerReceiver(mReceiver, mIntentFilter);

        //The device that wants the image becomes the server
        if (!isRequesting) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null){
                        mManager.removeGroup(mChannel, null);
                    } else {
                        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.i(TAG, "Creating p2p group");
                            }

                            @Override
                            public void onFailure(int i) {
                                Log.i(TAG, "Creating group failed, error code:" + i);

                            }
                        });
                    }
                }
            });

        } else {
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(context, "Discovery Initiated",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    Toast.makeText(context, "Discovery Failed : " + reasonCode,
                            Toast.LENGTH_SHORT).show();
                    switch (reasonCode) {
                        case WifiP2pManager.P2P_UNSUPPORTED:
                            Log.d(TAG, "P2P isn't supported on this device.");
                            break;
                        case WifiP2pManager.BUSY:
                            Log.d(TAG, "Too busy for request");
                            break;
                        case WifiP2pManager.ERROR:
                            Log.d(TAG, "An error ocurred");
                            break;
                        default:
                            break;

                    }
                }
            });
        }

    }


    public static class FileClientAsyncTask extends AsyncTask<Object, String, Void> {

        InetAddress address;
        int port;
        Socket socket;
        List<ImageModel> imageNames;

        public FileClientAsyncTask(InetAddress address, int port, List<ImageModel> imagesNames) {
            this.address = address;
            this.port = port;
            this.imageNames = imagesNames;
        }

        @Override
        protected Void doInBackground(Object... params) {

            try {

                socket = new Socket(address, port);
                socket.setSoTimeout(700000);

                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());

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
                }

                final String sentMsg = "File received";
                Log.d(TAG, sentMsg);
            } catch (IOException e) {

                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                Log.e(TAG, eMsg);

            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    public static class FileServerAsyncTask extends AsyncTask<Object, String, String> {

        private WifiP2pManager mManager;
        private WifiP2pManager.Channel mChannel;
        Socket client;

        public FileServerAsyncTask(WifiP2pManager.Channel mChannel, WifiP2pManager mManager) {
            this.mChannel = mChannel;
            this.mManager = mManager;
        }

        @Override
        protected String doInBackground(Object... params) {

            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            BufferedInputStream bis;
            try {

                Log.d(TAG, "Starting server");
                ServerSocket serverSocket = new ServerSocket(9991);
                Log.d(TAG, "Im waiting here at " + serverSocket.getLocalSocketAddress() + serverSocket.getLocalPort());
                client = serverSocket.accept();
                Log.d(TAG, "ACCEPTED CONNECTION");

                DataInputStream input = new DataInputStream(client.getInputStream());
                DataOutputStream output = new DataOutputStream(client.getOutputStream());

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
                    final String sentMsg = "File sent to " + client.getRemoteSocketAddress();
                    Log.d(TAG, sentMsg);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } /*finally {
                try {
                    client.close();
                    mManager.removeGroup(mChannel,null);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }*/

           return "POTATO";
        }

        @Override
        protected void onPostExecute (String result){
            if (result != null) {
                Log.d(TAG, "GOTTASHIT!");
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
                int bytes = input.read(buffer, 0, buffer.length);
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


