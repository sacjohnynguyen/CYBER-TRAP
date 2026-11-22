package com.example.cyber_trap;

public class NotificationItem {
    public String id;
    public String actorName;
    public String actorInitials;
    public String actionType; // "LIKE", "COMMENT", "SYSTEM"
    public String postTitle;
    public int postPosition;
    public String timeAgo;
    public boolean isRead;

    public NotificationItem(String id, String actorName, String actorInitials, String actionType,
                            String postTitle, int postPosition, String timeAgo, boolean isRead) {
        this.id = id;
        this.actorName = actorName;
        this.actorInitials = actorInitials;
        this.actionType = actionType;
        this.postTitle = postTitle;
        this.postPosition = postPosition;
        this.timeAgo = timeAgo;
        this.isRead = isRead;
    }
}
