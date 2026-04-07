package com.wcxawa.biliclient;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import com.wcxawa.biliclient.api.PartitionApi;
import com.wcxawa.biliclient.model.VideoCard;
import com.wcxawa.biliclient.util.SharedPreferencesUtil;
import com.google.android.material.snackbar.Snackbar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class VideoDetailActivity extends AppCompatActivity {

    private String bvid;
    private String aid;
    private ExoPlayer player;
    private PlayerView playerView;
    private ImageView coverImage;
    private ImageView loadingView;
    private Handler handler = new Handler(Looper.getMainLooper());

    private LinearLayout videoContainer;
    private FrameLayout rightContentContainer;
    private LinearLayout commentView;
    private LinearLayout recommendView;
    private LinearLayout upInfoSection;
    private FrameLayout videoFrameLayout;
    private ImageView commentBtn;
    private ImageView recommendBtn;

    private FrameLayout verticalContentContainer;
    private LinearLayout verticalCommentView;
    private LinearLayout verticalRecommendView;
    private RecyclerView verticalCommentRecyclerView;
    private RecyclerView verticalRecommendRecyclerView;
    private TextView verticalCommentPlaceholder;

    private RecyclerView commentRecyclerView;
    private RecyclerView recommendRecyclerView;
    private TextView commentPlaceholder;

    private VideoGridAdapter recommendAdapter;
    private VideoGridAdapter verticalRecommendAdapter;
    private CommentAdapter commentAdapter;
    private CommentAdapter verticalCommentAdapter;
    private List<VideoCard> recommendList = new ArrayList<>();
    private List<PartitionApi.Comment> commentList = new ArrayList<>();
    private List<PartitionApi.Comment> verticalCommentList = new ArrayList<>();

    private int currentRightView = 0;
    private int currentVerticalView = 0;
    private static final float FIXED_ASPECT_RATIO = 9f / 16f;

    private int currentCommentPage = 1;
    private boolean isLoadingMore = false;
    private int currentVerticalCommentPage = 1;
    private boolean isLoadingMoreVertical = false;

    private int coinCount = 0;

    // 纯方角 Snackbar 工具方法
    private void showHoloSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor("#FF333333"));
        bg.setCornerRadius(0);

        snackbarView.setBackground(bg);
        snackbar.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        String videoTitle = getIntent().getStringExtra("title");
        String upName = getIntent().getStringExtra("upName");
        String view = getIntent().getStringExtra("view");
        String coverUrl = getIntent().getStringExtra("cover");
        bvid = getIntent().getStringExtra("bvid");
        aid = getIntent().getStringExtra("aid");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (bvid != null && !bvid.isEmpty() && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(bvid);
        }

        ImageView shareIcon = findViewById(R.id.shareIcon);
        shareIcon.setOnClickListener(v -> shareVideoLink());

        commentBtn = findViewById(R.id.commentBtn);
        recommendBtn = findViewById(R.id.recommendBtn);

        upInfoSection = findViewById(R.id.upInfoSection);

        commentBtn.setOnClickListener(v -> {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                currentRightView = 0;
                updateRightContentView();
                updateUpInfoSectionVisibility();
            } else {
                currentVerticalView = 0;
                updateVerticalContentView();
                updateUpInfoSectionVisibility();
            }
        });

        recommendBtn.setOnClickListener(v -> {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                currentRightView = 1;
                updateRightContentView();
                updateUpInfoSectionVisibility();
            } else {
                currentVerticalView = 1;
                updateVerticalContentView();
                updateUpInfoSectionVisibility();
            }
        });

        TextView titleText = findViewById(R.id.titleText);
        TextView upNameText = findViewById(R.id.upNameText);
        TextView viewText = findViewById(R.id.viewText);
        titleText.setText(videoTitle);
        upNameText.setText(upName);
        viewText.setText(view);

        coverImage = findViewById(R.id.coverImage);
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this).load(coverUrl).into(coverImage);
        }

        playerView = findViewById(R.id.playerView);
        loadingView = findViewById(R.id.loadingView);

        videoContainer = findViewById(R.id.videoContainer);
        rightContentContainer = findViewById(R.id.rightContentContainer);
        commentView = findViewById(R.id.commentView);
        recommendView = findViewById(R.id.recommendView);
        videoFrameLayout = findViewById(R.id.videoFrameLayout);

        verticalContentContainer = findViewById(R.id.verticalContentContainer);
        verticalCommentView = findViewById(R.id.verticalCommentView);
        verticalRecommendView = findViewById(R.id.verticalRecommendView);
        verticalCommentRecyclerView = findViewById(R.id.verticalCommentRecyclerView);
        verticalCommentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        verticalRecommendRecyclerView = findViewById(R.id.verticalRecommendRecyclerView);
        verticalRecommendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        verticalCommentPlaceholder = findViewById(R.id.verticalCommentPlaceholder);

        recommendRecyclerView = findViewById(R.id.recommendRecyclerView);
        recommendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recommendAdapter = new VideoGridAdapter(recommendList);
        recommendRecyclerView.setAdapter(recommendAdapter);

        verticalRecommendAdapter = new VideoGridAdapter(recommendList);
        verticalRecommendRecyclerView.setAdapter(verticalRecommendAdapter);

        commentRecyclerView = findViewById(R.id.commentRecyclerView);
        commentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentPlaceholder = findViewById(R.id.commentPlaceholder);

        LinearLayout favoriteLayout = findViewById(R.id.favoriteLayout);
        favoriteLayout.setOnClickListener(v -> showFavoriteDialog());

        LinearLayout coinLayout = findViewById(R.id.coinLayout);
        coinLayout.setOnClickListener(v -> performCoinSimple());

        coverImage.setOnClickListener(v -> startLoadingAnimation());
        coverImage.setClickable(true);

        loadRecommendVideos();
        loadComments();
        loadVerticalComments();

        adjustLayoutForOrientation();
        coverImage.post(() -> setFixedVideoContainerHeight());

        new Thread(() -> {
            long aidNum = PartitionApi.getAidByBvid(bvid);
            runOnUiThread(() -> {
                aid = String.valueOf(aidNum);
                checkCoinStatus();
            });
        }).start();
    }

    private void updateUpInfoSectionVisibility() {
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (upInfoSection != null) {
                upInfoSection.setVisibility(View.VISIBLE);
            }
        } else {
            boolean showUpInfo = (currentVerticalView == 1);
            if (upInfoSection != null) {
                upInfoSection.setVisibility(showUpInfo ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void updateRightContentView() {
        if (currentRightView == 0) {
            commentView.setVisibility(View.VISIBLE);
            recommendView.setVisibility(View.GONE);
        } else {
            commentView.setVisibility(View.GONE);
            recommendView.setVisibility(View.VISIBLE);
        }
    }

    private void updateVerticalContentView() {
        if (currentVerticalView == 0) {
            verticalCommentView.setVisibility(View.VISIBLE);
            verticalRecommendView.setVisibility(View.GONE);
        } else {
            verticalCommentView.setVisibility(View.GONE);
            verticalRecommendView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustLayoutForOrientation();
        coverImage.post(() -> setFixedVideoContainerHeight());
        updateCoinIcon();
        updateUpInfoSectionVisibility();
    }

    private void adjustLayoutForOrientation() {
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (videoContainer != null) {
                videoContainer.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f));
            }
            if (rightContentContainer != null) {
                rightContentContainer.setVisibility(View.VISIBLE);
                updateRightContentView();
            }
            if (verticalContentContainer != null) {
                verticalContentContainer.setVisibility(View.GONE);
            }
            if (upInfoSection != null) {
                upInfoSection.setVisibility(View.VISIBLE);
            }
            if (commentBtn != null) {
                commentBtn.setVisibility(View.VISIBLE);
            }
            if (recommendBtn != null) {
                recommendBtn.setVisibility(View.VISIBLE);
            }
        } else {
            if (videoContainer != null) {
                videoContainer.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            if (rightContentContainer != null) {
                rightContentContainer.setVisibility(View.GONE);
            }
            if (verticalContentContainer != null) {
                verticalContentContainer.setVisibility(View.VISIBLE);
                updateVerticalContentView();
            }
            if (commentBtn != null) {
                commentBtn.setVisibility(View.VISIBLE);
            }
            if (recommendBtn != null) {
                recommendBtn.setVisibility(View.VISIBLE);
            }
            updateUpInfoSectionVisibility();
        }
    }

    private void setFixedVideoContainerHeight() {
        int containerWidth = videoFrameLayout.getWidth();
        if (containerWidth > 0) {
            int newHeight = (int) (containerWidth * FIXED_ASPECT_RATIO);
            ViewGroup.LayoutParams params = videoFrameLayout.getLayoutParams();
            if (params.height != newHeight) {
                params.height = newHeight;
                videoFrameLayout.setLayoutParams(params);
            }
        }
    }

    private void startLoadingAnimation() {
        coverImage.setVisibility(View.GONE);

        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .into(loadingView);
        loadingView.setVisibility(View.VISIBLE);

        new Thread(() -> {
            String videoUrl = fetchVideoUrl();
            runOnUiThread(() -> {
                if (videoUrl != null) {
                    prepareExoPlayerInBackground(videoUrl);
                } else {
                    showHoloSnackbar("获取视频地址失败");
                    coverImage.setVisibility(View.VISIBLE);
                    loadingView.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void loadRecommendVideos() {
        new Thread(() -> {
            try {
                String url = "https://api.bilibili.com/x/web-interface/archive/related?bvid=" + bvid;
                OkHttpClient client = new OkHttpClient.Builder().build();
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .addHeader("Cookie", getCookie())
                        .build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.body() != null) {
                        JSONObject result = new JSONObject(response.body().string());
                        if (result.optInt("code") == 0) {
                            JSONArray data = result.getJSONArray("data");
                            List<VideoCard> videos = new ArrayList<>();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                VideoCard video = new VideoCard();
                                video.bvid = item.optString("bvid");
                                video.title = item.optString("title");
                                video.cover = item.optString("pic");
                                JSONObject owner = item.optJSONObject("owner");
                                if (owner != null) {
                                    video.upName = owner.optString("name");
                                }
                                JSONObject stat = item.optJSONObject("stat");
                                if (stat != null) {
                                    video.view = stat.optInt("view") + "观看";
                                }
                                videos.add(video);
                            }
                            runOnUiThread(() -> {
                                recommendList.clear();
                                recommendList.addAll(videos);
                                if (recommendAdapter != null) {
                                    recommendAdapter.updateList(recommendList);
                                }
                                if (verticalRecommendAdapter != null) {
                                    verticalRecommendAdapter.updateList(recommendList);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadComments() {
        currentCommentPage = 1;

        new Thread(() -> {
            long aidNum = PartitionApi.getAidByBvid(bvid);
            List<PartitionApi.Comment> comments = PartitionApi.getComments(bvid, currentCommentPage);
            final long finalAid = aidNum;

            runOnUiThread(() -> {
                if (comments != null && !comments.isEmpty()) {
                    commentList.clear();
                    commentList.addAll(comments);
                    // 修改：添加 findViewById(android.R.id.content) 参数
                    commentAdapter = new CommentAdapter(commentList, finalAid, this, findViewById(android.R.id.content));
                    commentRecyclerView.setAdapter(commentAdapter);

                    commentRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);
                            LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                            int totalItemCount = lm.getItemCount();
                            int lastVisibleItem = lm.findLastVisibleItemPosition();
                            if (!isLoadingMore && lastVisibleItem >= totalItemCount - 3) {
                                loadMoreComments();
                            }
                        }
                    });
                } else {
                    commentPlaceholder.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void loadMoreComments() {
        if (isLoadingMore) return;

        isLoadingMore = true;

        int nextPage = currentCommentPage + 1;

        new Thread(() -> {
            List<PartitionApi.Comment> moreComments = PartitionApi.getComments(bvid, nextPage);

            runOnUiThread(() -> {
                isLoadingMore = false;

                if (moreComments != null && !moreComments.isEmpty()) {
                    List<PartitionApi.Comment> newComments = new ArrayList<>();
                    for (PartitionApi.Comment newComment : moreComments) {
                        boolean exists = false;
                        for (PartitionApi.Comment existing : commentList) {
                            if (existing.rpid == newComment.rpid) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) newComments.add(newComment);
                    }
                    if (!newComments.isEmpty()) {
                        commentList.addAll(newComments);
                        commentAdapter.updateList(commentList);
                        currentCommentPage = nextPage;

                        showHoloSnackbar("已加载更多" + newComments.size() + "条评论 (￣▽￣*)");
                    }
                }
            });
        }).start();
    }

    private void loadVerticalComments() {
        currentVerticalCommentPage = 1;

        new Thread(() -> {
            long aidNum = PartitionApi.getAidByBvid(bvid);
            List<PartitionApi.Comment> comments = PartitionApi.getComments(bvid, currentVerticalCommentPage);
            final long finalAid = aidNum;

            runOnUiThread(() -> {
                if (comments != null && !comments.isEmpty()) {
                    verticalCommentList.clear();
                    verticalCommentList.addAll(comments);
                    // 修改：添加 findViewById(android.R.id.content) 参数
                    verticalCommentAdapter = new CommentAdapter(verticalCommentList, finalAid, this, findViewById(android.R.id.content));
                    verticalCommentRecyclerView.setAdapter(verticalCommentAdapter);

                    verticalCommentRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);
                            LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                            int totalItemCount = lm.getItemCount();
                            int lastVisibleItem = lm.findLastVisibleItemPosition();
                            if (!isLoadingMoreVertical && lastVisibleItem >= totalItemCount - 3) {
                                loadMoreVerticalComments();
                            }
                        }
                    });
                } else {
                    verticalCommentPlaceholder.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void loadMoreVerticalComments() {
        if (isLoadingMoreVertical) return;

        isLoadingMoreVertical = true;

        int nextPage = currentVerticalCommentPage + 1;

        new Thread(() -> {
            List<PartitionApi.Comment> moreComments = PartitionApi.getComments(bvid, nextPage);

            runOnUiThread(() -> {
                isLoadingMoreVertical = false;

                if (moreComments != null && !moreComments.isEmpty()) {
                    List<PartitionApi.Comment> newComments = new ArrayList<>();
                    for (PartitionApi.Comment newComment : moreComments) {
                        boolean exists = false;
                        for (PartitionApi.Comment existing : verticalCommentList) {
                            if (existing.rpid == newComment.rpid) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) newComments.add(newComment);
                    }
                    if (!newComments.isEmpty()) {
                        verticalCommentList.addAll(newComments);
                        verticalCommentAdapter.updateList(verticalCommentList);
                        currentVerticalCommentPage = nextPage;

                        showHoloSnackbar("已加载更多" + newComments.size() + "条评论 (￣▽￣*)");
                    }
                }
            });
        }).start();
    }

    private void prepareExoPlayerInBackground(String videoUrl) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .header("Referer", "https://www.bilibili.com/")
                            .header("Cookie", getCookie())
                            .build();
                    return chain.proceed(request);
                })
                .build();

        DataSource.Factory dataSourceFactory = new OkHttpDataSource.Factory(httpClient);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory))
                .build();

        playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                runOnUiThread(() -> {
                    if (playbackState == Player.STATE_READY) {
                        loadingView.setVisibility(View.GONE);
                        loadingView.setBackgroundColor(0x00000000);
                        loadingView.setImageDrawable(null);

                        playerView.setVisibility(View.VISIBLE);
                        player.play();
                    }
                });
            }
        });

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
        player.setMediaItem(mediaItem);
        player.prepare();

        handler.postDelayed(() -> {
            if (loadingView.getVisibility() == View.VISIBLE) {
                loadingView.setVisibility(View.GONE);
                loadingView.setBackgroundColor(0x00000000);
                loadingView.setImageDrawable(null);
                playerView.setVisibility(View.VISIBLE);
                if (player != null) {
                    player.play();
                }
            }
        }, 3000);
    }

    private String fetchVideoUrl() {
        try {
            String cidUrl = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
            OkHttpClient client = new OkHttpClient.Builder().build();
            Request cidReq = new Request.Builder()
                    .url(cidUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("Cookie", getCookie())
                    .build();
            try (okhttp3.Response cidRes = client.newCall(cidReq).execute()) {
                if (cidRes.body() == null) return null;
                JSONObject cidObj = new JSONObject(cidRes.body().string());
                if (cidObj.optInt("code") != 0) return null;
                long cid = cidObj.getJSONObject("data").getLong("cid");
                aid = cidObj.getJSONObject("data").optString("aid");

                String playUrl = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid
                        + "&cid=" + cid + "&qn=64&fnval=1&platform=android";
                Request playReq = new Request.Builder()
                        .url(playUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .addHeader("Cookie", getCookie())
                        .build();
                try (okhttp3.Response playRes = client.newCall(playReq).execute()) {
                    if (playRes.body() == null) return null;
                    JSONObject playObj = new JSONObject(playRes.body().string());
                    if (playObj.optInt("code") != 0) return null;
                    JSONObject data = playObj.getJSONObject("data");
                    if (data.has("durl")) {
                        return data.getJSONArray("durl").getJSONObject(0).getString("url");
                    }
                    if (data.has("dash")) {
                        JSONObject dash = data.getJSONObject("dash");
                        if (dash.has("video")) {
                            return dash.getJSONArray("video").getJSONObject(0).getString("baseUrl");
                        }
                    }
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getCookie() {
        return SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null && playerView.getVisibility() == View.VISIBLE) {
            player.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void shareVideoLink() {
        String videoUrl = "https://www.bilibili.com/video/" + bvid;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, videoUrl);
        startActivity(Intent.createChooser(shareIntent, "分享视频"));
    }

    private void showFavoriteDialog() {
        if (aid == null || aid.isEmpty()) {
            showHoloSnackbar("获取视频信息失败");
            return;
        }

        final long aidNum = Long.parseLong(aid);
        showHoloSnackbar("正在获取收藏夹列表...");

        new Thread(() -> {
            List<PartitionApi.FavoriteFolder> folders = PartitionApi.getFavoriteFolders();

            runOnUiThread(() -> {
                if (folders == null || folders.isEmpty()) {
                    showHoloSnackbar("获取收藏夹失败，请稍后再试");
                    return;
                }

                String[] folderNames = new String[folders.size()];
                for (int i = 0; i < folders.size(); i++) {
                    PartitionApi.FavoriteFolder f = folders.get(i);
                    folderNames[i] = f.name + " (" + f.count + "个视频)";
                }

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("选择收藏夹");
                builder.setItems(folderNames, (dialog, which) -> {
                    PartitionApi.FavoriteFolder selected = folders.get(which);
                    addToFavorite(aidNum, selected.id, selected.name);
                });
                builder.setNegativeButton("取消", null);
                builder.show();
            });
        }).start();
    }

    private void addToFavorite(long aidNum, long folderId, String folderName) {
        showHoloSnackbar("正在添加到「" + folderName + "」...");

        new Thread(() -> {
            PartitionApi.FavoriteResult result = PartitionApi.addToFavorite(aidNum, folderId);

            runOnUiThread(() -> {
                showHoloSnackbar(result.message);
                if (result.success) {
                    // 收藏成功后可以更新图标（可选）
                }
            });
        }).start();
    }

    private void checkCoinStatus() {
        if (aid == null || aid.isEmpty()) return;
        final long aidNum = Long.parseLong(aid);

        new Thread(() -> {
            int coins = PartitionApi.getCoinCount(aidNum);
            runOnUiThread(() -> {
                coinCount = coins;
                updateCoinIcon();
            });
        }).start();
    }

    private void updateCoinIcon() {
        TextView coinText = findViewById(R.id.coinText);
        ImageView coinIcon = findViewById(R.id.coinIcon);
        if (coinCount > 0) {
            coinText.setText("已投" + coinCount);
            coinText.setTextColor(Color.parseColor("#FB7299"));
            coinIcon.setImageResource(R.drawable.coin);
        } else {
            coinText.setText("投币");
            coinText.setTextColor(Color.parseColor("#666666"));
            coinIcon.setImageResource(R.drawable.coin);
        }
    }

    private void performCoinSimple() {
        if (aid == null || aid.isEmpty()) {
            showHoloSnackbar("获取视频信息失败");
            return;
        }

        final long aidNum = Long.parseLong(aid);

        if (coinCount >= 2) {
            showHoloSnackbar("最多只能投2枚硬币");
            return;
        }

        int multiply = 1;
        showHoloSnackbar("正在投币...");

        new Thread(() -> {
            PartitionApi.CoinResult result = PartitionApi.addCoin(aidNum, multiply, 0);

            runOnUiThread(() -> {
                showHoloSnackbar(result.message);
                if (result.success) {
                    coinCount += multiply;
                    updateCoinIcon();
                }
            });
        }).start();
    }
}