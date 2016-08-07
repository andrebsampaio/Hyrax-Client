package edu.thesis.fct.bluedirect.router;

/**
 * This is an alternative representation the Android P2P library's WiFiP2PDevice class
 * it contains information about any client connected to the mesh and is stored in
 * the routing table
 *
 *
 */
public class AllEncompasingP2PClient {


	/**
	 * The client's bt mac address
	 */
	private String btmac;

	public Long getLastUpdate() {
		return lastUpdate;
	}


	public void setLastUpdate(Long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	private Long lastUpdate;

	/**
	 * The client's mac address
	 */
	private String mac;
	
	/**
	 * The client's name (i.e. Joe)
	 */
	private String name;
	
	/**
	 * The client's GO mac address, for routing
	 */
	private String groupOwnerMac;

	private String GroupID;

	/**
	 * The client's IP address
	 */
	private String ip;

	public Bridge getBridge() {
		return bridge;
	}

	public void setBridge(Bridge bridge) {
		this.bridge = bridge;
	}

	private Bridge bridge;

	/**
	 * Constructor
	 */
	public AllEncompasingP2PClient(String btmac, String mac_address, String ip, String name, String groupOwner,String groupID, String timestamp) {
		this.setMac(mac_address);
		this.setName(name);
		this.setIp(ip);
		this.setGroupOwnerMac(groupOwner);
		this.setBtmac(btmac);
		this.setGroupID(groupID);
		this.lastUpdate = initTimeStamp(timestamp);
	}

	public AllEncompasingP2PClient(String btmac, String mac_address, String ip, String name, String groupOwner, String groupID, Bridge bridge, String timestamp) {
		this.setMac(mac_address);
		this.setName(name);
		this.setIp(ip);
		this.setGroupOwnerMac(groupOwner);
		this.setBtmac(btmac);
		this.bridge = bridge;
		this.setGroupID(groupID);

		this.lastUpdate = initTimeStamp(timestamp);
	}

	public String getGroupID() {
		return GroupID;
	}

	public void setGroupID(String groupID) {
		GroupID = groupID;
	}

	public String getBtmac() {
		return btmac;
	}

	public void setBtmac(String btmac) {
		this.btmac = btmac;
	}

	/**
	 * Get GO MAC
	 * @return
	 */
	public String getGroupOwnerMac() {
		return groupOwnerMac;
	}

	/**
	 * Set GO MAC
	 * @param groupOwnerMac
	 */
	public void setGroupOwnerMac(String groupOwnerMac) {
		this.groupOwnerMac = groupOwnerMac;
	}

	/**
	 * Get the client's name
	 * Note: here we don't currently use this so much, and just refer to client's as MAC addresses
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set a client's name (like a nickname or something for future use)
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the client's mac address
	 * @return
	 */
	public String getMac() {
		return mac;
	}

	/**
	 * Set the client's mac address
	 * @param mac
	 */
	public void setMac(String mac) {
		this.mac = mac;
	}

	/**
	 * Get the client's IP as a string
	 * (i.e. 124.12.124.15)
	 * @return
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * Set the client's IP as a string
	 * @param ip
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * Serialize this client's information into a comma delimited form
	 */
	@Override
	public String toString() {
		String toString;
		if (this.getBridge() == null){
				toString = getIp() + "," + getBtmac() + "," + getMac() + "," + getName() + "," + getGroupOwnerMac() + "," + getGroupID()
						+ "," + "null" + "," + "null" + "," + lastUpdate.toString();
		} else {
			toString = getIp() + "," + getBtmac() + "," + getMac() + "," + getName() + "," + getGroupOwnerMac() + "," + getGroupID()
					+ "," + getBridge().getBTMac() + "," + getBridge().getGID() + "," + lastUpdate.toString();
		}
		return toString;
	}

	/**
	 * Generate a client object from a serialized string
	 * @param serialized
	 * @return
	 */
	public static AllEncompasingP2PClient fromString(String serialized) {
		String[] divided = serialized.split(",");
		if (divided[6].equals("null") || divided[7].equals("null")){
			divided[6] = null;
			divided[7] = null;
		}
		return new AllEncompasingP2PClient(divided[1], divided[2], divided[0], divided[3], divided[4],divided[5], new Bridge(divided[7],divided[6]),divided[8]);
	}

	private static Long initTimeStamp(String timestamp) {
		if (timestamp == null || timestamp.equals("null")){
			return getUnixCurrentTime();
		}
		return Long.valueOf(timestamp);
	}

	public static Long getUnixCurrentTime(){
		return System.currentTimeMillis();
	}





}
