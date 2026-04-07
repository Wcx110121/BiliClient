package com.wcxawa.biliclient;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.wcxawa.biliclient.util.NetWorkUtil;
import com.wcxawa.biliclient.util.SharedPreferencesUtil;

public class ProfileFragment extends Fragment {

    private LinearLayout unloggedLayout;
    private LinearLayout loggedLayout;
    private Button webLoginBtn;
    private Button tokenImportBtn;
    private TextView userNameText;
    private Button logoutBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setGravity(android.view.Gravity.CENTER);

        // ========== 未登录时的布局 ==========
        unloggedLayout = new LinearLayout(getContext());
        unloggedLayout.setOrientation(LinearLayout.VERTICAL);
        unloggedLayout.setGravity(android.view.Gravity.CENTER);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(android.R.drawable.ic_dialog_info);
        icon.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        unloggedLayout.addView(icon);

        TextView tipText = new TextView(getContext());
        tipText.setText("登录后可以使用收藏、投币等功能");
        tipText.setTextSize(14);
        tipText.setTextColor(0xFF666666);
        tipText.setGravity(android.view.Gravity.CENTER);
        tipText.setPadding(0, 16, 0, 32);
        unloggedLayout.addView(tipText);

        // 网页账号密码登录
        webLoginBtn = new Button(getContext());
        webLoginBtn.setText("网页账号密码登录");
        webLoginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), WebLoginActivity.class);
            startActivity(intent);
        });
        unloggedLayout.addView(webLoginBtn);

        // Token 导入登录
        tokenImportBtn = new Button(getContext());
        tokenImportBtn.setText("Token 导入登录");
        tokenImportBtn.setOnClickListener(v -> showTokenImportDialog());
        unloggedLayout.addView(tokenImportBtn);

        // ========== 登录后的布局 ==========
        loggedLayout = new LinearLayout(getContext());
        loggedLayout.setOrientation(LinearLayout.VERTICAL);
        loggedLayout.setGravity(android.view.Gravity.CENTER);
        loggedLayout.setVisibility(View.GONE);

        userNameText = new TextView(getContext());
        userNameText.setTextSize(18);
        userNameText.setTextColor(0xFF333333);
        userNameText.setGravity(android.view.Gravity.CENTER);
        userNameText.setPadding(0, 0, 0, 32);
        loggedLayout.addView(userNameText);

        TextView comingSoon = new TextView(getContext());
        comingSoon.setText("个人主页功能施工中\n\n收藏、投币记录等敬请期待");
        comingSoon.setTextSize(14);
        comingSoon.setTextColor(0xFF999999);
        comingSoon.setGravity(android.view.Gravity.CENTER);
        comingSoon.setPadding(0, 32, 0, 32);
        loggedLayout.addView(comingSoon);

        logoutBtn = new Button(getContext());
        logoutBtn.setText("退出登录");
        logoutBtn.setOnClickListener(v -> {
            SharedPreferencesUtil.clearAll();
            Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
            updateUI();
        });
        loggedLayout.addView(logoutBtn);

        root.addView(unloggedLayout);
        root.addView(loggedLayout);

        return root;
    }

    private void showTokenImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("导入 Token 登录");
        builder.setMessage("请粘贴在其他设备上复制的 Token（Cookie 字符串）");

        final EditText input = new EditText(getContext());
        input.setHint("SESSDATA=xxx; bili_jct=xxx; DedeUserID=xxx");
        input.setPadding(50, 20, 50, 20);
        builder.setView(input);

        builder.setPositiveButton("登录", (dialog, which) -> {
            String token = input.getText().toString().trim();
            if (token.isEmpty()) {
                Toast.makeText(getContext(), "请输入 Token", Toast.LENGTH_SHORT).show();
                return;
            }
            importTokenAndLogin(token);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void importTokenAndLogin(String token) {
        if (!token.contains("SESSDATA=")) {
            Toast.makeText(getContext(), "Token 格式错误，请检查是否包含 SESSDATA", Toast.LENGTH_LONG).show();
            return;
        }

        // 保存 Cookie
        SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, token);

        // 提取并保存 mid
        String mid = extractCookieValue(token, "DedeUserID");
        if (mid != null && !mid.isEmpty()) {
            SharedPreferencesUtil.putString(SharedPreferencesUtil.mid, mid);
        }

        // 刷新网络请求头
        NetWorkUtil.refreshHeaders();

        Toast.makeText(getContext(), "登录成功！", Toast.LENGTH_LONG).show();

        // 刷新界面
        updateUI();
    }

    private String extractCookieValue(String cookie, String key) {
        if (cookie == null || cookie.isEmpty()) return null;
        String[] parts = cookie.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        if (SharedPreferencesUtil.isLoggedIn()) {
            unloggedLayout.setVisibility(View.GONE);
            loggedLayout.setVisibility(View.VISIBLE);

            String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
            String userName = extractCookieValue(cookie, "DedeUserID");
            if (userName != null && !userName.isEmpty()) {
                userNameText.setText("欢迎，用户 " + userName);
            } else {
                userNameText.setText("已登录");
            }
        } else {
            unloggedLayout.setVisibility(View.VISIBLE);
            loggedLayout.setVisibility(View.GONE);
        }
    }
}