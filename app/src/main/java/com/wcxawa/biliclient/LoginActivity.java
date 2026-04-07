package com.wcxawa.biliretro;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.wcxawa.biliretro.api.LoginApi;
import com.wcxawa.biliretro.util.NetWorkUtil;
import com.wcxawa.biliretro.util.SharedPreferencesUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class LoginActivity extends AppCompatActivity {

    private ImageView qrcodeImage;
    private TextView statusText;
    private Button refreshButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private LoginApi.QRInfo currentQRInfo;
    private boolean isPolling = false;
    private Runnable pollingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        qrcodeImage = findViewById(R.id.qrcodeImage);
        statusText = findViewById(R.id.statusText);
        refreshButton = findViewById(R.id.refreshButton);

        refreshButton.setOnClickListener(v -> {
            stopPolling();
            generateQRCode();
        });

        generateQRCode();
    }

    private void generateQRCode() {
        statusText.setText("正在获取二维码...");
        refreshButton.setVisibility(Button.GONE);

        new Thread(() -> {
            LoginApi.QRInfo info = LoginApi.getQRInfo();
            runOnUiThread(() -> {
                if (info != null && info.qrcodeUrl != null) {
                    currentQRInfo = info;
                    showQRCode(info.qrcodeUrl);
                    statusText.setText("请使用哔哩哔哩APP扫码");
                    startPolling();
                } else {
                    statusText.setText("获取二维码失败，请重试");
                    refreshButton.setVisibility(Button.VISIBLE);
                }
            });
        }).start();
    }

    private void showQRCode(String url) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(matrix);
            qrcodeImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            statusText.setText("生成二维码失败");
        }
    }

    private void startPolling() {
        if (isPolling) return;
        isPolling = true;

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling || currentQRInfo == null) return;

                new Thread(() -> {
                    int code = LoginApi.pollLoginStatus(currentQRInfo.qrcodeKey);

                    runOnUiThread(() -> {
                        if (!isPolling) return;

                        // B站返回码说明：
                        // 0    - 扫码成功（已登录确认）
                        // 86038 - 二维码已过期
                        // 86039 - 未扫码
                        // 86101 - 已扫码但未确认

                        if (code == 0) {
                            // 真正的登录成功
                            isPolling = false;
                            statusText.setText("登录成功！");
                            Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();

                            // 刷新网络请求头
                            NetWorkUtil.refreshHeaders();

                            // 延迟关闭
                            handler.postDelayed(() -> {
                                setResult(RESULT_OK);
                                finish();
                            }, 1000);

                        } else if (code == 86038) {
                            // 二维码过期
                            isPolling = false;
                            statusText.setText("二维码已过期，请刷新");
                            refreshButton.setVisibility(Button.VISIBLE);

                        } else if (code == 86039) {
                            // 未扫码，继续轮询
                            statusText.setText("等待扫码...");
                            handler.postDelayed(this, 2000);

                        } else if (code == 86101) {
                            // 已扫码但未确认
                            statusText.setText("扫码成功，请在手机上确认登录");
                            handler.postDelayed(this, 2000);

                        } else {
                            // 其他错误
                            statusText.setText("等待中...");
                            handler.postDelayed(this, 3000);
                        }
                    });
                }).start();
            }
        };

        handler.post(pollingRunnable);
    }

    private void stopPolling() {
        isPolling = false;
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }
}