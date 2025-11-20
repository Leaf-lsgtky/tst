package com.test.hypernotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonObject;

/**
 * FocusNotificationHelper
 * * 用于发送 MIUI 島/焦点通知。
 */
public class FocusNotificationHelper {

    private static final String TAG = "FocusNotification";

    // 通知渠道 ID 和名称
    private static final String CHANNEL_ID = "pickup_notification_channel";
    private static final String CHANNEL_NAME = "取餐码通知";

    // 【新增】识别中通知的唯一ID，避免与结果通知冲突
    public static final int NOTIFICATION_ID_RECOGNIZING = 1001; 

    /**
     * 使用 pickupCode 和 merchantName 发送焦点通知（自动生成 param_v2）
     * @param context 上下文
     * @param pickupCode 取餐码
     * @param merchantName 商家名称
     */
    public static void sendFocusNotification(Context context, String pickupCode, String merchantName) {
        // 构建默认 param_v2 JSON
        JsonObject paramV2 = new JsonObject();
        paramV2.addProperty("protocol", 1);
        paramV2.addProperty("aodTitle", "取餐码: " + pickupCode);
        paramV2.addProperty("business", "order_pending");
        paramV2.addProperty("ticker", "取餐提醒");
        paramV2.addProperty("isShowNotification", true);
        paramV2.addProperty("enableFloat", true);
        paramV2.addProperty("timeout", 60);
        paramV2.addProperty("updatable", true);

        // baseInfo
        JsonObject baseInfo = new JsonObject();
        baseInfo.addProperty("type", 1);
        baseInfo.addProperty("title", pickupCode);
        baseInfo.addProperty("content", "取餐码");
        paramV2.add("baseInfo", baseInfo);

        // hintInfo
        JsonObject hintInfo = new JsonObject();
        hintInfo.addProperty("type", 1);
        hintInfo.addProperty("title", merchantName);
        hintInfo.addProperty("content", "商家");
        paramV2.add("hintInfo", hintInfo);

        // param_island
        JsonObject paramIsland = new JsonObject();
        paramIsland.addProperty("islandProperty", 1);
        paramIsland.addProperty("islandTimeout", 900);
        paramIsland.addProperty("highlightColor", "#FFBB0F");

        JsonObject bigIslandArea = new JsonObject();

        JsonObject imageTextInfoLeft = new JsonObject();
        JsonObject textInfoLeft = new JsonObject();
        textInfoLeft.addProperty("title", "取餐码");
        textInfoLeft.addProperty("showHighlightColor", true);
        imageTextInfoLeft.add("textInfo", textInfoLeft);
        imageTextInfoLeft.addProperty("type", 1);
        bigIslandArea.add("imageTextInfoLeft", imageTextInfoLeft);

        JsonObject imageTextInfoRight = new JsonObject();
        JsonObject textInfoRight = new JsonObject();
        textInfoRight.addProperty("title", pickupCode);
        imageTextInfoRight.add("textInfo", textInfoRight);
        imageTextInfoRight.addProperty("type", 2);
        bigIslandArea.add("imageTextInfoRight", imageTextInfoRight);

        paramIsland.add("bigIslandArea", bigIslandArea);
        paramV2.add("param_island", paramIsland);

        // 调用通用发送方法 (两参数调用)
        sendFocusNotificationWithCustomParams(context, paramV2);
    }

    /**
     * 【新增】显示 "识别中" 焦点通知
     */
    public static void showRecognizing(Context context) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            createNotificationChannel(manager);

