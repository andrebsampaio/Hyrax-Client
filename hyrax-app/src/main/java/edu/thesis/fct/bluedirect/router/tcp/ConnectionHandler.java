package edu.thesis.fct.bluedirect.router.tcp;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.thesis.fct.bluedirect.router.Packet;
import edu.thesis.fct.client.GalleryActivity;
import edu.thesis.fct.client.ImageModel;
import edu.thesis.fct.client.InstrumentationUtils;

/**
 * Created by abs on 29-08-2016.
 */
public class ConnectionHandler implements Runnable {
    Socket socket;
    ConcurrentLinkedQueue<Packet> queue;
    public ConnectionHandler(Socket socket, ConcurrentLinkedQueue<Packet> queue){
        this.socket = socket;
        this.queue = queue;
    }


    @Override
    public void run() {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));


            boolean isFile = in.readBoolean();
            if (isFile){
                GalleryActivity.utils.calculateLatency(InstrumentationUtils.RECOG);
                GalleryActivity.utils.registerLatency(InstrumentationUtils.P2P_TRANSFER);
                int filesNumber = in.readInt();
                for (int i = 0; i < filesNumber; i++){
                    int modelSize = in.readInt();
                    byte [] tmp = new byte[modelSize];
                    in.readFully(tmp);
                    ImageModel model = ImageModel.fromString(new String(tmp));

                    long filesize = in.readLong();

                    File photo=new File(Environment.getExternalStorageDirectory(), "Hyrax" + File.separator + model.getPhotoName() + File.separator + model.getPhotoName() + ".jpg" );
                    photo.getParentFile().mkdirs();

                    FileOutputStream fos = new FileOutputStream(photo);
                    OutputStream bos = new BufferedOutputStream(fos);

                    copyStream(in,bos,filesize);

                    bos.close();

                }
                GalleryActivity.utils.calculateLatency(InstrumentationUtils.P2P_TRANSFER);
                GalleryActivity.utils.endTest();
                System.out.println("ACCABBBOUUUU");
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buf = new byte[1024];
                while (true) {
                    int n = in.read(buf);
                    if (n < 0)
                        break;
                    baos.write(buf, 0, n);
                }

                byte trimmedBytes[] = baos.toByteArray();
                Packet p = Packet.deserialize(trimmedBytes);
                p.setSenderIP(socket.getInetAddress().getHostAddress());
                this.queue.add(p);
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyStream(InputStream input, OutputStream output, Long fileSize)
            throws IOException {
        int bytesRead = 0;
        try {
            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];

            // we need to know how may bytes were read to write them to the byteBuffer
            Long len = fileSize;

            while (len > 0) {
                int bytes = input.read(buffer, 0, (int)Math.min(buffer.length,len));
                bytesRead += bytes;
                len -= bytes;
                output.write(buffer, 0, bytes);
            }
            //Log.d("BT DEBUG", bytesRead + "");
        } finally {
            //Log.d("BT DEBUG", bytesRead + "");
            //output.flush();
            //output.close();
        }
    }

}
