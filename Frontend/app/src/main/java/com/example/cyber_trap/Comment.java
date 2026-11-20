package com.example.cyber_trap;

public class Comment {
    public String id;
    public String parentId;
    public String authorName;
    public String authorInitials;
    public String timeAgo;
    public String content;
    public int likeCount;
    public boolean isLiked;
    public boolean isReply;

    public Comment(String id, String parentId, String authorName, String authorInitials,
                   String timeAgo, String content, boolean isReply) {
        this.id = id;
        this.parentId = parentId;
        this.authorName = authorName;
        this.authorInitials = authorInitials;
        this.timeAgo = timeAgo;
        this.content = content;
        this.likeCount = 0;
        this.isLiked = false;
        this.isReply = isReply;
    }
}