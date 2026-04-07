package com.wcxawa.biliclient.model;

public class VideoCard {
    public String title;
    public String upName;
    public String view;
    public String cover;
    public String bvid;

    public VideoCard(String title, String upName, String view, String cover, String bvid) {
        this.title = title;
        this.upName = upName;
        this.view = view;
        this.cover = cover;
        this.bvid = bvid;
    }

    // 空构造方法（用于其他场景）
    public VideoCard() {
    }
}