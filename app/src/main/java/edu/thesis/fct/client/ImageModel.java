package edu.thesis.fct.client;

import java.util.List;

/**
 * Created by abs on 01-05-2016.
 */
public class ImageModel {

    int id;
    String location;
    String time;

    public ImageModel(int id, String location, String time, List<UserDevice> devices){
        this.id = id;
        this.location = location;
        this.time = time;
        this.photoName = location + time;
        this.devices = devices;
    }

    public String getTime() {
        return time;
    }

    public String getLocation() {
        return location;
    }

    public int getId() {
        return id;
    }

    public List<UserDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<UserDevice> devices) {
        this.devices = devices;
    }

    List<UserDevice> devices;

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    String photoName;


}
