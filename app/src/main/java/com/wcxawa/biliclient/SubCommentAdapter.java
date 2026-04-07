package com.wcxawa.biliretro;

import android.content.Context;
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
import com.wcxawa.biliretro.api.PartitionApi;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;

public class SubCommentAdapter extends RecyclerView.Adapter<SubCommentAdapter.ViewHolder> {

    private List<PartitionApi.SubComment> subCommentList;
    private long oid;
    private Context context;
    private SharedPreferences likePrefs;
    private View snackbarAnchor;  // 用于显示 Snackbar 的锚点 View

    public SubCommentAdapter(List<PartitionApi.SubComment> subCommentList, long oid, Context context, View snackbarAnchor) {
        this.subCommentList = subCommentList;
        this.oid = oid;
        this.context = context;
        this.snackbarAnchor = snackbarAnchor;
        if (context != null) {
            this.likePrefs = context.getSharedPreferences("comment_likes", Context.MODE_PRIVATE);
        }
    }

    // 重载方法，兼容旧调用
    public SubCommentAdapter(List<PartitionApi.SubComment> subCommentList, long oid, Context context) {
        this(subCommentList, oid, context, null);
    }

    public void updateList(List<PartitionApi.SubComment> newList) {
        this.subCommentList = newList;
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
                .inflate(R.layout.item_sub_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PartitionApi.SubComment subComment = subCommentList.get(position);

        holder.userNameText.setText(subComment.userName);
        holder.contentText.setText(subComment.content);

        if (oid != 0 && context != null) {
            String likeKey = "like_" + oid + "_" + subComment.rpid;
            boolean isLiked = likePrefs.getBoolean(likeKey, false);

            if (isLiked) {
                holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_on);
                holder.likeText.setTextColor(Color.parseColor("#FB7299"));
                holder.likeText.setText(String.valueOf(subComment.likeCount + 1));
            } else {
                holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_off);
                holder.likeText.setTextColor(Color.parseColor("#999999"));
                holder.likeText.setText(subComment.likeCount > 0 ? String.valueOf(subComment.likeCount) : "");
            }

            holder.likeLayout.setOnClickListener(v -> {
                if (holder.isLiking) return;
                holder.isLiking = true;

                String key = "like_" + oid + "_" + subComment.rpid;
                boolean currentLiked = likePrefs.getBoolean(key, false);

                if (!currentLiked) {
                    new Thread(() -> {
                        PartitionApi.LikeResult likeResult = PartitionApi.likeComment(oid, subComment.rpid, 1);
                        holder.itemView.post(() -> {
                            holder.isLiking = false;
                            if (likeResult.success) {
                                likePrefs.edit().putBoolean(key, true).apply();
                                subComment.likeCount++;
                                holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_on);
                                holder.likeText.setTextColor(Color.parseColor("#FB7299"));
                                holder.likeText.setText(String.valueOf(subComment.likeCount));
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
        }

        if (subComment.userAvatar != null && !subComment.userAvatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(subComment.userAvatar)
                    .circleCrop()
                    .into(holder.avatarView);
        }

        if (position < subCommentList.size() - 1) {
            holder.divider.setVisibility(View.VISIBLE);
        } else {
            holder.divider.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return subCommentList != null ? subCommentList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        ImageView avatarView;
        TextView userNameText;
        TextView contentText;
        TextView likeText;
        TextView replyCountText;
        ImageView likeIcon;
        LinearLayout likeLayout;
        View divider;
        boolean isLiking = false;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.subCommentContainer);
            avatarView = itemView.findViewById(R.id.subAvatarView);
            userNameText = itemView.findViewById(R.id.subUserNameText);
            contentText = itemView.findViewById(R.id.subContentText);
            likeText = itemView.findViewById(R.id.subLikeText);
            replyCountText = itemView.findViewById(R.id.subReplyCountText);
            likeIcon = itemView.findViewById(R.id.subLikeIcon);
            likeLayout = (LinearLayout) likeIcon.getParent();
            divider = itemView.findViewById(R.id.divider);
        }
    }
}