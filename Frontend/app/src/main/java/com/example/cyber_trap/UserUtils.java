package com.example.cyber_trap;

import android.content.Context;
import android.content.SharedPreferences;

public class UserUtils {

    public static String getLoggedUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String name = prefs.getString("user_name", null);
        if (name == null || name.isEmpty()) {
            SharedPreferences mainPrefs = context.getSharedPreferences("CyberTrapPrefs", Context.MODE_PRIVATE);
            name = mainPrefs.getString("LOGGED_USER", "Bảo Nam");
        }
        return name;
    }

    public static String getLoggedUserEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", null);
        if (email == null || email.isEmpty()) {
            String name = getLoggedUsername(context);
            email = name.contains("@") ? name : name.toLowerCase().replaceAll("\\s+", "") + "@gmail.com";
        }
        return email;
    }

    public static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "CT";
        String cleanName = name.trim().replaceAll("\\s+", " ");
        String[] parts = cleanName.split(" ");
        if (parts.length >= 2) {
            String first = parts[0].substring(0, 1).toUpperCase();
            String last = parts[parts.length - 1].substring(0, 1).toUpperCase();
            return first + last;
        } else {
            if (cleanName.length() >= 2) {
                return cleanName.substring(0, 2).toUpperCase();
            } else {
                return cleanName.toUpperCase();
            }
        }
    }
}
