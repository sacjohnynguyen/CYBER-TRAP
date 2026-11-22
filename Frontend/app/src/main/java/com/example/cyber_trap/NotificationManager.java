package com.example.cyber_trap;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationManager {
    private static final String PREF_NAME = "CyberTrapNotifs";
    private static final String KEY_NOTIFS = "NOTIF_LIST";

    public static List<NotificationItem> getNotifications(Context context) {
        if (context == null) return new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_NOTIFS, null);
            if (json == null || json.isEmpty()) {
                List<NotificationItem> initialList = new ArrayList<>();
                initialList.add(new NotificationItem(
                        UUID.randomUUID().toString(),
                        "Cyber Trap",
                        "CT",
                        "SYSTEM",
                        "Chào mừng bạn gia nhập cộng đồng chống lừa đảo!",
                        -1,
                        "Vừa xong",
                        false
                ));
                saveNotifications(context, initialList);
                return initialList;
            }
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<NotificationItem>>() {}.getType();
            ArrayList<NotificationItem> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveNotifications(Context context, List<NotificationItem> list) {
        if (context == null || list == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            prefs.edit().putString(KEY_NOTIFS, gson.toJson(list)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addNotification(Context context, NotificationItem item) {
        if (context == null || item == null) return;
        List<NotificationItem> list = getNotifications(context);
        list.add(0, item);
        saveNotifications(context, list);
    }

    public static int getUnreadCount(Context context) {
        List<NotificationItem> list = getNotifications(context);
        int unread = 0;
        for (NotificationItem item : list) {
            if (item != null && !item.isRead) {
                unread++;
            }
        }
        return unread;
    }

    public static void markAllAsRead(Context context) {
        List<NotificationItem> list = getNotifications(context);
        for (NotificationItem item : list) {
            if (item != null) {
                item.isRead = true;
            }
        }
        saveNotifications(context, list);
    }

    public static void markAsRead(Context context, String notifId) {
        if (notifId == null) return;
        List<NotificationItem> list = getNotifications(context);
        for (NotificationItem item : list) {
            if (item != null && notifId.equals(item.id)) {
                item.isRead = true;
                break;
            }
        }
        saveNotifications(context, list);
    }
}
