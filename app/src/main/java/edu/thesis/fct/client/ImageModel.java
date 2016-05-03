package edu.thesis.fct.client;

/**
 * Created by abs on 01-05-2016.
 */
public class ImageModel {

    int id;
    String location;
    String time;

    public ImageModel(int id, String location, String time){
        this.id = id;
        this.location = location;
        this.time = time;
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


}
