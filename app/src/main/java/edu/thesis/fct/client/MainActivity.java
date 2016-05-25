package edu.thesis.fct.client;


import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    Fragment[] arrFragment;
    ViewPager myViewPager;
    PageAdapter myFragmentPager;
    Activity activity;
    String username;
    String uploadURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.main_layout);

        Intent intent = new Intent(this, BluetoothServerService.class);
        startService(intent);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        WifiManager wifiMan = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String macAddressWD = wifiInf.getMacAddress();
        String macaddressBT = BluetoothAdapter.getDefaultAdapter().getAddress();

        editor.putString("macwd", macAddressWD);
        editor.putString("macbt", macaddressBT);
        editor.commit();


        username = pref.getString("username",null);

        NetworkInfoHolder nih = NetworkInfoHolder.getInstance();
        Log.d("NIH", nih.getHost() + "");
        if (nih.getHost() != null){
            uploadURL = "http://" + nih.getHost().getHostAddress()  + ":" + nih.getPort()  + "/hyrax-server/rest/upload/";
        }

        activity = this;
        Fragment cam = new CameraFragment();
        Bundle b = new Bundle();
        b.putString("username", username);
        b.putString("url", uploadURL);
        cam.setArguments(b);
        arrFragment = new Fragment[1];
        arrFragment[0] = cam;
        myFragmentPager = new PageAdapter(getFragmentManager(),arrFragment);
        myViewPager = (ViewPager) findViewById(R.id.pager);
        myViewPager.setOffscreenPageLimit(1);

        myViewPager.setAdapter(myFragmentPager);

        myViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    getSupportActionBar().show();
                } else
                    getSupportActionBar().hide();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

}
