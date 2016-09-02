package edu.thesis.fct.bluedirect.router;

import android.media.Image;
import android.os.AsyncTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.thesis.fct.bluedirect.BluedirectActivity;
import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.bluedirect.fallback.GCMSender;
import edu.thesis.fct.bluedirect.router.tcp.TcpSender;
import edu.thesis.fct.client.ImageModel;

/**
 * Responsible for sending all packets that appear in the queue
 *
 */
public class Sender implements Runnable {

	/**
	 * Queue for packets to send
	 */
	private static ConcurrentLinkedQueue<Packet> ccl;

	/**
	 * Constructor
	 */
	public Sender() {
		if (ccl == null)
			ccl = new ConcurrentLinkedQueue<Packet>();
	}

	/**
	 * Enqueue a packet to send
	 * @param p
	 * @return
	 */
	public static boolean queuePacket(Packet p) {
		if (ccl == null)
			ccl = new ConcurrentLinkedQueue<Packet>();
		return ccl.add(p);
	}

	@Override
	public void run() {
		final TcpSender packetSender = new TcpSender();

		while (true) {
			//Sleep to give up CPU cycles
			while (ccl.isEmpty()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			final Packet p = ccl.remove();
			IPBundle bundle = null;
			if (!p.getType().equals(Packet.TYPE.FB_QUERY) && !p.getType().equals(Packet.TYPE.FB_DATA)){
				bundle = MeshNetworkManager.getIPForClient(p.getMac());
			}

			if (bundle.getMethod().equals(Packet.METHOD.WD)){
				if (p.getType().equals(Packet.TYPE.FILE)){
					final IPBundle finalBundle = bundle;
					new AsyncTask<Void, Void, Void>(){
						@Override
						protected Void doInBackground(Void... params) {
							packetSender.sendFiles(finalBundle.getAddress(),Configuration.RECEIVE_PORT,convertByteArrayToList(p.getData()),p);
							return null;
						}
					}.execute();
				} else {
					packetSender.sendPacket(bundle.getAddress(), Configuration.RECEIVE_PORT, p);
				}

			} else if (bundle.getMethod().equals(Packet.METHOD.BT)) {
				BluedirectActivity.btService.write(p.serialize());
			}

		}
	}

	private List<ImageModel> convertByteArrayToList(byte [] array){
		ByteArrayInputStream bis = new ByteArrayInputStream(array);
		List<ImageModel> result = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(bis);
			result = (List<ImageModel>)ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}

}
