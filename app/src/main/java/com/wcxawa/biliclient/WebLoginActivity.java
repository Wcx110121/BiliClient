package com.wcxawa.biliclient;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.wcxawa.biliclient.util.NetWorkUtil;
import com.wcxawa.biliclient.util.SharedPreferencesUtil;

public class WebLoginActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private boolean loginCompleted = false;  // 防止重复保存

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(ProgressBar.GONE);

                // 延迟检查 Cookie，确保 Cookie 已经写入
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    checkAndSaveCookies(url);
                }, 500);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 拦截跳转，检查是否登录成功
                checkAndSaveCookies(url);
                return false;  // 继续加载
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
            }
        });

        // 加载 B 站登录页面
        webView.loadUrl("https://passport.bilibili.com/login");
    }

    private void checkAndSaveCookies(String url) {
        if (loginCompleted) return;

        // 获取 Cookie
        String cookies = CookieManager.getInstance().getCookie("https://www.bilibili.com");

        // 检查是否包含登录凭证
        if (cookies != null && cookies.contains("SESSDATA=") && cookies.contains("bili_jct=")) {
            loginCompleted = true;

            // 保存 Cookie
            SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, cookies);
            NetWorkUtil.refreshHeaders();

            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();

            // 设置返回结果，告诉 MainActivity 登录成功了
            setResult(RESULT_OK);

            // 延迟关闭，让 Toast 显示出来
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finish();
            }, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}