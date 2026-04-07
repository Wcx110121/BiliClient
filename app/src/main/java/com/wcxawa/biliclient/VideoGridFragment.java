package com.wcxawa.biliclient;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.wcxawa.biliclient.model.VideoCard;
import com.wcxawa.biliclient.api.PartitionApi;

public class VideoGridFragment extends Fragment {

    private static final String ARG_PARTITION_ID = "partition_id";
    private static final String ARG_PARTITION_NAME = "partition_name";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private VideoGridAdapter adapter;
    private List<VideoCard> videoList = new ArrayList<>();
    private List<VideoCard> cachedVideoList = new ArrayList<>();
    private int partitionId;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isDataLoaded = false;
    private View rootView;

    public static VideoGridFragment newInstance(int partitionId, String partitionName) {
        VideoGridFragment fragment = new VideoGridFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARTITION_ID, partitionId);
        args.putString(ARG_PARTITION_NAME, partitionName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            partitionId = getArguments().getInt(ARG_PARTITION_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_video_grid, container, false);
            recyclerView = rootView.findViewById(R.id.gridRecyclerView);
            progressBar = rootView.findViewById(R.id.progressBar);
            setupRecyclerView();
        } else {
            if (rootView.getParent() != null) {
                ((ViewGroup) rootView.getParent()).removeView(rootView);
            }
        }
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (isDataLoaded && !cachedVideoList.isEmpty()) {
            videoList.clear();
            videoList.addAll(cachedVideoList);
            adapter.setPlaceholderMode(false);
            adapter.updateList(videoList);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        } else if (!isDataLoaded) {
            loadData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isDataLoaded && getUserVisibleHint()) {
            loadData();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !isDataLoaded && isResumed() && rootView != null) {
            loadData();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int spanCount = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        if (recyclerView != null) {
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
            recyclerView.setLayoutManager(layoutManager);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void setupRecyclerView() {
        int spanCount = 2;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 4;
        }
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new VideoGridAdapter(videoList);
        adapter.setPlaceholderMode(true);
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        if (isLoading || isDataLoaded) return;
        isLoading = true;

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        new Thread(() -> {
            List<VideoCard> newVideos = PartitionApi.getPartitionVideos(partitionId, currentPage);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                isLoading = false;
                if (newVideos != null && !newVideos.isEmpty()) {
                    videoList.addAll(newVideos);
                    cachedVideoList.clear();
                    cachedVideoList.addAll(videoList);
                    adapter.setPlaceholderMode(false);
                    adapter.updateList(videoList);
                    currentPage++;
                    isDataLoaded = true;
                } else if (currentPage == 1) {
                    Toast.makeText(getContext(), "加载失败，请检查网络", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    public void forceRefresh() {
        if (isLoading) return;
        currentPage = 1;
        videoList.clear();
        cachedVideoList.clear();
        isDataLoaded = false;
        if (adapter != null) {
            adapter.setPlaceholderMode(true);
        }
        loadData();
    }
}