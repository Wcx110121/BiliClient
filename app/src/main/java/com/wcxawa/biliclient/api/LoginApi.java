package com.wcxawa.biliclient.api;

import com.wcxawa.biliclient.util.NetWorkUtil;
import org.json.JSONObject;

public class LoginApi {

    // 获取二维码信息
    public static QRInfo getQRInfo() {
        try {
            String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
            JSONObject result = NetWorkUtil.getJson(url);

            if (result.optInt("code") == 0) {
                JSONObject data = result.getJSONObject("data");
                QRInfo info = new QRInfo();
                info.qrcodeKey = data.getString("qrcode_key");
                info.qrcodeUrl = data.getString("url");
                return info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 轮询扫码状态
    public static int pollLoginStatus(String qrcodeKey) {
        try {
            String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcodeKey;
            JSONObject result = NetWorkUtil.getJson(url);
            return result.optInt("code");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static class QRInfo {
        public String qrcodeKey;
        public String qrcodeUrl;
    }
}