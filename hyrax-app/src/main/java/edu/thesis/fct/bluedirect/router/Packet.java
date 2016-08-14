package edu.thesis.fct.bluedirect.router;

/**
 * The echo packet structure
 * @author Peter Henderson
 *
 */
public class Packet {

	/**
	 * Different types of echo packets
	 * @author Peter Henderson
	 *
	 */
	public enum TYPE {
		HELLO, HELLO_ACK, BYE, QUERY, UPDATE, HELLO_BT, FILE, FILE_COUNT, FB_QUERY, FB_COUNT, FB_DATA
	};

	public enum METHOD {
		WD,BT, FB
	};

	private byte[] data;
	private Packet.TYPE type;


	private String receiverMac;
	private String senderMac;
	private String senderIP;


    public String getBtRMac() {
        return btRMac;
    }



    public void setBtRMac(String btRMac) {
        this.btRMac = btRMac;
    }

    private String btRMac;

    public String getBtSMac() {
        return btSMac;
    }

    public void setBtSMac(String btSMac) {
        this.btSMac = btSMac;
    }

    private String btSMac;
	private int ttl;

	/**
	 * constructor default TTL (3)
	 * @param type
	 * @param extraData
	 * @param receiverMac
	 * @param senderMac
	 */
	public Packet(Packet.TYPE type, byte[] extraData, String receiverMac, String senderMac, String btRMac, String btSMac) {
		this.setData(extraData);
		this.setType(type);
		this.receiverMac = receiverMac;
		this.setTtl(3);
		if (receiverMac == null)
			this.receiverMac = "00:00:00:00:00:00";
		this.senderMac = senderMac;
        this.btRMac = btRMac;
		if (btRMac == null){
			this.btRMac = "00:00:00:00:00:00";
		}
        this.btSMac = btSMac;
	}

	/**
	 * constructor custom ttl
	 * @param type2
	 * @param eData
	 * @param receivermac
	 * @param senderMac
	 * @param timetolive
	 */
	public Packet(TYPE type2, byte[] eData, String receivermac, String senderMac, String btRMac, String btSMac, int timetolive) {
		this.setData(eData);
		this.setType(type2);
		this.receiverMac = receivermac;
		if (receiverMac == null)
			this.receiverMac = "00:00:00:00:00:00";
		this.senderMac = senderMac;
		this.ttl = timetolive;
        this.btRMac = btRMac;
        this.btSMac = btSMac;
	}

	/**
	 * get the data (message body)
	 * @return
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * set the data (message body)
	 * @param data
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * the type of packet
	 * @return
	 */
	public Packet.TYPE getType() {
		return type;
	}

	/**
	 * set the type of packets
	 * @param type
	 */
	public void setType(Packet.TYPE type) {
		this.type = type;
	}

	/**
	 * Helper function to get a mac address string as bytes
	 * @param maca
	 * @return
	 */
	public static byte[] getMacAsBytes(String maca) {
		String[] mac = maca.split(":");
		byte[] macAddress = new byte[6]; // mac.length == 6 bytes
		for (int i = 0; i < mac.length; i++) {
			macAddress[i] = Integer.decode("0x" + mac[i]).byteValue();
		}
		return macAddress;
	}

	/**
	 * Helper function to get a byte array of data with an 
	 * offset and use the next six bytes to make a MAC address string
	 * @param data
	 * @param startOffset
	 * @return
	 */
	public static String getMacBytesAsString(byte[] data, int startOffset) {
		StringBuilder sb = new StringBuilder(18);
		for (int i = startOffset; i < startOffset + 6; i++) {
			byte b = data[i];
			if (sb.length() > 0)
				sb.append(':');
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * Serialize a packet according to the predefined structure
	 * @return
	 */
	public byte[] serialize() {

		// 6 bytes for mac
		byte[] serialized = new byte[1 + data.length + 26];
		serialized[0] = (byte) type.ordinal();

		serialized[1] = (byte) ttl;

		byte[] mac = getMacAsBytes(this.receiverMac);

		for (int i = 2; i <= 7; i++) {
			serialized[i] = mac[i - 2];
		}
		mac = getMacAsBytes(this.senderMac);

		for (int i = 8; i <= 13; i++) {
			serialized[i] = mac[i - 8];
		}

        mac = getMacAsBytes(this.btRMac);

        for (int i = 14; i <= 19; i++) {
            serialized[i] = mac[i - 14];
        }

        mac = getMacAsBytes(this.btSMac);

        for (int i = 20; i <= 25; i++) {
            serialized[i] = mac[i - 20];
        }

		for (int i = 27; i < serialized.length; i++) {
			serialized[i] = data[i - 27];
		}
		return serialized;
	}

	/**
	 * Deserialize a packet according to a predefined structure
	 * @param inputData
	 * @return
	 */
	public static Packet deserialize(byte[] inputData) {
		Packet.TYPE type = TYPE.values()[(int) inputData[0]];

		byte[] data = new byte[inputData.length - 27];
		int timetolive = (int) inputData[1];
		String mac = getMacBytesAsString(inputData, 2);
		String receivermac = getMacBytesAsString(inputData, 8);
        String btReceivemac = getMacBytesAsString(inputData, 14);
        String btSendmac = getMacBytesAsString(inputData, 20);

		for (int i = 27; i < inputData.length; i++) {
			data[i - 27] = inputData[i];
		}
		return new Packet(type, data, mac, receivermac,btReceivemac, btSendmac, timetolive);
	}

	/**
	 * Get the receivers mac
	 * @return
	 */
	public String getMac() {
		return receiverMac;
	}

	/**
	 * Set the receivers mac
	 * @param mac
	 */
	public void setMac(String mac) {
		this.receiverMac = mac;
	}

	/**
	 * Get the sender's MAC
	 * @return
	 */
	public String getSenderMac() {
		return this.senderMac;
	}

	/**
	 * get the sender's IP
	 * @return
	 */
	public String getSenderIP() {
		return senderIP;
	}

	/**
	 * Set the sender's IP
	 * @param senderIP
	 */
	public void setSenderIP(String senderIP) {
		this.senderIP = senderIP;
	}

	/**
	 * Stringify a packet
	 */
	@Override
	public String toString() {
		return "Type" + getType().toString() + "receiver:" + getMac() + "sender:" + getSenderMac();
	}

	/**
	 * Get the TTL
	 * @return
	 */
	public int getTtl() {
		return ttl;
	}

	/**
	 * Set the TTL
	 * @param ttl
	 */
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}
}
