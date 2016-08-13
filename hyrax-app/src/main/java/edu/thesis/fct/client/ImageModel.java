package edu.thesis.fct.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by abs on 01-05-2016.
 */
public class ImageModel {

    int id;
    String location;
    String time;
    String [] people;
    static final String SEPARATOR = ",";

    public ImageModel(int id, String location, String time, String [] people){
        this.id = id;
        this.location = location;
        this.time = time;
        this.photoName = location + time;
        this.people = people;
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

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String[] getPeople() {
        return people;
    }

    public void setPeople(String[] people) {
        this.people = people;
    }

    String photoName;

    @Override
    public String toString() {
        String part =  id + SEPARATOR + location + SEPARATOR + time + SEPARATOR + people.length;
        for (String s : people){
            part += SEPARATOR + s;
        }
        return part;
    }

    public static ImageModel fromString(String i){
        String [] model = i.split(SEPARATOR);
        String [] people = new String [Integer.valueOf(model[3])];
        for (int x = 0; x < Integer.valueOf(model[3]); x++){
            people[x] = model[4 + x];
        }
        return new ImageModel(Integer.valueOf(model[0]), model[1], model[2],people);
    }

    public void writeToFile(File f){
        f.getParentFile().mkdirs();
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(f, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        writer.println(this.toString());
        writer.close();
    }

    public static void writeToFile(File f, ImageModel i){
        f.getParentFile().mkdirs();
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(f, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        writer.println(i.toString());
        writer.close();
    }


}