            // 构建原生通知
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(context);
            }

            // 设置基础内容
            builder.setContentTitle("识别中...")
                    .setContentText("正在使用 AI 识别取餐码...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true) // 持续显示
                    .setAutoCancel(false) // 不自动取消
                    .setShowWhen(true);

            // 构造 "识别中" 的 param_v2 JSON
            JsonObject paramV2 = buildRecognizingParamV2();
            
            // 构建 extras，MIUI 读取显示焦点通知
            Bundle bundle = new Bundle();
            // TODO: 在您的项目里，可能需要添加 "miui.focus.pics" Bundle 来提供 icon_logo
            bundle.putString("miui.focus.param", "{\"param_v2\":" + paramV2.toString() + "}");
            builder.addExtras(bundle);

            // 发送通知，使用固定的 ID
            manager.notify(NOTIFICATION_ID_RECOGNIZING, builder.build());

            Log.d(TAG, "Recognizing focus notification sent.");
        } catch (Exception e) {
            Log.e(TAG, "Send recognizing notification error", e);
        }
    }

    /**
     * 【新增】取消焦点通知
     */
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
            Log.d(TAG, "Notification cancelled: " + notificationId);
        }
    }

    /**
     * 使用自定义 param_v2 JSON 发送焦点通知
     * 【恢复为两参数】
     * @param context 上下文
     * @param paramV2 自定义 param_v2
     */
    public static void sendFocusNotificationWithCustomParams(Context context, JsonObject paramV2) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            createNotificationChannel(manager);

            // 构建原生通知
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(context);
            }

            // 设置基础内容（供普通 ROM 显示）
            String title = "取餐码";
            String content = "";
            if (paramV2.has("baseInfo") && paramV2.get("baseInfo").isJsonObject()) {
                JsonObject baseInfo = paramV2.getAsJsonObject("baseInfo");
                if (baseInfo.has("title")) title = baseInfo.get("title").getAsString();
                if (baseInfo.has("content")) content = baseInfo.get("content").getAsString();
            }

            builder.setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setAutoCancel(true)
                    .setShowWhen(true);

            // 构建 extras，MIUI 读取显示焦点通知
            Bundle bundle = new Bundle();
            bundle.putString("miui.focus.param", "{\"param_v2\":" + paramV2.toString() + "}");
            builder.addExtras(bundle);

            // 【恢复】在这里生成 ID，以确保每次结果通知都是新的 ID
            int notificationId = (int) (System.currentTimeMillis() / 1000);

            // 发送通知
            manager.notify(notificationId, builder.build());

            Log.d(TAG, "Focus notification sent, param_v2=" + paramV2.toString());

        } catch (Exception e) {
            Log.e(TAG, "Send focus notification error", e);
        }
    }

    /**
     * 【新增】构造 "识别中" 的 MIUI 焦点通知参数 (FocusParam V2)
     */
    private static JsonObject buildRecognizingParamV2() {
        JsonObject paramV2 = new JsonObject();
        paramV2.addProperty("ticker", "识别中");
        paramV2.addProperty("isShowNotification", true);
        paramV2.addProperty("enableFloat", true);
        
        // baseInfo
        JsonObject baseInfo = new JsonObject();
        baseInfo.addProperty("type", 1);
        baseInfo.addProperty("title", "识别中...");
        baseInfo.addProperty("content", "正在使用 AI 识别...");
        paramV2.add("baseInfo", baseInfo);

        // param_island
        JsonObject paramIsland = new JsonObject();
        paramIsland.addProperty("islandProperty", 1);
        
        JsonObject bigIslandArea = new JsonObject();
        
        // 左侧图标 (假设 icon_logo 在 miui.focus.pics Bundle 中)
        JsonObject imageTextInfoLeft = new JsonObject();
        JsonObject picInfo = new JsonObject();
        picInfo.addProperty("type", 1);
        picInfo.addProperty("pic", "icon_logo");
        imageTextInfoLeft.add("picInfo", picInfo);
        imageTextInfoLeft.addProperty("type", 1);
        bigIslandArea.add("imageTextInfoLeft", imageTextInfoLeft);
        
        // 右侧文字
        JsonObject imageTextInfoRight = new JsonObject();
        JsonObject textInfoRight = new JsonObject();
        textInfoRight.addProperty("title", "识别中");
        imageTextInfoRight.add("textInfo", textInfoRight);
        imageTextInfoRight.addProperty("type", 2);
        bigIslandArea.add("imageTextInfoRight", imageTextInfoRight);
        
        paramIsland.add("bigIslandArea", bigIslandArea);
        paramV2.add("param_island", paramIsland);
        
        return paramV2;
    }

    /**
     * 辅助方法：创建通知渠道
     */
    private static void createNotificationChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("用于显示取餐码的焦点通知");
                manager.createNotificationChannel(channel);
            }
        }
    }
}
