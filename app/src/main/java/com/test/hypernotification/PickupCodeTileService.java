package com.test.hypernotification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class PickupCodeTileService extends TileService {

    private static final String TAG = "PickupCodeTile";
    private static final String PREF_TILE_DELAY = "tile_click_delay";
    private static final int DEFAULT_DELAY = 1000;

    @Override
    public void onClick() {
        super.onClick();

        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        int delay = prefs.getInt(PREF_TILE_DELAY, DEFAULT_DELAY);

        new Handler().postDelayed(() -> {
            try {
                if (isLocked()) {
                    unlockAndRun(() -> {
                        openQuickRun();
                        collapseNotificationPanel();
                    });
                } else {
                    openQuickRun();
                    collapseNotificationPanel();
                }
            } catch (Exception e) {
                Log.e(TAG, "磁贴识别异常", e);
                LogManager.getInstance().addLog("[磁贴] 异常: " + e.getMessage());
            }
        }, delay);
    }

    /** 启动快速识别服务（替换已删除的QuickRunActivity） */
    private void openQuickRun() {
        try {
            Intent serviceIntent = new Intent(this, QuickRunService.class);
            serviceIntent.setAction("com.test.hypernotification.QUICK_RUN");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            LogManager.getInstance().addLog("[磁贴] 已启动快速识别服务");
        } catch (Exception e) {
            Log.e(TAG, "启动 QuickRunService 失败", e);
            LogManager.getInstance().addLog("[磁贴] QuickRunService 启动失败: " + e.getMessage());
        }
    }

    /** 自动收起通知栏（隐藏 API） */
    @SuppressWarnings({"PrivateApi", "WrongConstant"})
    private void collapseNotificationPanel() {
        try {
            Object sbservice = getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            statusbarManager.getMethod("collapsePanels").invoke(sbservice);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("取餐码识别");
            tile.setContentDescription("点击识别取餐码");
            tile.updateTile();
        }
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("取餐码识别");
            tile.setContentDescription("点击识别取餐码");
            tile.updateTile();
        }
    }
}
