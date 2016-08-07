package edu.thesis.fct.client;

import android.os.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by abs on 07-08-2016.
 */
public class ListingSingleton {

    public static File listing = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "Hyrax" + File.separator + "listing");

    public Map<Integer, ImageModel> getImages() {
        return images;
    }

    public void setImages(Map<Integer, ImageModel> images) {
        this.images = images;
    }

    private Map<Integer, ImageModel> images = new HashMap<>();
    private static final ListingSingleton holder = new ListingSingleton();
    public static ListingSingleton getInstance() {return holder;}


}
