package edu.thesis.fct.bluedirect.router;

/**
 * Created by abs on 28-07-2016.
 */
public class IPBundle {
    public IPBundle(Packet.METHOD method, String ip) {
        this.method = method;
        this.address = ip;
    }

    public Packet.METHOD getMethod() {
        return method;
    }

    public void setMethod(Packet.METHOD method) {
        this.method = method;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    Packet.METHOD method;
    String address;

}
