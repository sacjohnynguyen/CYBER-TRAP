package com.example.cyber_trap;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ForumCacheManager {
    private static final String PREF_NAME = "CyberTrapForumPrefs";
    private static final String KEY_POSTS = "FORUM_POSTS";

    public static void savePosts(Context context, List<Post> posts) {
        if (context == null || posts == null) return;
        try {
            for (Post p : posts) {
                if (p != null) p.syncImageStrings();
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = gson.toJson(posts);
            prefs.edit().putString(KEY_POSTS, json).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Post> loadPosts(Context context) {
        if (context == null) return new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_POSTS, "[]");
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Post>>() {}.getType();
            ArrayList<Post> list = gson.fromJson(json, listType);
            if (list == null) list = new ArrayList<>();
            for (Post p : list) {
                if (p != null) p.restoreImageUris();
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
