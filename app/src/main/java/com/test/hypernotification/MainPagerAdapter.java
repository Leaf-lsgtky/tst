package com.test.hypernotification;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SettingsFragment();
            case 1:
                return new StatusFragment();
            case 2:
                return new TestFragment();  // ★ 新增测试页
            default:
                return new SettingsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // ★ 改为 3 个页面
    }

    public String getTabTitle(int position) {
        switch (position) {
            case 0:
                return "设置";
            case 1:
                return "状态";
            case 2:
                return "测试"; // ★ 新标签
            default:
                return "";
        }
    }
}