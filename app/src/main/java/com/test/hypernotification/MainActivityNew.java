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
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivityNew extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_SCREEN_CAPTURE = 101;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabRun;
    private MainPagerAdapter pagerAdapter;

    private SharedPreferences prefs;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tabs);

        initViews();
        setupViewPager();
        checkPermissions();

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.viewpager);
        fabRun = findViewById(R.id.fab_run);

        prefs = getSharedPreferences("config", MODE_PRIVATE);

        fabRun.setOnClickListener(v -> runPickupCodeRecognition());
    }

    private void setupViewPager() {
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(pagerAdapter.getTabTitle(position));
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // 状态页显示 FAB，设置页隐藏 FAB
                if (position == 1) {
                    fabRun.show();
                } else {
                    fabRun.hide();
                }
            }
        });

        fabRun.hide(); // 默认在设置页，隐藏
    }

    private void runPickupCodeRecognition() {
        if (TextUtils.isEmpty(prefs.getString("token", "")) ||
            TextUtils.isEmpty(prefs.getString("picgo_key", "")) ||
            TextUtils.isEmpty(prefs.getString("album_id", ""))) {

            Toast.makeText(this, "请先在设置页面保存配置", Toast.LENGTH_SHORT).show();

            viewPager.setCurrentItem(0);
            return;
        }

        // 切到状态页
        viewPager.setCurrentItem(1);

        boolean useRootMode = prefs.getBoolean("use_root_mode", false);

        if (useRootMode) {
            updateStatus("使用 Root 模式截图...");

            if (RootScreenshotHelper.takeScreenshot(this)) {
                updateStatus("Root 截图成功，启动前台识别服务...");

                new android.os.Handler().postDelayed(
                        this::startRecognitionService,
                        500
                );

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

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_PERMISSIONS
                );
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

                new android.os.Handler().postDelayed(
                        this::startRecognitionService,
                        1000
                );

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
            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateStatus(String status) {
        StatusFragment statusFragment = StatusFragment.getInstance();

        if (statusFragment != null) {
            statusFragment.onStatusUpdate(status);
        }
    }
}