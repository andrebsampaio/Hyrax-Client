package edu.thesis.fct.client;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.thesis.fct.client.config.Configurations;

/**
 * Created by abs on 07-08-2016.
 */
public class ListingSingleton {

    public static final String LISTING = "listing";

    public static File listing = new File(Configurations.HYRAX_PATH + File.separator + LISTING);

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
