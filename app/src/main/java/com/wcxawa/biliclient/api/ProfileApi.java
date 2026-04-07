package com.wcxawa.biliclient.api;

import com.wcxawa.biliclient.model.VideoCard;
import com.wcxawa.biliclient.util.NetWorkUtil;
import com.wcxawa.biliclient.util.SharedPreferencesUtil;
import com.wcxawa.biliclient.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProfileApi {

    // ========== 获取自己的信息 ==========

    public static String getMyMid() {
        try {
            String url = "https://api.bilibili.com/x/web-interface/nav";
            JSONObject result = NetWorkUtil.getJson(url);
            if (result.optInt("code") == 0) {
                return result.getJSONObject("data").optString("mid");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static UserInfo getUserInfo() {
        try {
            String mid = getMyMid();
            if (mid == null) return null;

            String url = "https://api.bilibili.com/x/space/acc/info?mid=" + mid;
            JSONObject result = NetWorkUtil.getJson(url);

            if (result.optInt("code") == 0) {
                JSONObject data = result.getJSONObject("data");
                UserInfo info = new UserInfo();
                info.mid = data.optString("mid");
                info.name = data.optString("name");
                info.face = data.optString("face");
                info.sign = data.optString("sign");
                info.level = data.optInt("level");
                return info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static UserStat getUserStat() {
        try {
            String mid = getMyMid();
            if (mid == null) return null;

            UserStat stat = new UserStat();

            String relationUrl = "https://api.bilibili.com/x/relation/stat?vmid=" + mid;
            JSONObject relationResult = NetWorkUtil.getJson(relationUrl);
            if (relationResult.optInt("code") == 0) {
                JSONObject data = relationResult.getJSONObject("data");
                stat.follower = data.optInt("follower");
                stat.following = data.optInt("following");
            }

            String upstatUrl = "https://api.bilibili.com/x/space/upstat?mid=" + mid;
            JSONObject upstatResult = NetWorkUtil.getJson(upstatUrl);
            if (upstatResult.optInt("code") == 0) {
                JSONObject data = upstatResult.getJSONObject("data");
                stat.likes = data.optInt("likes");
            }

            return stat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<VideoCard> getUserVideos() {
        List<VideoCard> videoList = new ArrayList<>();
        try {
            String mid = getMyMid();
            if (mid == null) return videoList;

            String url = "https://api.bilibili.com/x/space/arc/search?mid=" + mid + "&pn=1&ps=20";
            JSONObject result = NetWorkUtil.getJson(url);

            if (result.optInt("code") == 0) {
                JSONObject data = result.getJSONObject("data");
                JSONObject list = data.getJSONObject("list");
                JSONArray vlist = list.getJSONArray("vlist");

                for (int i = 0; i < vlist.length(); i++) {
                    JSONObject item = vlist.getJSONObject(i);
                    VideoCard video = new VideoCard();
                    video.bvid = item.optString("bvid");
                    video.title = item.optString("title");
                    video.cover = item.optString("pic");
                    video.upName = item.optString("author");
                    video.view = StringUtil.toWan(item.optInt("play")) + "观看";
                    videoList.add(video);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videoList;
    }

    // ========== 查看别人的信息 ==========

    public static UserInfo getUserInfoByMid(String mid) {
        try {
            String url = "https://api.bilibili.com/x/space/acc/info?mid=" + mid;
            JSONObject result = NetWorkUtil.getJson(url);

            if (result.optInt("code") == 0) {
                JSONObject data = result.getJSONObject("data");
                UserInfo info = new UserInfo();
                info.mid = data.optString("mid");
                info.name = data.optString("name");
                info.face = data.optString("face");
                info.sign = data.optString("sign");
                info.level = data.optInt("level");
                return info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static UserStat getUserStatByMid(String mid) {
        try {
            UserStat stat = new UserStat();

            String relationUrl = "https://api.bilibili.com/x/relation/stat?vmid=" + mid;
            JSONObject relationResult = NetWorkUtil.getJson(relationUrl);
            if (relationResult.optInt("code") == 0) {
                JSONObject data = relationResult.getJSONObject("data");
                stat.follower = data.optInt("follower");
                stat.following = data.optInt("following");
            }

            String upstatUrl = "https://api.bilibili.com/x/space/upstat?mid=" + mid;
            JSONObject upstatResult = NetWorkUtil.getJson(upstatUrl);
            if (upstatResult.optInt("code") == 0) {
                JSONObject data = upstatResult.getJSONObject("data");
                stat.likes = data.optInt("likes");
            }

            return stat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<VideoCard> getUserVideosByMid(String mid) {
        List<VideoCard> videoList = new ArrayList<>();
        try {
            String url = "https://api.bilibili.com/x/space/arc/search?mid=" + mid + "&pn=1&ps=20";
            JSONObject result = NetWorkUtil.getJson(url);

            if (result.optInt("code") == 0) {
                JSONObject data = result.getJSONObject("data");
                JSONObject list = data.getJSONObject("list");
                JSONArray vlist = list.getJSONArray("vlist");

                for (int i = 0; i < vlist.length(); i++) {
                    JSONObject item = vlist.getJSONObject(i);
                    VideoCard video = new VideoCard();
                    video.bvid = item.optString("bvid");
                    video.title = item.optString("title");
                    video.cover = item.optString("pic");
                    video.upName = item.optString("author");
                    video.view = StringUtil.toWan(item.optInt("play")) + "观看";
                    videoList.add(video);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videoList;
    }

    // ========== 数据类 ==========

    public static class UserInfo {
        public String mid;
        public String name;
        public String face;
        public String sign;
        public int level;
    }

    public static class UserStat {
        public int follower;
        public int following;
        public int likes;
    }
}