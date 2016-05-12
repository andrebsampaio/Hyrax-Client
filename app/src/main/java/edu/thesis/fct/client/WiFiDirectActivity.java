package edu.thesis.fct.client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abs on 03-05-2016.
 */
public class WiFiDirectActivity extends Activity {

    static String TAG = "WIFI DIRECT DEVICES";

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WifiP2pManager.PeerListListener mPeerListListener;
    WifiP2pManager.ConnectionInfoListener mConnectionListener;
    private static final int SELECT_PICTURE = 1;
    WifiP2pInfo info;
    public boolean connected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mPeerListListener = new WifiP2pManager.PeerListListener(){
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers){
                List<WifiP2pDevice> peersList = new ArrayList<>(peers.getDeviceList());
                int size = peers.getDeviceList().size();
                for (WifiP2pDevice device : peersList){
                    //Log.d(TAG, "Name: " + device.deviceName + " " + device.deviceAddress );
                }

                if (peersList.size() > 0){
                    final WifiP2pDevice device = peersList.get(0);
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;
                /*TODO - Descobrir maneira de enviar critérios de selecao do host e do client seja por bluetooth or something*/
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            //Connecting...
                        }

                        @Override
                        public void onFailure(int reason) {
                            switch(reason){
                                case WifiP2pManager.P2P_UNSUPPORTED:
                                    Log.d(TAG, "P2P isn't supported on this device.");
                                    break;
                                case WifiP2pManager.BUSY:
                                    Log.d(TAG, "Too busy for request");
                                    break;
                                case WifiP2pManager.ERROR:
                                    Log.d(TAG, "An error ocurred");
                                    break;
                                default:break;

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
                    if (info.groupFormed && info.isGroupOwner){
                        Log.d("TAG", "is GO");
                        new FileServerAsyncTask(WiFiDirectActivity.this, mChannel,mManager).execute();
                    } else if (info.groupFormed) {
                        WiFiDirectActivity.this.setConInfo(info);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        Log.d("TAG", "is Client");
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent,
                                "Select Picture"), SELECT_PICTURE);
                    }
                }
                connected = true;
            }
        };



        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this, mPeerListListener, mConnectionListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //The device that wants the image becomes the server
        if (Build.MODEL.equals("m2")){
            mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG,"Creating p2p group");
                }

                @Override
                public void onFailure(int i) {
                    Log.i(TAG,"Creating group failed, error code:"+i);

                }
            });
        } else {
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
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

    public void setConInfo(WifiP2pInfo info){
        this.info = info;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            connected = true;
            Uri uri = data.getData();
            Log.d(TAG, "IM HEREEEEEE");
            new FileClientAsyncTask(this).execute(info.groupOwnerAddress, 9991, uri);

        }
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public static class FileClientAsyncTask extends AsyncTask<Object, String, Void> {

        private Context context;

        public FileClientAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Object... params) {
            int len;
            Socket socket = null;
            byte buf[]  = new byte[1024];
            InetAddress address = (InetAddress)params[0];
            int port = (int) params[1];
            Uri uri = (Uri) params[2];


            try {
                /**
                 * Create a client socket with the host,
                 * port, and timeout information.
                 */
                Log.d(TAG, "is reachable" + address.isReachable(500));
                socket = new Socket(address,port);
                socket.setSoTimeout(500);

                /**
                 * Create a byte stream from a JPEG file and pipe it to the output stream
                 * of the socket. This data will be retrieved by the server device.
                 */
                OutputStream outputStream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream inputStream = null;
                //POTATO
                inputStream = cr.openInputStream(uri);
                while ((len = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                inputStream.close();
            } catch (FileNotFoundException e) {
                //catch logic
            } catch (IOException e) {
                e.printStackTrace();
            }

/**
 * Clean up any open sockets when done
 * transferring or if an exception occurred.
 */
            finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            //catch logic
                        }
                    }
                }
            }
            return null;
        }
    }

    public static class FileServerAsyncTask extends AsyncTask<Object, String, String> {

        private Context context;
        private WifiP2pManager mManager;
        private WifiP2pManager.Channel mChannel;

        public FileServerAsyncTask(Context context, WifiP2pManager.Channel mChannel, WifiP2pManager mManager) {
            this.context = context;
            this.mChannel = mChannel;
            this.mManager = mManager;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {

                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */

                Log.d(TAG, "Starting server");
                ServerSocket serverSocket = new ServerSocket(9991);
                Log.d(TAG, "Im waiting here at " + serverSocket.getLocalSocketAddress() + serverSocket.getLocalPort());
                Socket client = serverSocket.accept();
                Log.d(TAG, "ACCEPTED CONNECTION");

                /**
                 * If this code is reached, a client has connected and transferred data
                 * Save the input stream from the client as a JPEG file
                 */
                final File f = new File(Environment.getExternalStorageDirectory() + "/wifip2pshared/" + System.currentTimeMillis()
                        + ".jpg");

                Log.d(TAG, "Saved at " + f.getAbsolutePath());

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                InputStream in = client.getInputStream();
                FileOutputStream out = new FileOutputStream(f);
                byte[] buffer = new byte[1024];
                int len = in.read(buffer);
                while (len != -1) {
                    out.write(buffer, 0, len);
                    len = in.read(buffer);
                }
                serverSocket.close();
                mManager.removeGroup(mChannel,null);
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /**
         * Start activity that can handle the JPEG image
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }
        }
    }
}


