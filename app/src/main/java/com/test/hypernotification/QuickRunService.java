package com.test.hypernotification;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

public class QuickRunService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && "com.test.hypernotification.QUICK_RUN".equals(intent.getAction())) {
            LogManager.getInstance().addLog("[QuickRun] 外部调用触发识别");

            // 启动识别前台服务
            Intent sIntent = new Intent(this, RecognitionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(sIntent);
            } else {
                startService(sIntent);
            }
        }

        // 一次性任务，不需要常驻
        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}