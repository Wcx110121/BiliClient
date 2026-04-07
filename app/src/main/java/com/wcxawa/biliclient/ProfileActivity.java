package com.wcxawa.biliretro;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.wcxawa.biliretro.api.ProfileApi;
import com.wcxawa.biliretro.model.VideoCard;
import com.wcxawa.biliretro.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private RecyclerView videoRecyclerView;
    private VideoGridAdapter videoAdapter;
    private List<VideoCard> videoList = new ArrayList<>();

    private String targetMid;
    private String targetName;
    private boolean isLoading = false;
    private int retryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        targetMid = getIntent().getStringExtra("mid");
        targetName = getIntent().getStringExtra("name");

        if (targetName != null && !targetName.isEmpty()) {
            getSupportActionBar().setTitle(targetName + "的主页");
        } else {
            getSupportActionBar().setTitle("个人主页");
        }

        // 刷新按钮（文字版）
        TextView refreshIcon = findViewById(R.id.refreshIcon);
        refreshIcon.setOnClickListener(v -> {
            retryCount = 0;
            loadVideoList();
        });

        videoRecyclerView = findViewById(R.id.videoRecyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        videoRecyclerView.setLayoutManager(layoutManager);
        videoAdapter = new VideoGridAdapter(videoList);
        videoRecyclerView.setAdapter(videoAdapter);

        String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
        if (cookie == null || cookie.isEmpty() || !cookie.contains("SESSDATA=")) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadVideoList();
    }

    private void loadVideoList() {
        if (isLoading) {
            Toast.makeText(this, "正在加载中...", Toast.LENGTH_SHORT).show();
            return;
        }

        isLoading = true;
        Toast.makeText(this, "加载中...", Toast.LENGTH_SHORT).show();

        boolean isSelf = (targetMid == null || targetMid.isEmpty());

        new Thread(() -> {
            List<VideoCard> videos;
            try {
                if (isSelf) {
                    videos = ProfileApi.getUserVideos();
                } else {
                    videos = ProfileApi.getUserVideosByMid(targetMid);
                }
            } catch (Exception e) {
                videos = null;
                e.printStackTrace();
            }

            final List<VideoCard> finalVideos = videos;
            runOnUiThread(() -> {
                isLoading = false;

                if (finalVideos != null && !finalVideos.isEmpty()) {
                    videoList.clear();
                    videoList.addAll(finalVideos);
                    videoAdapter.updateList(videoList);
                    retryCount = 0;
                    Toast.makeText(this, "加载成功", Toast.LENGTH_SHORT).show();
                } else if (finalVideos != null && finalVideos.isEmpty()) {
                    Toast.makeText(this, "暂无投稿视频", Toast.LENGTH_SHORT).show();
                } else {
                    if (retryCount < 2) {
                        retryCount++;
                        Toast.makeText(this, "加载失败，正在自动重试(" + retryCount + "/2)...", Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> loadVideoList(), 1500);
                    } else {
                        Toast.makeText(this, "加载失败，请点击刷新按钮重试", Toast.LENGTH_LONG).show();
                        retryCount = 0;
                    }
                }
            });
        }).start();
    }
}