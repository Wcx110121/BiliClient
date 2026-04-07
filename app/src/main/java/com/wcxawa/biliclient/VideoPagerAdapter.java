package com.wcxawa.biliclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class VideoPagerAdapter extends FragmentPagerAdapter {

    private final int[] partitionIds;
    private final String[] partitionNames;
    private final Map<Integer, WeakReference<Fragment>> fragmentMap = new HashMap<>();

    public VideoPagerAdapter(@NonNull FragmentManager fm,
                             int[] partitionIds, String[] partitionNames) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.partitionIds = partitionIds;
        this.partitionNames = partitionNames;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        int partitionId = partitionIds[position];

        // 个人中心（partitionId == -1）
        if (partitionId == -1) {
            return new ProfileFragment();
        }

        // 正常的分区视频列表
        Fragment fragment = VideoGridFragment.newInstance(partitionId, partitionNames[position]);
        fragmentMap.put(position, new WeakReference<>(fragment));
        return fragment;
    }

    @Override
    public int getCount() {
        return partitionIds.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return partitionNames[position];
    }

    public Fragment getFragmentAtPosition(int position) {
        WeakReference<Fragment> ref = fragmentMap.get(position);
        if (ref != null && ref.get() != null) {
            return ref.get();
        }
        return null;
    }
}