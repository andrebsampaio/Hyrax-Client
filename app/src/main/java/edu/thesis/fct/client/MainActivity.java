package edu.thesis.fct.client;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    Fragment[] arrFragment;
    ViewPager myViewPager;
    PageAdapter myFragmentPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.main_layout);
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

    }





}
