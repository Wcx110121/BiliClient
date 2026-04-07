package com.wcxawa.biliretro;

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
import com.wcxawa.biliretro.model.VideoCard;

@UnstableApi
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {

    private final List<VideoCard> videoList;

    public VideoAdapter(List<VideoCard> videoList) {
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoCard video = videoList.get(position);
        holder.title.setText(video.title);
        holder.upName.setText(video.upName);
        holder.view.setText(video.view);

        // 使用Glide加载封面，失败时显示loadtv图片
        if (video.cover != null && !video.cover.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(video.cover)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.loadtv)  // 加载中显示loadtv
                            .error(R.drawable.loadtv)        // 加载失败显示loadtv
                            .centerCrop())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.cover);
        } else {
            // 没有封面URL时直接显示loadtv
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.loadtv)
                    .apply(new RequestOptions().centerCrop())
                    .into(holder.cover);
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

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, upName, view;
        ImageView cover;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            upName = itemView.findViewById(R.id.upName);
            view = itemView.findViewById(R.id.view);
            cover = itemView.findViewById(R.id.cover);
        }
    }
}