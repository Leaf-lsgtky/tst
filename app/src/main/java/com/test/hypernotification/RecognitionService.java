package com.test.hypernotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class RecognitionService extends Service {

    private static final String TAG = "RecognitionService";
    private static final String CHANNEL_ID = "recognition_foreground_channel";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "RecognitionService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // 前台服务 — 防止后台网络被 MIUI/HyperOS 限制
        startForeground(NOTIFICATION_ID, buildNotification());

        try {
            PickupCodeService pickup = new PickupCodeService(this);

            pickup.setStatusCallback(status ->
                    LogManager.getInstance().addLog("[RecognitionService] " + status)
            );

            pickup.startRecognition(new PickupCodeService.RecognitionListener() {
                @Override
                public void onFinished() {
                    Log.d(TAG, "Recognition finished, stopping service.");
                    stopForeground(true);
                    stopSelf();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Start recognition failed", e);
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private Notification buildNotification() {
        Notification.Builder builder =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        ? new Notification.Builder(this, CHANNEL_ID)
                        : new Notification.Builder(this);

        return builder.setContentTitle("取餐码识别")
                .setContentText("正在识别中（前台服务）...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setShowWhen(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "识别前台服务",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("保持识别任务在前台运行，防止网络被系统限制");
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "RecognitionService destroyed");
    }
}