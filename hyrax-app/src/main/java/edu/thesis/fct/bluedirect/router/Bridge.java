package edu.thesis.fct.bluedirect.router;

/**
 * Created by abs on 14-07-2016.
 */
public class Bridge {
    public String getGID() {
        return GID;
    }

    public void setGID(String GID) {
        this.GID = GID;
    }

    public String getBTMac() {
        return BTMac;
    }

    public void setBTMac(String BTMac) {
        this.BTMac = BTMac;
    }

    String GID;
    String BTMac;
    public Bridge(String GID, String BTMac){
        this.setGID(GID);
        this.setBTMac(BTMac);
    }
}
