package edu.thesis.fct.bluedirect.router.tcp;

import android.media.Image;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import edu.thesis.fct.bluedirect.router.MeshNetworkManager;
import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.bluedirect.router.Receiver;
import edu.thesis.fct.client.ImageModel;

/**
 * Runner for dequeueing packets from packets to send, and issues the TCP connection to send them
 *
 *
 */
public class TcpSender {

	Socket tcpSocket = null;
	final static int TCP_BUFFER_SIZE=65535;

	public boolean sendFiles (String ip, int port, List<ImageModel> imageList, Packet data){
		try {
			tcpSocket = prepareConnection(ip,port);
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

		DataOutputStream dos;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream()));

			dos.writeBoolean(true);
			dos.writeInt(imageList.size());
			for (ImageModel i : imageList){
				byte [] inBytes = i.toString().getBytes();

				// Send imagemodel
				dos.writeInt(inBytes.length);
				dos.write(inBytes);

				//Send File
				File f = new File(ImageModel.PREFIX + i.getPhotoName() + File.separator + i.getPhotoName() + ImageModel.SUFFIX);
				dos.writeLong(f.length());
				long start = System.currentTimeMillis();
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
				IOUtils.copy(bis,dos);
				bis.close();
			}
			dos.close();
			tcpSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean sendPacket(String ip, int port, Packet data) {
		// Try to connect, otherwise remove from table
		try {
			tcpSocket = prepareConnection(ip,port);

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
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeBoolean(false);
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

	private Socket prepareConnection(String ip, int port){
		try {
			System.out.println("IP: " + ip);
			InetAddress serverAddr = InetAddress.getByName(ip);
			Socket tcp = new Socket();
			tcp.setSendBufferSize(TCP_BUFFER_SIZE);
			tcp.bind(null);
			tcp.connect(new InetSocketAddress(serverAddr, port), 5000);
			return tcp;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void copyStream(InputStream input, OutputStream output, Long fileSize)
			throws IOException {
		int bytesRead = 0;
		try {
			int bufferSize = 8192;
			byte[] buffer = new byte[bufferSize];

			// we need to know how may bytes were read to write them to the byteBuffer
			Long len = fileSize;

			while (len > 0) {
				int bytes = input.read(buffer, 0, (int)Math.min(buffer.length,len));
				bytesRead += bytes;
				len -= bytes;
				output.write(buffer, 0, bytes);
			}
			Log.d("BT DEBUG", bytesRead + "");
		} finally {
			Log.d("BT DEBUG", bytesRead + "");
			//output.close();
		}
	}


}
