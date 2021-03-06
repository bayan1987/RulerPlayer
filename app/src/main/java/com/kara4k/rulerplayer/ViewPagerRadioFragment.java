package com.kara4k.rulerplayer;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;

public class ViewPagerRadioFragment extends ViewPagerFragment {

    public static ViewPagerRadioFragment newInstance() {
        Bundle args = new Bundle();
        ViewPagerRadioFragment fragment = new ViewPagerRadioFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    FragmentStatePagerAdapter getAdapter() {
        return new Adapter(getChildFragmentManager()){
            @Override
             Fragment getFirstFragment() {
                return RadioFragment.newInstance();
            }
        };
    }
}
