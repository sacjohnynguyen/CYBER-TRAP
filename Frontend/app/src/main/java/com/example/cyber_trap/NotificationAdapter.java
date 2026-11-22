package com.example.cyber_trap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    public interface OnNotifClickListener {
        void onNotifClick(NotificationItem item);
    }

    private final Context context;
    private final List<NotificationItem> notifList;
    private final OnNotifClickListener listener;

    public NotificationAdapter(Context context, List<NotificationItem> notifList, OnNotifClickListener listener) {
        this.context = context;
        this.notifList = notifList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        NotificationItem item = notifList.get(position);

        holder.tvNotifAvatar.setText(item.actorInitials != null ? item.actorInitials : "CT");
        holder.tvNotifTime.setText(item.timeAgo != null ? item.timeAgo : "Vừa xong");

        if ("LIKE".equalsIgnoreCase(item.actionType)) {
            holder.tvNotifActionBadge.setText("❤️");
        } else if ("COMMENT".equalsIgnoreCase(item.actionType)) {
            holder.tvNotifActionBadge.setText("💬");
        } else {
            holder.tvNotifActionBadge.setText("🛡️");
        }

        String fullMessage;
        if ("LIKE".equalsIgnoreCase(item.actionType)) {
            fullMessage = item.actorName + " đã bày tỏ tin cậy bài viết: " + (item.postTitle != null ? item.postTitle : "");
        } else if ("COMMENT".equalsIgnoreCase(item.actionType)) {
            fullMessage = item.actorName + " đã bình luận về bài viết: " + (item.postTitle != null ? item.postTitle : "");
        } else {
            fullMessage = item.actorName + ": " + (item.postTitle != null ? item.postTitle : "");
        }

        SpannableString spannable = new SpannableString(fullMessage);
        int nameEnd = item.actorName != null ? item.actorName.length() : 0;
        if (nameEnd > 0 && nameEnd <= fullMessage.length()) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, nameEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.tvNotifMessage.setText(spannable);

        holder.dotUnread.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
        holder.itemView.setBackgroundColor(item.isRead ? Color.parseColor("#FFFFFF") : Color.parseColor("#E7F3FF"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotifClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifList != null ? notifList.size() : 0;
    }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        TextView tvNotifAvatar, tvNotifActionBadge, tvNotifMessage, tvNotifTime;
        View dotUnread;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotifAvatar = itemView.findViewById(R.id.tvNotifAvatar);
            tvNotifActionBadge = itemView.findViewById(R.id.tvNotifActionBadge);
            tvNotifMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvNotifTime = itemView.findViewById(R.id.tvNotifTime);
            dotUnread = itemView.findViewById(R.id.dotUnread);
        }
    }
}
