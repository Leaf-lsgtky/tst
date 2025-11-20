package com.test.hypernotification;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatusFragment extends Fragment implements PickupCodeService.StatusCallback {

    private TextView tvStatusText;
    private TextView tvLog;
    private NestedScrollView scrollView;
    private Button btnViewFullLog;
    private Button btnClearLog;

    private static StatusFragment instance;

    public static StatusFragment getInstance() {
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        instance = this;

        initViews(view);
        loadLogs();

        return view;
    }

    private void initViews(View view) {
        tvStatusText = view.findViewById(R.id.tv_status_text);
        tvLog = view.findViewById(R.id.tv_log);
        scrollView = view.findViewById(R.id.scroll_view);
        btnViewFullLog = view.findViewById(R.id.btn_view_full_log);
        btnClearLog = view.findViewById(R.id.btn_clear_log);

        btnViewFullLog.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LogActivity.class);
            startActivity(intent);
        });

        btnClearLog.setOnClickListener(v -> {
            LogManager.getInstance().clearLogs();
            tvLog.setText("");
            updateStatusText("日志已清空");
        });
    }

    private void loadLogs() {
        String logs = LogManager.getInstance().getAllLogs();
        if (!logs.isEmpty()) {
            tvLog.setText(logs);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    @Override
    public void onStatusUpdate(String status) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // 更新状态文本
                updateStatusText(status);

                // 添加到日志
                String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date());
                String message = String.format("[%s] %s\n", timestamp, status);
                tvLog.append(message);

                // 自动滚动到底部
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

                // 保存日志
                LogManager.getInstance().addLog(message);
            });
        }
    }

    private void updateStatusText(String status) {
        // 提取简短的状态描述
        if (status.contains("开始")) {
            tvStatusText.setText("运行中...");
        } else if (status.contains("成功")) {
            tvStatusText.setText("执行成功");
        } else if (status.contains("失败") || status.contains("错误")) {
            tvStatusText.setText("执行失败");
        } else if (status.contains("完成")) {
            tvStatusText.setText("已完成");
        } else {
            tvStatusText.setText(status);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}