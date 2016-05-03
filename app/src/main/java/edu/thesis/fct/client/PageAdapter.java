package edu.thesis.fct.client;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

public class PageAdapter extends FragmentPagerAdapter {
    private final int PAGE_COUNT = 1;

    Fragment[] arrFragment;

    public PageAdapter(FragmentManager fm, Fragment [] arrFragment) {
        super(fm);
        this.arrFragment = arrFragment;
    }

    @Override
    public Fragment getItem(int i) {
        return arrFragment[i];
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
