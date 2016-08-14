package edu.thesis.fct.bluedirect.fallback;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.thesis.fct.bluedirect.BluedirectActivity;
import edu.thesis.fct.bluedirect.config.Configuration;
import edu.thesis.fct.bluedirect.router.IPBundle;
import edu.thesis.fct.bluedirect.router.MeshNetworkManager;
import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.bluedirect.router.tcp.TcpSender;
import edu.thesis.fct.client.ImageModel;

/**
 * Responsible for sending all packets that appear in the queue
 *
 */
public class FBSender implements Runnable {

	static final class PacketEntry<K, V> implements Map.Entry<K, V> {
		private final K key;
		private V value;

		public PacketEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}
	}

	/**
	 * Queue for packets to send
	 */
	private static ConcurrentLinkedQueue<PacketEntry<Packet,ImageModel>> ccl;

	/**
	 * Constructor
	 */
	public FBSender() {
		if (ccl == null)
			ccl = new ConcurrentLinkedQueue<>();
	}


	public static boolean running = false;
	/**
	 * Enqueue a packet to send
	 * @param p
	 * @return
	 */
	public static boolean queuePacket(Packet p, ImageModel model) {
		if (ccl == null)
			ccl = new ConcurrentLinkedQueue<>();

		return ccl.add(new PacketEntry<>(p,model));
	}

	@Override
	public void run() {
		while (true) {
			//Sleep to give up CPU cycles
			while (ccl.isEmpty()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			PacketEntry<Packet,ImageModel> p = ccl.remove();

			GCMSender.sendPacket(p.getKey(),p.getValue());

		}
	}

}
