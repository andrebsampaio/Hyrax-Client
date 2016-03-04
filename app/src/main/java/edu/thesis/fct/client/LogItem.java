package edu.thesis.fct.client;


/**
 * Created by abs on 02-03-2016.
 */
public class LogItem {
    int itemId;
    int photosFound;
    int peersHit;
    String location;

    public int getItemId() {
        return itemId;
    }

    public int getPhotosFound() {
        return photosFound;
    }

    public int getPeersHit() {
        return peersHit;
    }

    public String getLocation() {
        return location;
    }

    public LogItem(int itemId, int photosFound, int peersHit, String location){
        this.itemId = itemId;
        this.photosFound = photosFound;
        this.peersHit = peersHit;
        this.location = location;
    }


}
