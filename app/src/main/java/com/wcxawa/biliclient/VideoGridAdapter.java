package com.wcxawa.biliclient;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.util.UnstableApi;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;
import com.wcxawa.biliclient.model.VideoCard;

@UnstableApi
public class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.ViewHolder> {

    private List<VideoCard> videoList;
    private boolean isPlaceholder = false;

    public VideoGridAdapter(List<VideoCard> videoList) {
        this.videoList = videoList;
    }

    public void setPlaceholderMode(boolean enabled) {
        this.isPlaceholder = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isPlaceholder) {
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.loadtv)
                    .apply(new RequestOptions().centerCrop())
                    .into(holder.coverImage);
            holder.titleText.setText("加载中...");
            holder.upNameText.setText("等待数据");
            holder.viewText.setText("----观看");
            holder.itemView.setOnClickListener(null);
        } else if (position < videoList.size()) {
            VideoCard video = videoList.get(position);
            holder.titleText.setText(video.title);
            holder.upNameText.setText(video.upName);
            holder.viewText.setText(video.view);

            if (video.cover != null && !video.cover.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(video.cover)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.loadtv)
                                .error(R.drawable.loadtv)
                                .centerCrop())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.coverImage);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.loadtv)
                        .apply(new RequestOptions().centerCrop())
                        .into(holder.coverImage);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), VideoDetailActivity.class);
                intent.putExtra("title", video.title);
                intent.putExtra("upName", video.upName);
                intent.putExtra("view", video.view);
                intent.putExtra("cover", video.cover);
                intent.putExtra("bvid", video.bvid);
                holder.itemView.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        if (isPlaceholder) {
            return 36;
        }
        return videoList.size();
    }

    public void updateList(List<VideoCard> newList) {
        this.videoList = newList;
        isPlaceholder = false;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView coverImage;
        public TextView titleText, upNameText, viewText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.coverImage);
            titleText = itemView.findViewById(R.id.titleText);
            upNameText = itemView.findViewById(R.id.upNameText);
            viewText = itemView.findViewById(R.id.viewText);
        }
    }
}