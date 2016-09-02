package edu.thesis.fct.bluedirect.router.tcp;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.thesis.fct.bluedirect.router.Packet;

/**
 * Receives packets on a server socket threads and enqueues them to a receiver runner
 *
 */
public class TcpReceiver implements Runnable {

	private ServerSocket serverSocket;
	private ConcurrentLinkedQueue<Packet> packetQueue;
	static final int TCP_BUFFER_SIZE=65535;

	/**
	 * Constructor with the queue
	 * @param port
	 * @param queue
	 */
	public TcpReceiver(int port, ConcurrentLinkedQueue<Packet> queue) {
		try {
			this.serverSocket = new ServerSocket(port);
			serverSocket.setReceiveBufferSize(TCP_BUFFER_SIZE);
		} catch (IOException e) {
			System.err.println("Server socket on port " + port + " could not be created. ");
			e.printStackTrace();
		}
		this.packetQueue = queue;
	}

	/**
	 * Thread runner
	 */
	@Override
	public void run() {
		Socket socket;
		while (!Thread.currentThread().isInterrupted()) {
			try {
				socket = this.serverSocket.accept();
				Runnable handler = new ConnectionHandler(socket,packetQueue);
				new Thread(handler).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}



}