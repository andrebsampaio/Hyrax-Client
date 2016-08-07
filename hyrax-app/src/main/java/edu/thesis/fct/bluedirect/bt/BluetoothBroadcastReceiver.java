package edu.thesis.fct.bluedirect.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import edu.thesis.fct.bluedirect.WiFiDirectActivity;
import edu.thesis.fct.bluedirect.router.MeshNetworkManager;

/**
 *  A BroadcastReceiver that notifies of important bluetooth connection events.
 */
public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    private static final UUID UUID_KEY = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private BluetoothAdapter mBluetoothAdapter;
    private Queue<BluetoothDevice> foundDevices = new LinkedList<>();
    private List<String> seenDevices = new LinkedList<>();
    private int discoverySleepRate = 1;
    public BluetoothBroadcastReceiver(){
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            if (seenDevices.contains(device.getAddress()))return;
            for (BluetoothDevice d : foundDevices){
                if (d.getAddress().equals(device)) return;
            }
            foundDevices.add(device);
            System.out.println("FOUND " + device.getName() + " with " + device.getAddress());
        }
        else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            //Device is now connected
        }
        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            System.out.println("BT discovery finished");
            new Thread()
            {
                public void run() {
                    while (WiFiDirectActivity.btService.bridge == null){
                        if (foundDevices.isEmpty()){
                            try {
                                this.sleep(2000 * discoverySleepRate);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            discoverySleepRate++;
                            mBluetoothAdapter.startDiscovery();
                            return;
                        }
                        while (MeshNetworkManager.routingTable.size() == 1){
                            try {
                                this.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        while (!foundDevices.isEmpty()){
                            BluetoothDevice deviceTmp = foundDevices.remove();
                            /*if (MeshNetworkManager.getSelf() != null && !BluetoothServer.establishingBridge && !btSender.establishingBridge){
                                btSender.sendPacket(deviceTmp.getAddress(),MeshNetworkManager.getSelf().getGroupID(),false);
                            }*/

                            if (WiFiDirectActivity.btService.getState() != BTService.STATE_CONNECTED && !seenDevices.contains(deviceTmp.getAddress())){
                                WiFiDirectActivity.btService.connect(deviceTmp,true);
                                seenDevices.add(deviceTmp.getAddress());
                            }
                        }
                    }
                }
            }.start();



        }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            //Device is about to disconnect
        }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            //Device has disconnected
        }
    }
}
