package com.wcxawa.biliclient;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.wcxawa.biliclient.api.PartitionApi;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class SubReplyActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SubCommentAdapter adapter;
    private List<PartitionApi.SubComment> subReplyList = new ArrayList<>();
    private long oid;
    private long rpid;

    // 纯方角 Snackbar 工具方法
    private void showHoloSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor("#FF333333"));
        bg.setCornerRadius(0);

        snackbarView.setBackground(bg);
        snackbar.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_reply);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("回复列表");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        List<PartitionApi.SubComment>传入列表 = (List<PartitionApi.SubComment>) getIntent().getSerializableExtra("subReplies");
        if (传入列表 != null && !传入列表.isEmpty()) {
            subReplyList.addAll(传入列表);
        }
        String userName = getIntent().getStringExtra("userName");
        String content = getIntent().getStringExtra("content");
        oid = getIntent().getLongExtra("oid", 0);
        rpid = getIntent().getLongExtra("rpid", 0);

        TextView originalReplyText = findViewById(R.id.originalReplyText);
        originalReplyText.setText(userName + "：" + content);

        recyclerView = findViewById(R.id.subReplyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 修改：传入 snackbarAnchor
        adapter = new SubCommentAdapter(new ArrayList<>(), oid, this, findViewById(android.R.id.content));
        recyclerView.setAdapter(adapter);

        showHoloSnackbar("正在加载全部回复...");

        new Thread(() -> {
            List<PartitionApi.SubComment> allReplies = PartitionApi.getAllReplies(oid, rpid);

            runOnUiThread(() -> {
                if (allReplies != null && !allReplies.isEmpty()) {
                    subReplyList.clear();
                    subReplyList.addAll(allReplies);

                    // 修改：传入 snackbarAnchor
                    adapter = new SubCommentAdapter(subReplyList, oid, this, findViewById(android.R.id.content));
                    recyclerView.setAdapter(adapter);

                    showHoloSnackbar("已加载全部 " + subReplyList.size() + " 条回复");
                } else if (传入列表 != null && !传入列表.isEmpty()) {
                    subReplyList.clear();
                    subReplyList.addAll(传入列表);
                    // 修改：传入 snackbarAnchor
                    adapter = new SubCommentAdapter(subReplyList, oid, this, findViewById(android.R.id.content));
                    recyclerView.setAdapter(adapter);
                    showHoloSnackbar("加载失败，仅显示部分回复");
                } else {
                    showHoloSnackbar("暂无回复");
                }
            });
        }).start();
    }
}