package com.example.cyber_trap;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostActionListener {
        void onPostClick(Post post);
        void onEditPost(Post post, int position);
        void onDeletePost(Post post, int position);
    }

    private final Context context;
    private final List<Post> postList;
    private final OnPostActionListener listener;

    public PostAdapter(Context context, List<Post> postList, OnPostActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvAuthorAvatar.setText(post.authorInitials);
        holder.tvAuthorName.setText(post.authorName);
        holder.tvPostTime.setText(post.timeAgo);

        String privacyText = (post.privacy != null && !post.privacy.isEmpty()) ? post.privacy : "Công khai";
        holder.tvPostPrivacy.setText("  ·  " + privacyText);

        holder.tvPostTag.setText(post.tag);
        holder.tvPostTitle.setText(post.title);
        holder.tvPostContent.setText(post.content);
        holder.tvCommentCount.setText(post.commentCount + " Bình luận");

        if (post.imageUris != null && !post.imageUris.isEmpty()) {
            holder.layoutPostImages.setVisibility(View.VISIBLE);
            holder.ivPostImg1.setImageURI(post.imageUris.get(0));
            if (post.imageUris.size() > 1) {
                holder.ivPostImg2.setVisibility(View.VISIBLE);
                holder.ivPostImg2.setImageURI(post.imageUris.get(1));
            } else {
                holder.ivPostImg2.setVisibility(View.GONE);
            }
        } else {
            holder.layoutPostImages.setVisibility(View.GONE);
        }

        updateLikeUI(holder, post);

        holder.btnLike.setOnClickListener(v -> {
            post.isLiked = !post.isLiked;
            post.likeCount += post.isLiked ? 1 : -1;
            updateLikeUI(holder, post);
        });

        holder.btnMoreOptions.setVisibility(post.isOwner ? View.VISIBLE : View.GONE);
        holder.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMoreOptions);
            popup.getMenu().add(0, 1, 0, "Chỉnh sửa bài viết");
            popup.getMenu().add(0, 2, 1, "Xóa bài viết");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    listener.onEditPost(post, holder.getAdapterPosition());
                    return true;
                } else if (item.getItemId() == 2) {
                    listener.onDeletePost(post, holder.getAdapterPosition());
                    return true;
                }
                return false;
            });
            popup.show();
        });

        View.OnClickListener openDetail = v -> listener.onPostClick(post);
        holder.itemView.setOnClickListener(openDetail);
        holder.btnComment.setOnClickListener(openDetail);
    }

    private void updateLikeUI(PostViewHolder holder, Post post) {
        holder.tvLikeIcon.setText(post.isLiked ? "❤️" : "🤍");
        holder.tvLikeCount.setText(post.likeCount + " Tin cậy");
        holder.tvLikeCount.setTextColor(post.isLiked ? Color.parseColor("#D32F2F") : Color.parseColor("#555555"));
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorAvatar, tvAuthorName, tvPostTime, tvPostPrivacy, tvPostTag, tvPostTitle, tvPostContent;
        TextView btnMoreOptions, tvLikeIcon, tvLikeCount, tvCommentCount;
        LinearLayout layoutPostImages, btnLike, btnComment, btnShare;
        ImageView ivPostImg1, ivPostImg2;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorAvatar = itemView.findViewById(R.id.tvAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvPostPrivacy = itemView.findViewById(R.id.tvPostPrivacy);
            tvPostTag = itemView.findViewById(R.id.tvPostTag);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
            tvLikeIcon = itemView.findViewById(R.id.tvLikeIcon);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            layoutPostImages = itemView.findViewById(R.id.layoutPostImages);
            ivPostImg1 = itemView.findViewById(R.id.ivPostImg1);
            ivPostImg2 = itemView.findViewById(R.id.ivPostImg2);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}