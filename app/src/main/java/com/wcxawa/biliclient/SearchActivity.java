package com.wcxawa.biliretro;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.wcxawa.biliretro.api.PartitionApi;
import com.wcxawa.biliretro.model.VideoCard;
import com.wcxawa.biliretro.util.SharedPreferencesUtil;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private ImageView clearBtn;
    private ImageView searchBtn;
    private TextView emptyText;
    private ProgressBar loadingMore;
    private RecyclerView recyclerView;
    private VideoGridAdapter adapter;
    private List<VideoCard> videoList = new ArrayList<>();

    private String currentKeyword = "";
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("搜索");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        searchInput = findViewById(R.id.searchInput);
        clearBtn = findViewById(R.id.clearBtn);
        searchBtn = findViewById(R.id.searchBtn);
        emptyText = findViewById(R.id.emptyText);
        loadingMore = findViewById(R.id.loadingMore);
        recyclerView = findViewById(R.id.recyclerView);

        int spanCount = getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new VideoGridAdapter(videoList);
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        searchBtn.setOnClickListener(v -> performSearch());

        clearBtn.setOnClickListener(v -> {
            searchInput.setText("");
            clearBtn.setVisibility(View.GONE);
        });

        setupScrollListener();
    }

    private void performSearch() {
        String keyword = searchInput.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }

        String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
        if (cookie == null || cookie.isEmpty() || !cookie.contains("SESSDATA=")) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentKeyword = keyword;
        currentPage = 1;
        hasMore = true;
        isSearching = true;

        adapter.setPlaceholderMode(true);
        emptyText.setVisibility(View.GONE);

        search(currentPage);
    }

    private void search(int page) {
        if (isLoading) return;
        isLoading = true;

        if (page > 1) {
            loadingMore.setVisibility(View.VISIBLE);
        }

        new Thread(() -> {
            List<VideoCard> results = PartitionApi.searchVideos(currentKeyword, page);

            runOnUiThread(() -> {
                isLoading = false;
                loadingMore.setVisibility(View.GONE);

                if (results != null && !results.isEmpty()) {
                    if (page == 1) {
                        videoList.clear();
                        videoList.addAll(results);
                        adapter.setPlaceholderMode(false);
                        adapter.updateList(videoList);
                        emptyText.setVisibility(View.GONE);

                        if (results.size() < 36) {
                            hasMore = false;
                        }
                    } else {
                        for (VideoCard video : results) {
                            boolean exists = false;
                            for (VideoCard existing : videoList) {
                                if (existing.bvid.equals(video.bvid)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                videoList.add(video);
                            }
                        }
                        adapter.updateList(videoList);

                        if (results.size() < 36) {
                            hasMore = false;
                            if (!results.isEmpty()) {
                                Toast.makeText(this, "已加载全部内容", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    if (page == 1 && recyclerView.getLayoutManager() != null) {
                        recyclerView.getLayoutManager().scrollToPosition(0);
                    }
                } else {
                    if (page == 1) {
                        adapter.setPlaceholderMode(false);
                        adapter.updateList(new ArrayList<>());
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("没有找到相关内容\n试试其他关键词吧");
                    } else {
                        hasMore = false;
                        if (videoList.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                        }
                    }
                }

                isSearching = false;
            });
        }).start();
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                int totalItemCount = lm.getItemCount();
                int lastVisibleItem = lm.findLastVisibleItemPosition();

                if (!isLoading && hasMore && !isSearching && lastVisibleItem >= totalItemCount - 5) {
                    currentPage++;
                    search(currentPage);
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int spanCount = newConfig.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}