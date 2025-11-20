package com.test.hypernotification;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_SCREEN_CAPTURE = 101;

    private EditText etToken;
    private EditText etPicgoKey;
    private EditText etPicgoUrl;
    private EditText etAlbumId;
    private Button btnSave;
    private Button btnRun;
    private Button btnViewLog;
    private TextView tvStatus;
    private ScrollView scrollView;
    private Switch switchRootMode;

    private SharedPreferences prefs;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadPreferences();
        checkPermissions();

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void initViews() {
        etToken = findViewById(R.id.et_token);
        etPicgoKey = findViewById(R.id.et_picgo_key);
        etPicgoUrl = findViewById(R.id.et_picgo_url);
        etAlbumId = findViewById(R.id.et_album_id);
        btnSave = findViewById(R.id.btn_save);
        btnRun = findViewById(R.id.btn_run);
        btnViewLog = findViewById(R.id.btn_view_log);
        tvStatus = findViewById(R.id.tv_status);
        scrollView = findViewById(R.id.scroll_view);
        switchRootMode = findViewById(R.id.switch_root_mode);

        prefs = getSharedPreferences("config", MODE_PRIVATE);

        etPicgoUrl.setText("https://www.picgo.net/api/1/upload");
        switchRootMode.setChecked(prefs.getBoolean("use_root_mode", false));

        if (switchRootMode.isChecked()) {
            checkRootAccess();
        }

        switchRootMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (RootScreenshotHelper.isRootAvailable()) {
                    prefs.edit().putBoolean("use_root_mode", true).apply();
                    updateStatus("已启用 Root 模式截图");
                } else {
                    switchRootMode.setChecked(false);
                    Toast.makeText(this, "Root 权限不可用", Toast.LENGTH_SHORT).show();
                    prefs.edit().putBoolean("use_root_mode", false).apply();
                }
            } else {
                prefs.edit().putBoolean("use_root_mode", false).apply();
                updateStatus("已切换到普通模式截图");
            }
        });

        btnSave.setOnClickListener(v -> saveConfiguration());
        btnRun.setOnClickListener(v -> runPickupCodeRecognition());
        btnViewLog.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogActivity.class);
            startActivity(intent);
        });
    }

    private void checkRootAccess() {
        if (!RootScreenshotHelper.isRootAvailable()) {
            switchRootMode.setChecked(false);
            prefs.edit().putBoolean("use_root_mode", false).apply();
            Toast.makeText(this, "Root 权限不可用，已切换到普通模式", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPreferences() {
        etToken.setText(prefs.getString("token", ""));
        etPicgoKey.setText(prefs.getString("picgo_key", ""));
        etPicgoUrl.setText(prefs.getString("picgo_url", "https://www.picgo.net/api/1/upload"));
        etAlbumId.setText(prefs.getString("album_id", ""));
    }

    private void saveConfiguration() {
        String token = etToken.getText().toString().trim();
        String picgoKey = etPicgoKey.getText().toString().trim();
        String picgoUrl = etPicgoUrl.getText().toString().trim();
        String albumId = etAlbumId.getText().toString().trim();

        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(picgoKey) ||
            TextUtils.isEmpty(picgoUrl) || TextUtils.isEmpty(albumId)) {
            Toast.makeText(this, "请填写所有配置项", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("token", token);
        editor.putString("picgo_key", picgoKey);
        editor.putString("picgo_url", picgoUrl);
        editor.putString("album_id", albumId);
        editor.apply();

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        updateStatus("配置已保存");
    }

    private void runPickupCodeRecognition() {
        if (TextUtils.isEmpty(prefs.getString("token", "")) ||
            TextUtils.isEmpty(prefs.getString("picgo_key", "")) ||
            TextUtils.isEmpty(prefs.getString("album_id", ""))) {
            Toast.makeText(this, "请先保存配置", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean useRootMode = prefs.getBoolean("use_root_mode", false);

        if (useRootMode) {
            updateStatus("使用 Root 模式截图...");
            if (RootScreenshotHelper.takeScreenshot(this)) {
                updateStatus("Root 截图成功，启动前台识别服务...");
                new android.os.Handler().postDelayed(() -> startRecognitionService(), 500);
            } else {
                updateStatus("Root 截图失败");
                Toast.makeText(this, "Root 截图失败，请检查权限", Toast.LENGTH_SHORT).show();
            }
        } else {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_SCREEN_CAPTURE
            );
        }
    }

    private void startRecognitionService() {
        Intent serviceIntent = new Intent(this, RecognitionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                updateStatus("开始截屏...");

                Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                startService(serviceIntent);

                new android.os.Handler().postDelayed(() -> startRecognitionService(), 1000);
            } else {
                updateStatus("截屏权限被拒绝");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
            String message = String.format("[%s] %s\n", timestamp, status);
            tvStatus.append(message);

            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

            LogManager.getInstance().addLog(message);
        });
    }
}