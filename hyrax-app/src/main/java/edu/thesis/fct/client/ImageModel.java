package edu.thesis.fct.client;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by abs on 01-05-2016.
 */
public class ImageModel implements Serializable {

    static final long serialVersionUID = 42L;

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
        try(FileWriter fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(this.toString());
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    public static void writeToFile(File f, ImageModel i){
        f.getParentFile().mkdirs();
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try(FileWriter fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(i.toString());
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }


}
