package com.test.hypernotification;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 日志查看Activity
 */
public class LogActivity extends AppCompatActivity {

    private TextView tvLogs;
    private Button btnClear;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        tvLogs = findViewById(R.id.tv_logs);
        btnClear = findViewById(R.id.btn_clear);
        btnRefresh = findViewById(R.id.btn_refresh);

        tvLogs.setMovementMethod(new ScrollingMovementMethod());

        btnClear.setOnClickListener(v -> {
            LogManager.getInstance().clearLogs();
            loadLogs();
        });

        btnRefresh.setOnClickListener(v -> loadLogs());

        loadLogs();
    }

    private void loadLogs() {
        String logs = LogManager.getInstance().getAllLogs();
        tvLogs.setText(logs);
    }
}