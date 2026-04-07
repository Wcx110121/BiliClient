package com.wcxawa.biliclient.util;

public class StringUtil {

    // 将播放量转换为"万"、"亿"格式
    public static String toWan(long count) {
        if (count >= 100000000) {
            return String.format("%.1f亿", count / 100000000.0);
        } else if (count >= 10000) {
            return String.format("%.1f万", count / 10000.0);
        }
        return String.valueOf(count);
    }

    // 将播放量转换为"万"、"亿"格式（int版本）
    public static String toWan(int count) {
        return toWan((long) count);
    }
}