package edu.thesis.fct.bluedirect.router;

import android.widget.Toast;

import java.util.concurrent.ConcurrentLinkedQueue;

import edu.thesis.fct.bluedirect.BluedirectActivity;
import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.bluedirect.router.tcp.TcpReceiver;
import edu.thesis.fct.bluedirect.ui.DeviceDetailFragment;

/**
 * The main receiver class
 */
public class Receiver implements Runnable {

	public interface onPacketReceivedListener {
		public abstract void onPacketReceived(Packet p);
	}

	/**
	 * Flag if the receiver has been running to prevent overzealous thread spawning
	 */
	public static boolean running = false;

	private static onPacketReceivedListener listener;

	public static void setListener(onPacketReceivedListener toAdd){
		listener = toAdd;
	}

	/*
	 * A queue for received packets
	 */
	public static ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue<Packet>();
	
	/**
	 * A ref to the activity
	 */
	static BluedirectActivity activity;

	/**
	 * Constructor with activity
	 * @param a
	 */
	public Receiver(BluedirectActivity a) {
		Receiver.activity = a;
		running = true;
	}

	/** 
	 * Main thread runner
	 */
	public void run() {



		/*
		 * Receiver thread 
		 */

		new Thread(new TcpReceiver(Configuration.RECEIVE_PORT, packetQueue)).start();

		Packet p;

		/*
		 * Keep going through packets
		 */
		while (true) {
			/*
			 * If the queue is empty, sleep to give up CPU cycles
			 */
			while (packetQueue.isEmpty()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			/*
			 * Pop a packet off the queue
			 */
			p = packetQueue.remove();


			/*
			 * If it's a hello, this is special and need to go through the connection mechanism for any node receiving this
			 */
			if (BluedirectActivity.fallback){
				if (p.getType().equals(Packet.TYPE.FB_QUERY)
						|| p.getType().equals(Packet.TYPE.FB_DATA)){
					listener.onPacketReceived(p);
					continue;
				}
			}
			if (p.getType().equals(Packet.TYPE.HELLO)) {
				// Put it in your routing table
				for (AllEncompasingP2PClient c : MeshNetworkManager.routingTable.values()) {
					if (c.getMac().equals(MeshNetworkManager.getSelf().getMac()) || c.getMac().equals(p.getSenderMac()))
						continue;
					byte[] macs = new byte[12];

					byte[] wMac = Packet.getMacAsBytes(p.getSenderMac());
					for (int i = 0; i <= 5; i++) {
						macs[i] = wMac[i];
					}

					wMac = Packet.getMacAsBytes(p.getBtSMac());
					for (int i = 6; i < 12; i++) {
						macs[i] = wMac[i - 6];
					}

					Packet update = new Packet(Packet.TYPE.UPDATE, macs, c.getMac(),
							MeshNetworkManager.getSelf().getMac(), p.getBtSMac(), Configuration.getBluetoothSelfMac(activity));
					Sender.queuePacket(update);
				}

				MeshNetworkManager.newClient(new AllEncompasingP2PClient(p.getBtSMac(), p.getSenderMac(), p.getSenderIP(), p.getSenderMac(),
						MeshNetworkManager.getSelf().getMac(), MeshNetworkManager.getSelf().getGroupID(),null));

				// Send routing table back as HELLO_ACK
				byte[] rtable = MeshNetworkManager.serializeRoutingTable();

				Packet ack = new Packet(Packet.TYPE.HELLO_ACK, rtable, p.getSenderMac(), MeshNetworkManager.getSelf()
						.getMac(), p.getBtSMac(), Configuration.getBluetoothSelfMac(activity));
				Sender.queuePacket(ack);
				somebodyJoined(p.getSenderMac());
				updatePeerList();
			} else {
				// If you're the intended target for a non hello message
				if (p.getMac().equals(MeshNetworkManager.getSelf().getMac())) {
					//if we get a hello ack populate the table
					if (p.getType().equals(Packet.TYPE.HELLO_ACK)) {
						AllEncompasingP2PClient a = MeshNetworkManager.deserializeRoutingTableAndAdd(p.getData());
						MeshNetworkManager.getSelf().setGroupOwnerMac(p.getSenderMac());
						MeshNetworkManager.getSelf().setGroupID(a.getGroupID());
						somebodyJoined(p.getSenderMac());
						updatePeerList();
						if (BluedirectActivity.btService == null)
							BluedirectActivity.btService = Configuration.startBluetoothConnections(activity,this);
					} else if (p.getType().equals(Packet.TYPE.UPDATE)) {
						//if it's an update, add to the table
						String emb_mac = Packet.getMacBytesAsString(p.getData(), 0);
						String emb_bt_mac = Packet.getMacBytesAsString(p.getData(), 6);
						MeshNetworkManager.newClient(new AllEncompasingP2PClient(emb_bt_mac, emb_mac, p.getSenderIP(), p.getMac(), MeshNetworkManager
								.getSelf().getMac(), MeshNetworkManager.getSelf().getGroupID(),null));

						final String message = emb_mac + " joined the conversation";
						final String name = p.getSenderMac();
						activity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (activity.isVisible) {
									Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
								}
							}
						});
						updatePeerList();

					} else if (p.getType().equals(Packet.TYPE.QUERY) || p.getType().equals(Packet.TYPE.FILE) ||
							p.getType().equals(Packet.TYPE.FILE_COUNT)) {

						listener.onPacketReceived(p);

						if (!MeshNetworkManager.routingTable.contains(p.getSenderMac())) {
							/*
							 * Update your routing table if for some reason this
							 * guy isn't in it
							 */

							MeshNetworkManager.newClient(new AllEncompasingP2PClient(p.getBtSMac(), p.getSenderMac(), p.getSenderIP(), p.getSenderMac(),
									MeshNetworkManager.getSelf().getGroupOwnerMac(), MeshNetworkManager.getSelf().getGroupID(),null));

							updatePeerList();
						}
					} else {
						// otherwise forward it if you're not the recipient
						int ttl = p.getTtl();
						// Have a ttl so that they don't bounce around forever
						ttl--;
						if (ttl > 0) {
							p.setTtl(ttl);
							Sender.queuePacket(p);
						}
					}
				}

			}
		}
	}

    public static int byteArrayToInt(byte[] b)
    {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }


	/**
	 * GUI thread to send somebody joined notification
	 * @param smac
	 */
	public static void somebodyJoined(String smac) {

		final String message;
		final String msg;
		message = msg = smac + " has joined.";
		final String name = smac;
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (activity.isVisible) {
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
				} else {
//					MessageActivity.addMessage(name, msg);
				}
			}
		});
	}

	/**
	 * Somebody left notification on the UI thread
	 * @param smac
	 */
	public static void somebodyLeft(String smac) {

		final String message;
		final String msg;
		message = msg = smac + " has left.";
		final String name = smac;
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (activity.isVisible) {
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	/**
	 * Update the list of peers on the front page
	 */
	public static void updatePeerList() {
		if (activity == null)
			return;
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				DeviceDetailFragment.updateGroupChatMembersMessage();
			}

		});
	}

}