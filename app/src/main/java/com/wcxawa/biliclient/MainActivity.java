package com.wcxawa.biliclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.wcxawa.biliclient.api.PartitionApi;
import com.wcxawa.biliclient.model.VideoCard;
import com.wcxawa.biliclient.util.SharedPreferencesUtil;
import com.wcxawa.biliclient.util.NetWorkUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private VideoPagerAdapter pagerAdapter;
    private EditText searchBox;
    private ImageView searchIcon;
    private RecyclerView searchResultsView;
    private ProgressBar progressBar;
    private VideoGridAdapter searchAdapter;
    private List<VideoCard> searchResults = new ArrayList<>();
    private boolean isSearchMode = false;
    private String currentKeyword = "";
    private long backPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(android.R.color.black));
        }

        SharedPreferencesUtil.init(this);
        NetWorkUtil.refreshHeaders();

        // 不再拦截登录，直接显示首页

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // 初始化搜索相关视图
        searchBox = findViewById(R.id.searchBox);
        searchIcon = findViewById(R.id.searchIcon);
        searchResultsView = findViewById(R.id.searchResultsView);
        progressBar = findViewById(R.id.progressBar);

        // 设置搜索结果 RecyclerView
        int spanCount = getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        searchResultsView.setLayoutManager(layoutManager);
        searchAdapter = new VideoGridAdapter(searchResults);
        searchResultsView.setAdapter(searchAdapter);

        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class))
        );

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        setupViewPager();
        setupTabListener();
        setupViewPagerListener();

        // 搜索按钮点击
        searchIcon.setOnClickListener(v -> performSearch());

        // 键盘搜索
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String keyword = searchBox.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }

        currentKeyword = keyword;

        // 切换到搜索模式
        isSearchMode = true;
        viewPager.setVisibility(View.GONE);
        tabLayout.setVisibility(View.GONE);
        searchResultsView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        // 清空旧结果，显示占位符
        searchResults.clear();
        searchAdapter.setPlaceholderMode(true);
        searchAdapter.updateList(searchResults);

        // 执行搜索
        new Thread(() -> {
            List<VideoCard> results = PartitionApi.searchVideos(keyword, 1);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (results != null && !results.isEmpty()) {
                    searchResults.clear();
                    searchResults.addAll(results);
                    searchAdapter.setPlaceholderMode(false);
                    searchAdapter.updateList(searchResults);
                    Toast.makeText(this, "找到 " + results.size() + " 个视频", Toast.LENGTH_SHORT).show();
                } else {
                    searchAdapter.setPlaceholderMode(false);
                    searchAdapter.updateList(new ArrayList<>());
                    Toast.makeText(this, "没有找到相关视频", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void exitSearchMode() {
        if (isSearchMode) {
            isSearchMode = false;
            viewPager.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);
            searchResultsView.setVisibility(View.GONE);
            searchBox.setText("");
        }
    }

    private void setupViewPager() {
        // Tab顺序：个人中心在最左侧，然后是各个分区
        String[] partitions = {"我的", "热门", "动画", "音乐", "游戏", "科技", "生活"};
        int[] partitionIds = {
                -1,  // 个人中心
                PartitionApi.PARTITION_POPULAR,
                PartitionApi.PARTITION_ANIMATION,
                PartitionApi.PARTITION_MUSIC,
                PartitionApi.PARTITION_GAME,
                PartitionApi.PARTITION_TECH,
                PartitionApi.PARTITION_LIFE
        };

        pagerAdapter = new VideoPagerAdapter(
                getSupportFragmentManager(), partitionIds, partitions);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(partitions.length - 1);

        // TabLayout 可滚动模式
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isSearchMode) {
                    exitSearchMode();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupViewPagerListener() {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {}

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int spanCount = newConfig.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        searchResultsView.setLayoutManager(layoutManager);
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (isSearchMode) {
            exitSearchMode();
        } else {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                backPressedTime = System.currentTimeMillis();
                Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            }
        }
    }
}