package edu.thesis.fct.client;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    Fragment[] arrFragment;
    ViewPager myViewPager;
    PageAdapter myFragmentPager;
    static final String TAG = "NSD_DISCOVER";
    static final String SERVICE_TYPE = "_http._tcp.";
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.ResolveListener mResolveListener;
    NsdManager mNsdManager;
    int port;
    InetAddress host;
    String mServiceName = "hyrax";
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.main_layout);

        activity = this;
        Fragment cam = new CameraFragment();
        Fragment log = new SearchLogFragment();
        arrFragment = new Fragment[2];
        arrFragment[0] = cam;
        arrFragment[1] = log;
        myFragmentPager = new PageAdapter(getFragmentManager(),arrFragment);
        myViewPager = (ViewPager) findViewById(R.id.pager);
        myViewPager.setOffscreenPageLimit(2);

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

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    mNsdManager.resolveService(service, mResolveListener);
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("hyrax")){
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                port = serviceInfo.getPort();
                host = serviceInfo.getHost();
                NetworkInfoHolder.getInstance().setData(host);
                NetworkInfoHolder.getInstance().setPort(port);
                Log.d(TAG,  NetworkInfoHolder.getInstance().getHost().getHostAddress());
                CharSequence detected =  "Server detected";
                Toast toast = Toast.makeText(activity,detected,Toast.LENGTH_SHORT);
                toast.show();

                mNsdManager.stopServiceDiscovery(mDiscoveryListener);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
            }
        };
    }


}
