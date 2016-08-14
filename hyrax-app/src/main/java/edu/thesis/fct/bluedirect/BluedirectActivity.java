package edu.thesis.fct.bluedirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.thesis.fct.bluedirect.bt.BTService;
import edu.thesis.fct.bluedirect.bt.BluetoothBroadcastReceiver;
import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.bluedirect.fallback.FBSender;
import edu.thesis.fct.bluedirect.fallback.FileSender;
import edu.thesis.fct.bluedirect.fallback.QuickstartPreferences;
import edu.thesis.fct.bluedirect.fallback.RegistrationIntentService;
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
import edu.thesis.fct.client.LoginActivity;
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
public class BluedirectActivity extends Activity implements ChannelListener, DeviceActionListener {

	public static final String TAG = "wifidirectdemo";
	private WifiP2pManager manager;
	private boolean isWifiP2pEnabled = false;
	private boolean retryChannel = false;
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private final IntentFilter intentFilter = new IntentFilter();
	private final IntentFilter wifiIntentFilter = new IntentFilter();
	private final IntentFilter btIntentFilter = new IntentFilter();
	private Channel channel;
	private BroadcastReceiver receiver = null;
	private BluetoothBroadcastReceiver btReceiver = null;
	public static BTService btService = null;
	private BroadcastReceiver mRegistrationBroadcastReceiver;
	private boolean isReceiverRegistered;


	WifiManager wifiManager;
	private boolean isWifiConnected;

	public boolean isVisible = true;

	BluedirectActivity context;

