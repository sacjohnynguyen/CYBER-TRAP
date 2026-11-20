package com.example.cyber_trap;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnReplyClickListener {
        void onReplyClick(Comment comment);
    }

    private final Context context;
    private final List<Comment> commentList;
    private final OnReplyClickListener replyListener;

    public CommentAdapter(Context context, List<Comment> commentList, OnReplyClickListener replyListener) {
        this.context = context;
        this.commentList = commentList;
        this.replyListener = replyListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvCommentAvatar.setText(comment.authorInitials);
        holder.tvCommentAuthor.setText(comment.authorName);
        holder.tvCommentContent.setText(comment.content);
        holder.tvCommentTime.setText(comment.timeAgo);

        int indentPx = comment.isReply ? (int) (42 * context.getResources().getDisplayMetrics().density) : 0;
        holder.layoutCommentRoot.setPadding(indentPx, holder.layoutCommentRoot.getPaddingTop(), 0, holder.layoutCommentRoot.getPaddingBottom());

        updateLikeUI(holder, comment);
        holder.btnCommentLike.setOnClickListener(v -> {
            comment.isLiked = !comment.isLiked;
            comment.likeCount += comment.isLiked ? 1 : -1;
            updateLikeUI(holder, comment);
        });

        holder.btnCommentReply.setOnClickListener(v -> replyListener.onReplyClick(comment));
    }

    private void updateLikeUI(CommentViewHolder holder, Comment comment) {
        String label = comment.likeCount > 0 ? "Thích (" + comment.likeCount + ")" : "Thích";
        holder.btnCommentLike.setText(label);
        holder.btnCommentLike.setTextColor(comment.isLiked ? Color.parseColor("#D32F2F") : Color.parseColor("#555555"));
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutCommentRoot;
        TextView tvCommentAvatar, tvCommentAuthor, tvCommentContent, tvCommentTime, btnCommentLike, btnCommentReply;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCommentRoot = itemView.findViewById(R.id.layoutCommentRoot);
            tvCommentAvatar = itemView.findViewById(R.id.tvCommentAvatar);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            btnCommentLike = itemView.findViewById(R.id.btnCommentLike);
            btnCommentReply = itemView.findViewById(R.id.btnCommentReply);
        }
    }
}