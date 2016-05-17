package edu.thesis.fct.client;

/**
 * Created by abs on 16-05-2016.
 */
public class UserDevice {

    public String getMacBT() {
        return macBT;
    }

    public void setMacBT(String macBT) {
        this.macBT = macBT;
    }

    String macBT;

    public String getMacWD() {
        return macWD;
    }

    public void setMacWD(String macWD) {
        this.macWD = macWD;
    }

    String macWD;

    public UserDevice(String macBT, String macWD){
        this.macBT = macBT;
        this.macWD = macWD;
    }
}
