package edu.thesis.fct.client;

import java.net.InetAddress;

/**
 * Created by abs on 10-03-2016.
 */
public class NetworkInfoHolder {
    private InetAddress data;
    private int port;
    public InetAddress getHost() {return data;}
    public int getPort () {return port;}
    public void setData(InetAddress data) {this.data = data;}
    public void setPort(int port) {this.port = port;}

    private static final NetworkInfoHolder holder = new NetworkInfoHolder();
    public static NetworkInfoHolder getInstance() {return holder;}
}
