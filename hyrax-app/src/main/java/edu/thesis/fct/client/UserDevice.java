package edu.thesis.fct.client;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (macWD == null ? 0 : macWD.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other){
        UserDevice tmp;
        if (other == null)
            return false;
        if (!(other instanceof UserDevice))
            return false;
        else tmp = (UserDevice)other;
        if (this.getMacBT().equals(tmp.getMacBT()) && this.getMacWD().equals(tmp.getMacWD()))
            return true;
        else return false;

    }
}
