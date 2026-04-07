package com.wcxawa.biliretro;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.wcxawa.biliretro.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private ListView settingsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        settingsList = findViewById(R.id.settingsList);

        // 只保留获取Token和关于
        List<String> settingsItems = new ArrayList<>();
        settingsItems.add("获取当前账号 Token（复制）");
        settingsItems.add("关于");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                settingsItems
        );
        settingsList.setAdapter(adapter);

        settingsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                copyCurrentToken();
            } else if (position == 1) {
                showAboutDialog();
            }
        });
    }

    private void copyCurrentToken() {
        if (!SharedPreferencesUtil.isLoggedIn()) {
            Toast.makeText(this, "请先登录后再获取Token", Toast.LENGTH_LONG).show();
            return;
        }

        String cookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
        if (cookie == null || cookie.isEmpty()) {
            Toast.makeText(this, "未获取到登录凭证", Toast.LENGTH_LONG).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("BiliToken", cookie);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Token 已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private void showAboutDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("确定", null);
        builder.show();
    }
}