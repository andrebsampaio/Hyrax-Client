package edu.thesis.fct.bluedirect.router;

import java.util.concurrent.ConcurrentHashMap;

import edu.thesis.fct.bluedirect.config.Configuration;

/**
 * A manager for keeping track of a mesh and handling routing
 *
 *
 */
public class MeshNetworkManager {

	public static void setJoinListener(onGroupJoinedListener l) {
		joinListener = l;
	}

	public static void setLeaveListener(onGroupLeaveListener l) {
		leaveListener = l;
	}

	public interface onGroupJoinedListener {
		public abstract void onGroupJoined();
	}

	public interface onGroupLeaveListener{
		public abstract void onGroupLeave();
	}

	private static onGroupJoinedListener joinListener;
	private static onGroupLeaveListener leaveListener;
	private static boolean connected = false;

	/**
	 * Your routing table
	 */
	public static ConcurrentHashMap<String, AllEncompasingP2PClient> routingTable = new ConcurrentHashMap<String, AllEncompasingP2PClient>();

	/**
	 * Need to know yourself
	 */
	private static AllEncompasingP2PClient self;

	/**
	 * Introduce a new client into the routing table
	 * @param c
	 */
	public static void newClient(AllEncompasingP2PClient c) {
		routingTable.put(c.getMac(), c);
		if (routingTable.size() == 2 && !connected){
			connected = true;
			joinListener.onGroupJoined();
		}
	}

	/**
	 * A client has left the routing table
	 * @param mac
	 */
	public static void clientGone(String mac) {
		routingTable.remove(mac);
		if (routingTable.size() == 1 && connected) {
			connected = false;
			leaveListener.onGroupLeave();
		}

	}

	/**
	 * Get yourself
	 * @return
	 */
	public static AllEncompasingP2PClient getSelf() {
		return self;
	}

	/**
	 * Set yourself
	 * @param self
	 */
	public static void setSelf(AllEncompasingP2PClient self) {
		MeshNetworkManager.self = self;
		newClient(self);
	}


	/**
	 * Either returns the IP in the current net if on the same one, or sends to
	 * the relevant Group Owner or sends to all group owners if group owner not
	 * in mesh
	 * 
	 * @param c
	 */
	public static IPBundle getIPForClient(AllEncompasingP2PClient c) {

		/*
		 * This is part of the same Group so its okay to use its IP
		 */
		if (self.getGroupID().equals(c.getGroupID())) {
			// share the same Group then just give its IP
			if (self.getGroupOwnerMac().equals(c.getGroupOwnerMac())) {
				System.out.println("Have the same group owner, sending to :" + c.getIp());
				return new IPBundle(Packet.METHOD.WD, c.getIp());
			}

		} else {
			//I am the bridge to the other group
			if (self.getBridge() != null && self.getBridge().getGID().equals(c.getGroupID())) {
				return new IPBundle(Packet.METHOD.BT, c.getBtmac());
			}

			String ip = Configuration.GO_IP;

			// Find a client that has a bridge with the other group
			for (AllEncompasingP2PClient client : MeshNetworkManager.routingTable.values()) {
				if (client.getGroupID().equals(self.getGroupID())) {
					if (client.getBridge().getBTMac().equals(c.getBtmac())) {
						return new IPBundle(Packet.METHOD.WD, client.getIp());
					} else if (client.getBridge().getGID().equals(c.getGroupID())) {
						ip = client.getIp();
					}
				}
			}

			// Send to a random group to expand
			if (self.getGroupOwnerMac() == self.getMac() && ip.equals(Configuration.GO_IP)) {
				IPBundle tmp = null;
				for (AllEncompasingP2PClient client : MeshNetworkManager.routingTable.values()) {
					if (client.getBridge().getGID() != self.getGroupID()) {
						tmp = new IPBundle(Packet.METHOD.WD, client.getIp());
						break;
					}
				}
				if (tmp == null)
					return new IPBundle(Packet.METHOD.WD, "0.0.0.0"); // No other groups - drop packet

			}
			return new IPBundle(Packet.METHOD.WD, ip);
		}

		return new IPBundle(Packet.METHOD.WD, "0.0.0.0");
	}

	/**
	 * Serialize the routing table, one serialized AllEncompasingP2PClient per line
	 * @return
	 */
	public static byte[] serializeRoutingTable() {
		StringBuilder serialized = new StringBuilder();

		for (AllEncompasingP2PClient v : routingTable.values()) {
			serialized.append(v.toString());
			serialized.append("\n");
		}

		return serialized.toString().getBytes();
	}

	/**
	 * De serialize a routing table and populate the existing one with the data
	 * @param rtable
	 */
	public static AllEncompasingP2PClient deserializeRoutingTableAndAdd(byte[] rtable) {
		String rstring = new String(rtable);

		String[] div = rstring.split("\n");
		AllEncompasingP2PClient a = null;
		for (String s : div) {
			a = AllEncompasingP2PClient.fromString(s);
			AllEncompasingP2PClient b = routingTable.get(a.getMac());
			if (b != null){
				if (a.getLastUpdate() > b.getLastUpdate()){
					newClient(a);
				}
			} else {
				newClient(a);
			}

		}
		return a;
	}

	/**
	 * Either returns the IP in the current net if on the same one, or sends to
	 * the relevant Group Owner or sends to all group owners if group owner not
	 * in mesh
	 * 
	 * @param mac
	 */
	public static IPBundle getIPForClient(String mac) {

		AllEncompasingP2PClient c = routingTable.get(mac);
		if (c == null) {
			System.out.println("NULL ENTRY in ROUTING TABLE FOR MAC");
			return new IPBundle(Packet.METHOD.WD, Configuration.GO_IP);
		}

		return getIPForClient(c);

	}

}
