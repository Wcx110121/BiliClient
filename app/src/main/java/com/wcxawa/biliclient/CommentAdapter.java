package com.wcxawa.biliclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.wcxawa.biliclient.api.PartitionApi;
import com.google.android.material.snackbar.Snackbar;
import java.io.Serializable;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<PartitionApi.Comment> commentList;
    private long oid;
    private Context context;
    private SharedPreferences likePrefs;
    private View snackbarAnchor;  // 用于显示 Snackbar 的锚点 View

    public CommentAdapter(List<PartitionApi.Comment> commentList, long oid, Context context, View snackbarAnchor) {
        this.commentList = commentList;
        this.oid = oid;
        this.context = context;
        this.snackbarAnchor = snackbarAnchor;
        this.likePrefs = context.getSharedPreferences("comment_likes", Context.MODE_PRIVATE);
    }

    // 重载方法，兼容旧调用
    public CommentAdapter(List<PartitionApi.Comment> commentList, long oid, Context context) {
        this(commentList, oid, context, null);
    }

    public void updateList(List<PartitionApi.Comment> newList) {
        this.commentList = newList;
        notifyDataSetChanged();
    }

    // 纯方角 Snackbar
    private void showHoloSnackbar(String message) {
        if (snackbarAnchor == null) return;
        Snackbar snackbar = Snackbar.make(snackbarAnchor, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor("#FF333333"));
        bg.setCornerRadius(0);

        snackbarView.setBackground(bg);
        snackbar.show();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PartitionApi.Comment comment = commentList.get(position);

        holder.userNameText.setText(comment.userName);
        holder.contentText.setText(comment.content);
        holder.timeText.setText(comment.getFormattedTime());

        int previewCount = comment.replies != null ? comment.replies.size() : 0;

        if (previewCount >= 3) {
            holder.replyCountText.setText("查看" + previewCount + "条及以上回复");
        } else if (previewCount > 0) {
            holder.replyCountText.setText(previewCount + "条回复");
        } else {
            holder.replyCountText.setText("回复");
        }

        String likeKey = "like_" + oid + "_" + comment.rpid;
        boolean isLiked = likePrefs.getBoolean(likeKey, false);

        if (isLiked) {
            holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_on);
            holder.likeText.setTextColor(Color.parseColor("#FB7299"));
            holder.likeText.setText(String.valueOf(comment.likeCount + 1));
        } else {
            holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_off);
            holder.likeText.setTextColor(Color.parseColor("#999999"));
            holder.likeText.setText(comment.likeCount > 0 ? String.valueOf(comment.likeCount) : "");
        }

        if (comment.userAvatar != null && !comment.userAvatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(comment.userAvatar)
                    .circleCrop()
                    .into(holder.avatarView);
        }

        if (comment.replies != null && !comment.replies.isEmpty()) {
            holder.replyPreviewContainer.setVisibility(View.VISIBLE);
            holder.replyPreviewContainer.removeAllViews();

            int showCount = Math.min(comment.replies.size(), 3);
            for (int i = 0; i < showCount; i++) {
                PartitionApi.SubComment reply = comment.replies.get(i);
                TextView replyView = new TextView(holder.itemView.getContext());
                replyView.setText(reply.userName + "：" + reply.content);
                replyView.setTextSize(12);
                replyView.setTextColor(Color.parseColor("#666666"));
                replyView.setPadding(0, 4, 0, 4);
                holder.replyPreviewContainer.addView(replyView);
            }
        } else {
            holder.replyPreviewContainer.setVisibility(View.GONE);
        }

        holder.likeLayout.setOnClickListener(v -> {
            if (holder.isLiking) return;
            holder.isLiking = true;

            String key = "like_" + oid + "_" + comment.rpid;
            boolean currentLiked = likePrefs.getBoolean(key, false);

            if (!currentLiked) {
                new Thread(() -> {
                    PartitionApi.LikeResult likeResult = PartitionApi.likeComment(oid, comment.rpid, 1);
                    holder.itemView.post(() -> {
                        holder.isLiking = false;
                        if (likeResult.success) {
                            likePrefs.edit().putBoolean(key, true).apply();
                            comment.likeCount++;
                            holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_on);
                            holder.likeText.setTextColor(Color.parseColor("#FB7299"));
                            holder.likeText.setText(String.valueOf(comment.likeCount));
                            showHoloSnackbar(likeResult.message);
                        } else {
                            showHoloSnackbar(likeResult.message);
                        }
                    });
                }).start();
            } else {
                holder.isLiking = false;
                showHoloSnackbar("你已经点过赞了");
            }
        });

        View.OnClickListener clickListener = v -> {
            Intent intent = new Intent(holder.itemView.getContext(), SubReplyActivity.class);
            intent.putExtra("subReplies", (Serializable) comment.replies);
            intent.putExtra("userName", comment.userName);
            intent.putExtra("content", comment.content);
            intent.putExtra("oid", oid);
            intent.putExtra("rpid", comment.rpid);
            holder.itemView.getContext().startActivity(intent);
        };

        holder.replyCountText.setOnClickListener(clickListener);
        holder.replyPreviewContainer.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarView;
        TextView userNameText;
        TextView contentText;
        TextView timeText;
        TextView likeText;
        TextView replyCountText;
        LinearLayout replyPreviewContainer;
        LinearLayout likeLayout;
        ImageView likeIcon;
        boolean isLiking = false;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.avatarView);
            userNameText = itemView.findViewById(R.id.userNameText);
            contentText = itemView.findViewById(R.id.contentText);
            timeText = itemView.findViewById(R.id.timeText);
            likeText = itemView.findViewById(R.id.likeText);
            replyCountText = itemView.findViewById(R.id.replyCountText);
            replyPreviewContainer = itemView.findViewById(R.id.replyPreviewContainer);
            likeLayout = itemView.findViewById(R.id.likeLayout);
            likeIcon = itemView.findViewById(R.id.likeIcon);
        }
    }
}