package edu.thesis.fct.bluedirect.router.tcp;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import edu.thesis.fct.bluedirect.router.MeshNetworkManager;
import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.bluedirect.router.Receiver;

/**
 * Runner for dequeueing packets from packets to send, and issues the TCP connection to send them
 *
 *
 */
public class TcpSender {

	Socket tcpSocket = null;

	public boolean sendPacket(String ip, int port, Packet data) {
		// Try to connect, otherwise remove from table
		try {
			System.out.println("IP: " + ip);
			InetAddress serverAddr = InetAddress.getByName(ip);
			tcpSocket = new Socket();
			tcpSocket.bind(null);
			tcpSocket.connect(new InetSocketAddress(serverAddr, port), 5000);

		} catch (Exception e) {
			/*
			 * If can't connect assume that they left the chat and remove them
			 */
			MeshNetworkManager.clientGone(data.getMac());
			Receiver.somebodyLeft(data.getMac());
			Receiver.updatePeerList();
			e.printStackTrace();
			return false;
		}

		OutputStream os = null;

		//try to send otherwise remove from table
		try {
			os = tcpSocket.getOutputStream();
			os.write(data.serialize());
			os.close();
			tcpSocket.close();

		} catch (Exception e) {
			MeshNetworkManager.clientGone(data.getMac());
			Receiver.somebodyLeft(data.getMac());
			Receiver.updatePeerList();
			e.printStackTrace();
		}

		return true;
	}

}
