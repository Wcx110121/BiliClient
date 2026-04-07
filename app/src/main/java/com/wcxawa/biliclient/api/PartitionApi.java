package com.wcxawa.biliretro.api;

import com.wcxawa.biliretro.model.VideoCard;
import com.wcxawa.biliretro.util.NetWorkUtil;
import com.wcxawa.biliretro.util.SharedPreferencesUtil;
import com.wcxawa.biliretro.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class PartitionApi {

    public static final int PARTITION_POPULAR = 1;
    public static final int PARTITION_ANIMATION = 2;
    public static final int PARTITION_MUSIC = 3;
    public static final int PARTITION_GAME = 4;
    public static final int PARTITION_TECH = 5;
    public static final int PARTITION_LIFE = 6;

    public static String getPartitionName(int partitionId) {
        switch (partitionId) {
            case PARTITION_POPULAR: return "热门";
            case PARTITION_ANIMATION: return "动画";
            case PARTITION_MUSIC: return "音乐";
            case PARTITION_GAME: return "游戏";
            case PARTITION_TECH: return "科技";
            case PARTITION_LIFE: return "生活";
            default: return "热门";
        }
    }

    public static List<VideoCard> getPartitionVideos(int partitionId, int page) {
        List<VideoCard> videoList = new ArrayList<>();
        try {
            String url = getPartitionUrl(partitionId, page);
            if (url == null) return videoList;
            JSONObject result = NetWorkUtil.getJson(url);
            if (result.has("data") && !result.isNull("data")) {
                JSONArray items = null;
                if (partitionId == PARTITION_POPULAR) {
                    if (result.getJSONObject("data").has("list")) {
                        items = result.getJSONObject("data").getJSONArray("list");
                    }
                } else {
                    if (result.has("data") && result.getJSONObject("data").has("archives")) {
                        items = result.getJSONObject("data").getJSONArray("archives");
                    }
                }
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String bvid = item.optString("bvid", "");
                        if (bvid.isEmpty()) continue;
                        String title = item.optString("title", "无标题");

                        String cover = item.optString("pic", "");
                        if (!cover.isEmpty() && cover.startsWith("//")) {
                            cover = "https:" + cover;
                        } else if (!cover.isEmpty() && cover.startsWith("http://")) {
                            cover = cover.replace("http://", "https://");
                        }

                        String upName = "未知UP主";
                        String view = "0观看";
                        if (item.has("owner") && !item.isNull("owner")) {
                            upName = item.getJSONObject("owner").optString("name", "未知UP主");
                        }
                        if (item.has("stat") && !item.isNull("stat")) {
                            int viewCount = item.getJSONObject("stat").optInt("view", 0);
                            view = StringUtil.toWan(viewCount) + "观看";
                        }
                        videoList.add(new VideoCard(title, upName, view, cover, bvid));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videoList;
    }

    private static String getPartitionUrl(int partitionId, int page) {
        switch (partitionId) {
            case PARTITION_POPULAR:
                return "https://api.bilibili.com/x/web-interface/popular?pn=" + page + "&ps=36";
            case PARTITION_ANIMATION:
                return "https://api.bilibili.com/x/web-interface/dynamic/region?ps=36&rid=1";
            case PARTITION_MUSIC:
                return "https://api.bilibili.com/x/web-interface/dynamic/region?ps=36&rid=3";
            case PARTITION_GAME:
                return "https://api.bilibili.com/x/web-interface/dynamic/region?ps=36&rid=4";
            case PARTITION_TECH:
                return "https://api.bilibili.com/x/web-interface/dynamic/region?ps=36&rid=36";
            case PARTITION_LIFE:
                return "https://api.bilibili.com/x/web-interface/dynamic/region?ps=36&rid=160";
            default:
                return null;
        }
    }

    public static List<VideoCard> searchVideos(String keyword, int page) {
        List<VideoCard> videoList = new ArrayList<>();
        try {
            String url = "https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword="
                    + URLEncoder.encode(keyword, "UTF-8") + "&pn=" + page + "&ps=36";
            JSONObject result = NetWorkUtil.getJson(url);
            if (result.has("data") && !result.isNull("data")) {
                JSONObject data = result.getJSONObject("data");
                if (data.has("result")) {
                    JSONArray items = data.getJSONArray("result");
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String bvid = item.optString("bvid");
                        if (bvid.isEmpty()) continue;
                        String title = item.optString("title", "无标题");
                        title = title.replaceAll("<[^>]*>", "");

                        String cover = item.optString("pic", "");
                        if (!cover.isEmpty() && cover.startsWith("//")) {
                            cover = "https:" + cover;
                        } else if (!cover.isEmpty() && cover.startsWith("http://")) {
                            cover = cover.replace("http://", "https://");
                        }

                        String upName = item.optString("author", "未知UP主");
                        int play = item.optInt("play", 0);
                        String view = StringUtil.toWan(play) + "观看";
                        videoList.add(new VideoCard(title, upName, view, cover, bvid));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videoList;
    }

    // ========== 评论相关 ==========

    public static List<Comment> getComments(String bvid, int page) {
        List<Comment> commentList = new ArrayList<>();
        try {
            long aid = getAidByBvid(bvid);
            if (aid == 0) return commentList;
            String url = "https://api.bilibili.com/x/v2/reply?type=1&oid=" + aid + "&pn=" + page + "&ps=20";
            JSONObject result = NetWorkUtil.getJson(url);
            if (result.optInt("code") == 0) {
                JSONObject data = result.getJSONObject("data");
                JSONArray replies = data.optJSONArray("replies");
                if (replies != null) {
                    for (int i = 0; i < replies.length(); i++) {
                        JSONObject item = replies.getJSONObject(i);
                        Comment comment = new Comment();
                        comment.content = item.getJSONObject("content").optString("message");
                        JSONObject member = item.getJSONObject("member");
                        comment.userName = member.optString("uname");
                        comment.userAvatar = member.optString("avatar");
                        comment.time = item.optLong("ctime");
                        comment.likeCount = item.optInt("like");
                        comment.rpid = item.optLong("rpid");
                        JSONArray repliesList = item.optJSONArray("replies");
                        if (repliesList != null && repliesList.length() > 0) {
                            comment.replies = new ArrayList<>();
                            for (int j = 0; j < repliesList.length(); j++) {
                                JSONObject reply = repliesList.getJSONObject(j);
                                SubComment sub = new SubComment();
                                sub.content = reply.getJSONObject("content").optString("message");
                                JSONObject replyMember = reply.getJSONObject("member");
                                sub.userName = replyMember.optString("uname");
                                sub.userAvatar = replyMember.optString("avatar");
                                sub.likeCount = reply.optInt("like");
                                sub.rpid = reply.optLong("rpid");
                                comment.replies.add(sub);
                            }
                        }
                        commentList.add(comment);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return commentList;
    }

    public static long getAidByBvid(String bvid) {
        try {
            String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
            JSONObject result = NetWorkUtil.getJson(url);
            if (result.optInt("code") == 0) {
                return result.getJSONObject("data").optLong("aid");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static LikeResult likeComment(long oid, long rpid, int action) {
        LikeResult result = new LikeResult();
        result.success = false;

        try {
            String url = "https://api.bilibili.com/x/v2/reply/action";
            String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");

            String csrf = extractCsrfFromCookie(cookie);
            if (csrf == null || csrf.isEmpty()) {
                result.message = "登录信息无效，请重新登录";
                return result;
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            okhttp3.FormBody formBody = new okhttp3.FormBody.Builder()
                    .add("type", "1")
                    .add("oid", String.valueOf(oid))
                    .add("rpid", String.valueOf(rpid))
                    .add("action", String.valueOf(action))
                    .add("csrf", csrf)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("Cookie", cookie)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    String respBody = response.body().string();
                    JSONObject json = new JSONObject(respBody);
                    int code = json.optInt("code");

                    result.success = (code == 0);
                    result.code = code;

                    switch (code) {
                        case 0:
                            result.message = action == 1 ? "点赞成功" : "已取消点赞";
                            break;
                        case -101:
                            result.message = "登录已过期，请重新登录";
                            break;
                        case -111:
                            result.message = "验证失败，请重新登录";
                            break;
                        case -509:
                            result.message = "操作太快了，请稍后再试";
                            break;
                        case 12004:
                            result.message = "无法操作该评论";
                            break;
                        default:
                            result.message = json.optString("message", "操作失败");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.message = "网络错误，请重试";
        }

        return result;
    }

    private static String extractCsrfFromCookie(String cookie) {
        if (cookie == null || cookie.isEmpty()) return null;
        String[] parts = cookie.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("bili_jct=")) {
                return part.substring(9);
            }
        }
        return null;
    }

    private static String extractMidFromCookie(String cookie) {
        if (cookie == null || cookie.isEmpty()) return null;
        String[] parts = cookie.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("DedeUserID=")) {
                return part.substring(11);
            }
        }
        return null;
    }

    /**
     * 获取评论的所有回复（楼中楼）
     */
    public static List<SubComment> getAllReplies(long oid, long rpid) {
        List<SubComment> replyList = new ArrayList<>();
        int page = 1;

        while (true) {
            try {
                String url = "https://api.bilibili.com/x/v2/reply/reply?type=1&oid=" + oid
                        + "&root=" + rpid + "&pn=" + page + "&ps=20";

                JSONObject result = NetWorkUtil.getJson(url);

                if (result.optInt("code") == 0) {
                    JSONObject data = result.getJSONObject("data");
                    JSONArray replies = data.optJSONArray("replies");

                    if (replies != null && replies.length() > 0) {
                        for (int i = 0; i < replies.length(); i++) {
                            JSONObject item = replies.getJSONObject(i);
                            SubComment sub = new SubComment();
                            sub.content = item.getJSONObject("content").optString("message");
                            JSONObject member = item.getJSONObject("member");
                            sub.userName = member.optString("uname");
                            sub.userAvatar = member.optString("avatar");
                            sub.likeCount = item.optInt("like");
                            sub.rpid = item.optLong("rpid");
                            replyList.add(sub);
                        }
                        page++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        return replyList;
    }

    // ========== 收藏相关 ==========

    public static class FavoriteFolder {
        public long id;
        public String name;
        public int count;
    }

    public static List<FavoriteFolder> getFavoriteFolders() {
        List<FavoriteFolder> folders = new ArrayList<>();
        try {
            String mid = SharedPreferencesUtil.getString(SharedPreferencesUtil.mid, "");
            if (mid == null || mid.isEmpty()) {
                String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
                mid = extractMidFromCookie(cookie);
                if (mid != null && !mid.isEmpty()) {
                    SharedPreferencesUtil.putString(SharedPreferencesUtil.mid, mid);
                }
            }
            if (mid == null || mid.isEmpty()) {
                return folders;
            }

            String url = "https://api.bilibili.com/x/v3/fav/folder/created/list-all?type=2&up_mid=" + mid;
            String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("Cookie", cookie)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        JSONArray list = data.optJSONArray("list");
                        if (list != null) {
                            for (int i = 0; i < list.length(); i++) {
                                JSONObject folder = list.getJSONObject(i);
                                FavoriteFolder f = new FavoriteFolder();
                                f.id = folder.optLong("id", 0);
                                f.name = folder.optString("title", "未命名");
                                f.count = folder.optInt("media_count", 0);
                                folders.add(f);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return folders;
    }

    public static FavoriteResult addToFavorite(long aid, long folderId) {
        FavoriteResult result = new FavoriteResult();
        result.success = false;

        try {
            String url = "http://api.bilibili.com/medialist/gateway/coll/resource/deal";
            String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");

            String csrf = extractCsrfFromCookie(cookie);
            if (csrf == null || csrf.isEmpty()) {
                result.message = "登录信息无效，请重新登录";
                return result;
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            okhttp3.FormBody formBody = new okhttp3.FormBody.Builder()
                    .add("rid", String.valueOf(aid))
                    .add("type", "2")
                    .add("add_media_ids", String.valueOf(folderId))
                    .add("del_media_ids", "")
                    .add("csrf", csrf)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("Cookie", cookie)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    String respBody = response.body().string();
                    JSONObject json = new JSONObject(respBody);
                    int code = json.optInt("code");

                    result.success = (code == 0);
                    result.code = code;

                    switch (code) {
                        case 0:
                            result.message = "收藏成功";
                            break;
                        case 11201:
                            result.message = "已经收藏过了";
                            result.success = true;
                            break;
                        case 11203:
                            result.message = "达到收藏上限";
                            break;
                        case -101:
                            result.message = "登录已过期，请重新登录";
                            break;
                        case -111:
                            result.message = "验证失败，请重新登录";
                            break;
                        default:
                            result.message = json.optString("message", "操作失败");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.message = "网络错误，请重试";
        }

        return result;
    }

    // ========== 投币相关 ==========

    public static CoinResult addCoin(long aid, int multiply, int selectLike) {
        CoinResult result = new CoinResult();
        result.success = false;

        try {
            String url = "http://api.bilibili.com/x/web-interface/coin/add";
            String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");

            String csrf = extractCsrfFromCookie(cookie);
            if (csrf == null || csrf.isEmpty()) {
                result.message = "登录信息无效，请重新登录";
                return result;
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            okhttp3.FormBody formBody = new okhttp3.FormBody.Builder()
                    .add("aid", String.valueOf(aid))
                    .add("multiply", String.valueOf(multiply))
                    .add("select_like", String.valueOf(selectLike))
                    .add("csrf", csrf)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("Cookie", cookie)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    String respBody = response.body().string();
                    JSONObject json = new JSONObject(respBody);
                    int code = json.optInt("code");

                    result.success = (code == 0);
                    result.code = code;

                    switch (code) {
                        case 0:
                            result.message = "投币成功";
                            break;
                        case -104:
                            result.message = "硬币不足";
                            break;
                        case 34002:
                            result.message = "不能给自己投币";
                            break;
                        case 34003:
                            result.message = "非法的投币数量";
                            break;
                        case 34004:
                            result.message = "投币间隔太短，请稍后再试";
                            break;
                        case 34005:
                            result.message = "超过投币上限";
                            break;
                        case -101:
                            result.message = "登录已过期，请重新登录";
                            break;
                        case -111:
                            result.message = "验证失败，请重新登录";
                            break;
                        default:
                            result.message = json.optString("message", "操作失败");
                    }

                    if (result.success && json.has("data")) {
                        JSONObject data = json.getJSONObject("data");
                        result.likeSuccess = data.optBoolean("like", false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.message = "网络错误，请重试";
        }

        return result;
    }

    public static int getCoinCount(long aid) {
        try {
            String url = "https://api.bilibili.com/x/web-interface/archive/coins?aid=" + aid;
            String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("Cookie", cookie)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
                        return data.optInt("multiply", 0);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ========== 数据类 ==========

    public static class Comment implements Serializable {
        public String content;
        public String userName;
        public String userAvatar;
        public long time;
        public int likeCount;
        public long rpid;
        public List<SubComment> replies;

        public String getFormattedTime() {
            long diff = System.currentTimeMillis() / 1000 - time;
            if (diff < 60) {
                return diff + "秒前";
            } else if (diff < 3600) {
                return (diff / 60) + "分钟前";
            } else if (diff < 86400) {
                return (diff / 3600) + "小时前";
            } else {
                return (diff / 86400) + "天前";
            }
        }
    }

    public static class SubComment implements Serializable {
        public String content;
        public String userName;
        public String userAvatar;
        public int likeCount;
        public long rpid;
        public List<SubComment> replies;
    }

    public static class LikeResult {
        public boolean success;
        public int code;
        public String message;
    }

    public static class FavoriteResult {
        public boolean success;
        public int code;
        public String message;
    }

    public static class CoinResult {
        public boolean success;
        public int code;
        public String message;
        public boolean likeSuccess;
    }
}