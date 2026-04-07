package com.wcxawa.biliclient.util;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetWorkUtil {
    private static OkHttpClient client;
    public static final ArrayList<String> webHeaders = new ArrayList<>();

    static {
        webHeaders.add("User-Agent");
        webHeaders.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        webHeaders.add("Referer");
        webHeaders.add("https://www.bilibili.com/");
        webHeaders.add("Cookie");
        webHeaders.add("");
    }

    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    public static JSONObject getJson(String url) throws IOException, JSONException {
        Request.Builder builder = new Request.Builder().url(url);
        for (int i = 0; i < webHeaders.size(); i += 2) {
            builder.addHeader(webHeaders.get(i), webHeaders.get(i + 1));
        }
        Request request = builder.build();
        Response response = getClient().newCall(request).execute();

        if (response.isSuccessful() && response.body() != null) {
            return new JSONObject(response.body().string());
        }
        throw new IOException("请求失败: " + response.code());
    }

    public static void refreshHeaders() {
        String cookiesStr = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
        for (int i = 0; i < webHeaders.size(); i += 2) {
            if (webHeaders.get(i).equals("Cookie")) {
                webHeaders.set(i + 1, cookiesStr);
                break;
            }
        }
    }

    public static class FormData {
        private Map<String, String> data = new HashMap<>();

        public FormData put(String key, Object value) {
            data.put(key, String.valueOf(value));
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            try {
                for (String key : data.keySet()) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(URLEncoder.encode(key, "UTF-8"));
                    sb.append("=");
                    sb.append(URLEncoder.encode(data.get(key), "UTF-8"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return sb.toString();
        }
    }
}