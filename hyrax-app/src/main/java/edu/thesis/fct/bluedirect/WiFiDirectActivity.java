package edu.thesis.fct.bluedirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.thesis.fct.bluedirect.bt.BTService;
import edu.thesis.fct.bluedirect.bt.BluetoothBroadcastReceiver;
import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.bluedirect.router.MeshNetworkManager;
import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.bluedirect.router.Receiver;
import edu.thesis.fct.bluedirect.router.Sender;
import edu.thesis.fct.bluedirect.ui.DeviceDetailFragment;
import edu.thesis.fct.bluedirect.ui.DeviceListFragment;
import edu.thesis.fct.bluedirect.ui.DeviceListFragment.DeviceActionListener;
import edu.thesis.fct.bluedirect.wifi.WiFiDirectBroadcastReceiver;
import edu.thesis.fct.client.GalleryActivity;
import edu.thesis.fct.client.ImageModel;
import edu.thesis.fct.client.ListingSingleton;
import edu.thesis.fct.client.R;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 * 
 * Note: much of this is taken from the Wi-Fi P2P example 
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceActionListener {

	public static final String TAG = "wifidirectdemo";
	private WifiP2pManager manager;
	private boolean isWifiP2pEnabled = false;
	private boolean retryChannel = false;

	private final IntentFilter intentFilter = new IntentFilter();
	private final IntentFilter wifiIntentFilter = new IntentFilter();
	private final IntentFilter btIntentFilter = new IntentFilter();
	private Channel channel;
	private BroadcastReceiver receiver = null;
	private BluetoothBroadcastReceiver btReceiver = null;
	public static BTService btService = null;

	WifiManager wifiManager;
	private boolean isWifiConnected;

	public boolean isVisible = true;

	WiFiDirectActivity context;

	static int totalToReceive = 0;

	/**
	 * @param isWifiP2pEnabled
	 *            the isWifiP2pEnabled to set
	 */
	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
		this.isWifiP2pEnabled = isWifiP2pEnabled;
	}

	/**
	 * On create start running listeners and try Wi-Fi bridging if possible
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		context = this;

		setTitle("Group Selection");

		// add necessary intent values to be matched.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);

		BluedirectAPI.setOnPacketReceivedListener(new Receiver.onPacketReceivedListener() {
			@Override
			public void onPacketReceived(Packet p) {
				if (p.getType().equals(Packet.TYPE.QUERY)) {
					String username = new String(p.getData());

					checkAndSendImages(username, p.getSenderMac(), p.getBtSMac(), context);
				} else if (p.getType().equals(Packet.TYPE.FILE)) {
					new SavePhotoTask(p.getMac()).execute(p.getData());
				} else {
					int count = ByteBuffer.wrap(p.getData()).getInt();
					totalToReceive += count;
					updateProgressDialog(totalToReceive);


				}
			}
		});


		BluedirectAPI.setOnGroupLeaveListener(new MeshNetworkManager.onGroupLeaveListener() {
			@Override
			public void onGroupLeave() {
				if (!isVisible)
				startActivity(getIntent(context, WiFiDirectActivity.class));
			}
		});


		final Button button = (Button) findViewById(R.id.btn_switch);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(
						R.id.frag_list);
				fragment.onInitiateDiscovery();
				manager.discoverPeers(channel, new ActionListener() {

					@Override
					public void onSuccess() {
						Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int reasonCode) {
						Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT)
								.show();
					}
				});
			}
		});

	}


	private void checkAndSendImages(String username, String rcvMac, String btRcvMac, Context context){
		List<ImageModel> toSend = new ArrayList<>();
		for (ImageModel i : ListingSingleton.getInstance().getImages().values()){
			for (String p : i.getPeople()){
				if (p.equals(username)){
					toSend.add(i);
					break;
				}
			}
		}

		BluedirectAPI.sendToClient(intToBytes(toSend.size()), rcvMac, btRcvMac, Packet.TYPE.FILE_COUNT,context);

		for (ImageModel i : toSend){
			sendFile(i, rcvMac,btRcvMac,context);
		}
	}

	private static void updateProgressDialog(final int val){
		if (GalleryActivity.progressDialog.isShowing()){
			GalleryActivity.runOnUI(new Runnable() {
				@Override
				public void run() {
					GalleryActivity.progressDialog.setMessage("Receiving " + val + " photos of you");
				}
			});

		}
	}

	private static void sendFile(ImageModel i, String rcvMac, String btRcvMac, Context context){
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + "Hyrax" + File.separator + i.getPhotoName() + File.separator + i.getPhotoName() + ".jpg");
		Sender.queuePacket(new Packet(Packet.TYPE.FILE, ImageToBytes(i, file), rcvMac, WiFiDirectBroadcastReceiver.MAC, btRcvMac, Configuration.getBluetoothSelfMac(context)));
	}

	private static Intent getIntent(Context context, Class<?> cls) {
		Intent intent = new Intent(context, cls);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		return intent;
	}


	class SavePhotoTask extends AsyncTask<byte[], String, String> {

		String mac;

		public SavePhotoTask(String mac){
			this.mac = mac;
		}

		@Override
		protected String doInBackground(byte[]... params) {

			ByteArrayInputStream bis = new ByteArrayInputStream(params[0]);
			DataInputStream dis = new DataInputStream(bis);

			ImageModel i = null;
			byte [] data = null;
			try {
				int modelSize = dis.readInt();

				byte [] model = new byte[modelSize];
				dis.readFully(model);
				i = ImageModel.fromString(new String(model));

				if (ListingSingleton.getInstance().getImages().containsKey(i.getId())) return null;
				else {
					ListingSingleton.getInstance().getImages().put(i.getId(),i);
					i.writeToFile(ListingSingleton.getInstance().listing);
				}

				byte [] b = new byte[1024];
				int len = 0;
				ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

				while ((len = dis.read(b)) != -1) {
					byteBuffer.write(b, 0, len);
				}

				data = byteBuffer.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			}

			File photo=new File(Environment.getExternalStorageDirectory(), "Hyrax" + File.separator + i.getPhotoName() + File.separator + i.getPhotoName() + ".jpg" );
			photo.getParentFile().mkdirs();

			if (photo.exists()) {
				photo.delete();
			}

			try {
				FileOutputStream fos=new FileOutputStream(photo.getPath());

				fos.write(data);
				fos.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			return photo.getAbsolutePath();
		}

		@Override
		protected void onPostExecute(String s){
			totalToReceive--;
			if (totalToReceive == 0) GalleryActivity.progressDialog.dismiss();
			else updateProgressDialog(totalToReceive);
			if (s != null && GalleryActivity.isVisible()){
				GalleryActivity.updateGallery(new File(s));
			}
		}

	}

	private static byte[] intToBytes( final int i ) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(i);
		return bb.array();
	}

	private static byte[] ImageToBytes(ImageModel i, File file) {
		FileInputStream fileInputStream = null;
		byte [] info = i.toString().getBytes();
		byte[] bFile = new byte[4 + info.length + (int) file.length()];
		System.arraycopy(intToBytes(info.length), 0, bFile, 0,4);
		System.arraycopy(info, 0, bFile, 4, info.length);
		try
		{
			//convert file into array of bytes
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile, info.length + 4, (int)file.length());
			fileInputStream.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return bFile;
	}


	/** register the BroadcastReceiver with the intent values to be matched */
	@Override
	public void onResume() {
		super.onResume();
		receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
		registerReceiver(receiver, intentFilter);
		this.isVisible = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
		this.isVisible = false;
	}

	/**
	 * Remove all peers and clear all fields. This is called on
	 * BroadcastReceiver receiving a state change event.
	 */
	public void resetData() {
		DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
		DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(
				R.id.frag_detail);
		if (fragmentList != null) {
			fragmentList.clearPeers();
		}
		if (fragmentDetails != null) {
			fragmentDetails.resetViews();
		}
	}



	@Override
	public void showDetails(WifiP2pDevice device) {
		DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
		fragment.showDetails(device);
	}
	
	/**
	 * Try to connect through a callback to a given device
	 */
	@Override
	public void connect(WifiP2pConfig config) {
		manager.connect(channel, config, new ActionListener() {

			@Override
			public void onSuccess() {
				// WiFiDirectBroadcastReceiver will notify us. Ignore for now.
			}

			@Override
			public void onFailure(int reason) {
				Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void disconnect() {
		// TODO: again here it should also include the other wifi hotspot thing
		final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(
				R.id.frag_detail);
		fragment.resetViews();
		manager.removeGroup(channel, new ActionListener() {

			@Override
			public void onFailure(int reasonCode) {
				Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

			}

			@Override
			public void onSuccess() {
				fragment.getView().setVisibility(View.GONE);
			}

		});
	}

	@Override
	public void onChannelDisconnected() {
		// we will try once more
		if (manager != null && !retryChannel) {
			Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
			resetData();
			retryChannel = true;
			manager.initialize(this, getMainLooper(), this);
		} else {
			Toast.makeText(this, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void cancelDisconnect() {

		/*
		 * A cancel abort request by user. Disconnect i.e. removeGroup if
		 * already connected. Else, request WifiP2pManager to abort the ongoing
		 * request
		 */
		if (manager != null) {
			final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(
					R.id.frag_list);
			if (fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
				disconnect();
			} else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
					|| fragment.getDevice().status == WifiP2pDevice.INVITED) {

				manager.cancelConnect(channel, new ActionListener() {

					@Override
					public void onSuccess() {
						Toast.makeText(WiFiDirectActivity.this, "Aborting connection", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int reasonCode) {
						Toast.makeText(WiFiDirectActivity.this,
								"Connect abort request failed. Reason Code: " + reasonCode, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}

	}
}
