package edu.thesis.fct.client.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import edu.thesis.fct.client.NetworkInfoHolder;

/**
 * Created by abs on 02-10-2016.
 */
public class Configurations {

    public enum ACTION {
        REGISTER,UNTAG, LOGIN;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum KEYS {
        USERNAME, IMAGE_PATH, REGISTRATION, SERVER_URL;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    static final String HTTP = "http://";
    static final String PREFIX = "/hyrax-server/rest/";
    static final String PORT_SEPARATOR = ":";
    public static final String STORAGE = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String HYRAX_PATH = STORAGE + File.separator + "Hyrax";
    public static final String ZIP = ".zip";
    public static final String JPG = ".jpg";
    public static final String GALLERY = "Gallery";

    public static final String NSD_TAG = "NSD_DISCOVER";
    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String SERVICE_NAME = "hyrax";

    public final static String SAVE_PATH_TMP = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxRecog" + File.separator + "my_template.yml";
    public final static String RECOG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxRecog" + File.separator;
    public final static String GZ = ".gz";
    public final static String MY_TEMPLATE = "my_template";
    public final static String SAVE_PATH =  RECOG_PATH + MY_TEMPLATE ;
    public final static String TRAIN_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "HyraxTrain";
    public final static String YML = ".yml";

    public static String buildURL () {
        NetworkInfoHolder nih = NetworkInfoHolder.getInstance();
        if ( nih == null) return null;
        Log.d("NIH", nih.getHost() + "");
        String address = NetworkInfoHolder.getInstance().getHost().getHostAddress();
        int port = NetworkInfoHolder.getInstance().getPort();
        return HTTP + address + PORT_SEPARATOR + port + PREFIX;
    }

    public static String getActionURL(ACTION action){
        return buildURL() + action.toString();
    }

    public static String getUsername(Context context){
        return context.getSharedPreferences("MyPref", context.MODE_PRIVATE)
                .getString(KEYS.USERNAME.toString(),null);
    }

    public static void setUsername(Context context, String user){
        SharedPreferences pref = context.getSharedPreferences("MyPref", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEYS.USERNAME.toString(), user);
        editor.commit();
    }

    public static void storeURL(Context context, String url){
        SharedPreferences pref = context.getSharedPreferences("MyPref", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEYS.SERVER_URL.toString(), url);
        editor.commit();
    }



}
