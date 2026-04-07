package com.wcxawa.biliretro.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    private static SharedPreferences sharedPreferences;

    public static final String cookies = "cookies";
    public static final String mid = "mid";

    public static void init(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("bili_client", Context.MODE_PRIVATE);
        }
    }

    public static void putString(String key, String value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(key, value).apply();
        }
    }

    public static String getString(String key, String defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public static void putLong(String key, long value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putLong(key, value).apply();
        }
    }

    public static long getLong(String key, long defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getLong(key, defaultValue);
        }
        return defaultValue;
    }

    public static void clearAll() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().apply();
        }
    }

    public static boolean isLoggedIn() {
        if (sharedPreferences == null) return false;
        String cookie = sharedPreferences.getString(cookies, "");
        return cookie != null && cookie.contains("SESSDATA=") && !cookie.isEmpty();
    }
}