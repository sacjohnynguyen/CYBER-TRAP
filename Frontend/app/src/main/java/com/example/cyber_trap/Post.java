package com.example.cyber_trap;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class Post {
    public String id;
    public String authorName;
    public String authorInitials;
    public String timeAgo;
    public String tag;
    public String title;
    public String content;
    public ArrayList<Uri> imageUris;
    public int likeCount;
    public int commentCount;
    public boolean isLiked;
    public boolean isSaved;
    public boolean isOwner;
    public List<Comment> comments;
    public String privacy;

    public Post(String id, String authorName, String authorInitials, String timeAgo,
                String tag, String title, String content, ArrayList<Uri> imageUris, boolean isOwner) {
        this.id = id;
        this.authorName = authorName;
        this.authorInitials = authorInitials;
        this.timeAgo = timeAgo;
        this.tag = tag;
        this.title = title;
        this.content = content;
        this.imageUris = imageUris != null ? imageUris : new ArrayList<>();
        this.likeCount = 0;
        this.commentCount = 0;
        this.isLiked = false;
        this.isOwner = isOwner;
        this.comments = new ArrayList<>();
        this.privacy = "Công khai";
    }

    public ArrayList<String> imageUriStrings;

    public void syncImageStrings() {
        if (imageUriStrings == null) imageUriStrings = new ArrayList<>();
        imageUriStrings.clear();
        if (imageUris != null) {
            for (Uri uri : imageUris) {
                if (uri != null) imageUriStrings.add(uri.toString());
            }
        }
    }

    public void restoreImageUris() {
        if (imageUris == null) imageUris = new ArrayList<>();
        if (imageUriStrings != null) {
            imageUris.clear();
            for (String str : imageUriStrings) {
                if (str != null && !str.trim().isEmpty()) {
                    try {
                        imageUris.add(Uri.parse(str));
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}