	public static boolean fallback = false;

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
				if(p.getType().equals(Packet.TYPE.FB_QUERY) || p.getType().equals(Packet.TYPE.FB_DATA)
						|| p.getType().equals(Packet.TYPE.FB_COUNT) ){
					if (p.getSenderMac().equals(Configuration.getFallbackId(context))) return;
				}
				if (p.getType().equals(Packet.TYPE.QUERY) || p.getType().equals(Packet.TYPE.FB_QUERY) ) {
					String username = new String(p.getData());
					if (!BluedirectActivity.fallback){
						checkAndSendImages(username, p.getSenderMac(), p.getBtSMac(), context);
					} else {
						checkAndSendImages(username, p.getSenderMac(), null, context);
					}
				} else if (p.getType().equals(Packet.TYPE.FILE)) {
					new SavePhotoTask(p.getMac()).execute(p.getData());
				} else if (p.getType().equals(Packet.TYPE.FILE_COUNT) || p.getType().equals(Packet.TYPE.FB_COUNT)) {
					int count = 0;
					if (p.getType().equals(Packet.TYPE.FB_COUNT))
						count = Integer.valueOf(new String(p.getData()));
					else
						count = ByteBuffer.wrap(p.getData()).getInt();

					totalToReceive += count;
					updateProgressDialog(totalToReceive);
				} else if (p.getType().equals(Packet.TYPE.FB_DATA)) {
					String [] info = deserializePacket(p);
					ImageModel i = ImageModel.fromString(info[0]);
					File location = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
							+ File.separator + "Hyrax" + File.separator + i.getPhotoName() + File.separator + i.getPhotoName() + ".jpg");
					new DownloadFileFromURL(location,info[1], i).execute();
				}
			}
		});


		BluedirectAPI.setOnGroupLeaveListener(new MeshNetworkManager.onGroupLeaveListener() {
			@Override
			public void onGroupLeave() {
				if (!isVisible)
				startActivity(getIntent(context, BluedirectActivity.class));
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
						Toast.makeText(BluedirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int reasonCode) {
						Toast.makeText(BluedirectActivity.this, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT)
								.show();
					}
				});
			}
		});


		mRegistrationBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				SharedPreferences sharedPreferences =
						PreferenceManager.getDefaultSharedPreferences(context);
				boolean sentToken = sharedPreferences
						.getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
				System.out.println("Token Sent: " + sentToken);
			}
		};
		registerReceiver(context);


		if (checkPlayServices()) {
			// Start IntentService to register this application with GCM.
			Intent intent = new Intent(context, RegistrationIntentService.class);
			startService(intent);
			if (!FBSender.running){
				FBSender sender = new FBSender();
				new Thread(sender).start();
				FBSender.running = true;
			}
		}

		final Button fbMode = (Button) findViewById(R.id.btn_fallback);
		fbMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fallback = true;
				LoginActivity.proceedFromLogin(v.getContext());
			}
		});

	}

	private String [] deserializePacket(Packet p){
		ByteArrayInputStream bai = new ByteArrayInputStream(p.getData());
		DataInputStream dis = new DataInputStream(bai);

		try {
			int infoSize = dis.readInt();
			byte [] tmp = new byte[infoSize];
			dis.readFully(tmp);
			String info = new String(tmp);

			int urlSize = dis.readInt();
			tmp = new byte[urlSize];
			dis.readFully(tmp);
			String url = new String(tmp);

			return new String []{info,url};
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void storeBitmap(Bitmap bmp, File filename){
		FileOutputStream out = null;
		try {
			filename.getParentFile().mkdirs();
			out = new FileOutputStream(filename);
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
			// PNG is a lossless format, the compression factor (100) is ignored
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void registerReceiver(Context context){
		if(!isReceiverRegistered) {
			LocalBroadcastManager.getInstance(context).registerReceiver(mRegistrationBroadcastReceiver,
					new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
			isReceiverRegistered = true;
		}
	}

	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode)) {
				apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}


	private void checkAndSendImages(String username, final String rcvMac, String btRcvMac, final Context context){
		final List<ImageModel> toSend = new ArrayList<>();
		for (ImageModel i : ListingSingleton.getInstance().getImages().values()){
			for (String p : i.getPeople()){
				if (p.equals(username)){
					toSend.add(i);
					break;
				}
			}
		}

		if (!BluedirectActivity.fallback)
			BluedirectAPI.sendToClient(intToBytes(toSend.size()), rcvMac, btRcvMac, Packet.TYPE.FILE_COUNT,context);
		else{
			FBSender.queuePacket(new Packet(Packet.TYPE.FB_COUNT,intToBytes(toSend.size()),rcvMac,Configuration.getFallbackId((Activity)context),null,null),null);
		}


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

	private void sendFile(ImageModel img, final String rcvMac, String btRcvMac, final Context context){
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + "Hyrax" + File.separator + img.getPhotoName() + File.separator + img .getPhotoName() + ".jpg");
		if (!BluedirectActivity.fallback)
			Sender.queuePacket(new Packet(Packet.TYPE.FILE, ImageToBytes(img, file), rcvMac, WiFiDirectBroadcastReceiver.MAC, btRcvMac, Configuration.getBluetoothSelfMac(context)));
		else {
			try {
				FileSender.sendFile(file,Configuration.getServerURL(context) + "upload", context, img);
				FileSender.setOnFileReceivedListener(new FileSender.onFileReceivedListener() {
					@Override
					public void onFileReceived(NetworkResponse response, ImageModel image) {
						final String id = new String(response.data);
						FBSender.queuePacket(new Packet(Packet.TYPE.FB_DATA,(Configuration.getServerURL(context) + "images/" + id).getBytes(), rcvMac, Configuration.getFallbackId((Activity)context),null,null),image);
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
				Toast.makeText(BluedirectActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
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
						Toast.makeText(BluedirectActivity.this, "Aborting connection", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int reasonCode) {
						Toast.makeText(BluedirectActivity.this,
								"Connect abort request failed. Reason Code: " + reasonCode, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}

	}

	class DownloadFileFromURL extends AsyncTask<Void, String, String> {

		File mFile;
		String mURL;
		ImageModel i;

		public DownloadFileFromURL(File mFile, String mURL, ImageModel i){
			this.mFile = mFile;
			this.mURL = mURL;
			this.i = i;
		}

		/**
		 * Downloading file in background thread
		 * */
		@Override
		protected String doInBackground(Void... voids) {
			int count;
			try {
				URL url = new URL(mURL);
				URLConnection conection = url.openConnection();
				conection.connect();
				// this will be useful so that you can show a tipical 0-100%           progress bar
				int lenghtOfFile = conection.getContentLength();

				// download the file
				InputStream input = new BufferedInputStream(url.openStream(), 8192);

				mFile.getParentFile().mkdirs();

				// Output stream
				OutputStream output = new FileOutputStream(mFile);

				byte data[] = new byte[1024];

				long total = 0;

				while ((count = input.read(data)) != -1) {
					total += count;
					// publishing the progress....
					// After this onProgressUpdate will be called

					// writing data to file
					output.write(data, 0, count);
				}

				// flushing output
				output.flush();

				// closing streams
				output.close();
				input.close();

				ImageModel.writeToFile(ListingSingleton.listing,i);
				return mFile.getAbsolutePath();
			} catch (Exception e) {
				Log.e("Error: ", e.getMessage());
			}

			return null;
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
}
