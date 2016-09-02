package edu.thesis.fct.bluedirect.router;

import java.util.Random;


public class Packet {

	public static final int NEW_ID = -1;

	public enum TYPE {
		HELLO, HELLO_ACK, BYE, QUERY, UPDATE, HELLO_BT, FILE, FILE_COUNT, FB_QUERY, FB_COUNT, FB_DATA
	};

	public enum METHOD {
		WD,BT, FB
	};

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private int id;

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
	public Packet(int id, Packet.TYPE type, byte[] extraData, String receiverMac, String senderMac, String btRMac, String btSMac) {
		this.setData(extraData);
		this.setType(type);
		if (id == -1){
			this.id = new Random().nextInt();
		} else this.id = id;
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
	public Packet(int id,TYPE type2, byte[] eData, String receivermac, String senderMac, String btRMac, String btSMac, int timetolive) {
		this.setData(eData);
		this.setType(type2);
		this.receiverMac = receivermac;
		if (id == -1){
			this.id = new Random().nextInt();
		} else this.id = id;
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
		byte[] serialized = new byte[2 + data.length + 26];
		serialized[0] = (byte) id;

		serialized[1] = (byte) type.ordinal();

		serialized[2] = (byte) ttl;

		byte[] mac = getMacAsBytes(this.receiverMac);

		for (int i = 3; i <= 8; i++) {
			serialized[i] = mac[i - 3];
		}
		mac = getMacAsBytes(this.senderMac);

		for (int i = 9; i <= 14; i++) {
			serialized[i] = mac[i - 9];
		}

        mac = getMacAsBytes(this.btRMac);

        for (int i = 15; i <= 20; i++) {
            serialized[i] = mac[i - 15];
        }

        mac = getMacAsBytes(this.btSMac);

        for (int i = 21; i <= 26; i++) {
            serialized[i] = mac[i - 21];
        }

		for (int i = 28; i < serialized.length; i++) {
			serialized[i] = data[i - 28];
		}
		return serialized;
	}

	/**
	 * Deserialize a packet according to a predefined structure
	 * @param inputData
	 * @return
	 */
	public static Packet deserialize(byte[] inputData) {
		int id = (int) inputData[0];
		Packet.TYPE type = TYPE.values()[(int) inputData[1]];
		byte[] data = new byte[inputData.length - 28];
		int timetolive = (int) inputData[2];
		String mac = getMacBytesAsString(inputData, 3);
		String receivermac = getMacBytesAsString(inputData, 9);
        String btReceivemac = getMacBytesAsString(inputData, 15);
        String btSendmac = getMacBytesAsString(inputData, 21);

		for (int i = 28; i < inputData.length; i++) {
			data[i - 28] = inputData[i];
		}
		return new Packet(id,type, data, mac, receivermac,btReceivemac, btSendmac, timetolive);
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
