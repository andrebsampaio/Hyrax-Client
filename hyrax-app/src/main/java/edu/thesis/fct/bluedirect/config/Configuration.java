package edu.thesis.fct.bluedirect.config;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import edu.thesis.fct.bluedirect.bt.BTService;
import edu.thesis.fct.bluedirect.bt.BluetoothBroadcastReceiver;
import edu.thesis.fct.bluedirect.router.Receiver;

/**
 * Contains configuration settings related to the WiFi Direct implementation
 *
 */
public class Configuration {
	/**
	 * The default ports that all clients receive at
	 */
	public static final int RECEIVE_PORT = 8888;
	
	/**
	 * The default GO IP address for initial connections
	 */
	public static final String GO_IP = "192.168.49.1";

	/**
	 * This only works on certain devices where multiple simultaneous
	 * connections are available (infrastructure & ad-hoc) (multiroll)
	 */
	public static final boolean isDeviceBridgingEnabled = false;

    public static String getBluetoothSelfMac(Context context){
        BluetoothManager ba=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        return ba.getAdapter().getAddress();
    }

	public static String getFallbackId(Activity context){
		SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
		return sharedPref.getString("instance_token",null);
	}

	private static void ensureDiscoverable(Context context) {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
			context.startActivity(discoverableIntent);
		}
	}

	public static String getServerURL(Context context){
		return context.getSharedPreferences("MyPref", context.MODE_PRIVATE).getString("server_url",null);
	}

	public static boolean setBluetooth(boolean enable) {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		boolean isEnabled = bluetoothAdapter.isEnabled();
		if (enable && !isEnabled) {
			return bluetoothAdapter.enable();
		}
		else if(!enable && isEnabled) {
			return bluetoothAdapter.disable();
		}
		// No need to change bluetooth state
		return true;
	}

	private static void setBluetoothBroadcast(Context context){
		final IntentFilter btIntentFilter = new IntentFilter();

		btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		BluetoothBroadcastReceiver btReceiver = new BluetoothBroadcastReceiver();
		context.registerReceiver(btReceiver, btIntentFilter);
		BluetoothAdapter.getDefaultAdapter().startDiscovery();
	}

	public static BTService startBluetoothConnections(Context activity, Receiver r){
		Configuration.setBluetooth(true);
		Configuration.ensureDiscoverable(activity);

		/*if (!BluetoothServer.running){
			BluetoothServer btServer = new BluetoothServer(activity,r.packetQueue);
			new Thread(btServer).start();
		}*/

		BTService btService = new BTService(activity, r.packetQueue );
		btService.start();

		Configuration.setBluetoothBroadcast(activity);

		return btService;
	}

}
