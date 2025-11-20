package com.test.hypernotification;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * PickupCodeService
 * 修改为使用Base64直接发送图片到AI，无需上传图床
 */
public class PickupCodeService {
    private static final String TAG = "PickupCodeService";

    private Context context;
    private SharedPreferences prefs;
    private StatusCallback statusCallback;
    private ExecutorService executor;
    private OkHttpClient client;
    private Gson gson;

    // 配置参数
    private String token;

    public interface StatusCallback {
        void onStatusUpdate(String status);
    }

    /**
     * 供前台服务使用的完成回调
     */
    public interface RecognitionListener {
        void onFinished();
    }

    public PickupCodeService(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();

        loadConfiguration();
    }

    private void loadConfiguration() {
        token = prefs.getString("token", "");
    }

    public void setStatusCallback(StatusCallback callback) {
        this.statusCallback = callback;
    }

    /**
     * 兼容旧方法：不传回调
     */
    public void startRecognition() {
        startRecognition(null);
    }

    /**
     * 新方法：识别完成后会调用 listener.onFinished()
     */
    public void startRecognition(final RecognitionListener listener) {
        executor.execute(() -> {
            android.os.PowerManager powerManager = null;
            android.os.PowerManager.WakeLock wakeLock = null;

            try {
                // 获取WakeLock防止CPU休眠
                powerManager = (android.os.PowerManager) context.getSystemService(android.content.Context.POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(
                        android.os.PowerManager.PARTIAL_WAKE_LOCK,
                        "HyperNotification:RecognitionWakeLock"
                );
                wakeLock.acquire(120 * 1000L); // 120秒超时

                // 显示"识别中"焦点通知
                FocusNotificationHelper.showRecognizing(context);
                updateStatus("🔔 显示\"识别中\"焦点通知");

                RecognitionResult result = null;

                try {
                    updateStatus("🔧 开始执行识别流程");

                    // 1. 截图
                    updateStatus("📸 正在截图...");
                    String imagePath = captureScreen();
                    if (imagePath == null) {
                        updateStatus("❌ 截图失败");
                        return;
                    }
                    updateStatus("✅ 截图成功: " + imagePath);

                    // 2. 转换图片为Base64
                    updateStatus("🔄 转换图片为Base64...");
                    String base64Image = convertImageToBase64(imagePath);
                    if (base64Image == null) {
                        updateStatus("❌ 图片转换失败");
                        return;
                    }
                    updateStatus("✅ 图片转换成功");

                    // 3. AI识别
                    updateStatus("🤖 正在进行AI识别...");
                    result = recognizeWithAIBase64(base64Image);
                    if (result == null) {
                        updateStatus("❌ AI识别失败");
                        return;
                    }
                    updateStatus("✅ AI识别成功");

                    // 4. 显示结果
                    updateStatus("==========================================");
                    updateStatus("识别结果：");
                    updateStatus("  取餐码: " + result.pickupCode);
                    updateStatus("  商家: " + result.merchantName);
                    updateStatus("==========================================");

                    // 5. 发送最终结果焦点通知
                    updateStatus("🔔 发送结果焦点通知...");
                    sendFocusNotification(result);
                    updateStatus("✅ 焦点通知已发送");

                    updateStatus("✅ 流程完成");

                } catch (Exception e) {
                    Log.e(TAG, "Recognition error", e);
                    updateStatus("❌ 发生错误: " + e.getMessage());
                } finally {
                    // 无论成功失败，都取消"识别中"通知
                    FocusNotificationHelper.cancelNotification(context, FocusNotificationHelper.NOTIFICATION_ID_RECOGNIZING);
                    updateStatus("🔔 取消\"识别中\"焦点通知");
                }

            } catch (Exception e) {
                Log.e(TAG, "WakeLock error", e);
                updateStatus("❌ WakeLock错误: " + e.getMessage());
            } finally {
                // 释放WakeLock
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                    updateStatus("🔋 释放WakeLock");
                }

                // 通知调用方（前台服务）识别已结束
                if (listener != null) {
                    try {
                        listener.onFinished();
                    } catch (Exception e) {
                        Log.e(TAG, "onFinished callback error", e);
                    }
                }
            }
        });
    }

    private String captureScreen() {
        try {
            // 使用root命令截图
            String imagePath = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    + "/pickup_screen.png";

            Process process = Runtime.getRuntime().exec(new String[] {
                    "su", "-c", "screencap -p " + imagePath
            });

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                File file = new File(imagePath);
                if (file.exists() && file.length() > 0) {
                    // 保存为 ScreenCaptureService 的 lastScreenshot 以便兼容其他逻辑
                    ScreenCaptureService.setLastScreenshot(file);
                    return imagePath;
                }
            }

            // 如果root失败，尝试使用MediaProjection（ScreenCaptureService 提供）
            File screenshotFile = ScreenCaptureService.getLastScreenshot();
            if (screenshotFile != null && screenshotFile.exists()) {
                return screenshotFile.getAbsolutePath();
            }

        } catch (Exception e) {
            Log.e(TAG, "Capture screen error", e);
        }
        return null;
    }

    /**
     * 将图片文件转换为Base64字符串
     */
    private String convertImageToBase64(String imagePath) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                updateStatus("❌ 图片文件不存在: " + imagePath);
                return null;
            }

            // 读取图片文件
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 优化：先读取并压缩图片以减少Base64大小
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1; // 可以根据需要调整采样率
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            if (bitmap == null) {
                updateStatus("❌ 无法解码图片");
                fis.close();
                return null;
            }

            // 压缩图片质量
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();

            // 转换为Base64（不包含前缀）
            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // 清理资源
            bitmap.recycle();
            baos.close();
            fis.close();

            updateStatus("✅ Base64转换成功，大小: " + (base64String.length() / 1024) + " KB");
            return base64String;

        } catch (Exception e) {
            Log.e(TAG, "Convert image to base64 error", e);
            updateStatus("❌ Base64转换异常: " + e.getMessage());
        }
        return null;
    }

    /**
     * 使用Base64图片进行AI识别
     */
    private RecognitionResult recognizeWithAIBase64(String base64Image) {
        try {
            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("model", "glm-4v-flash");

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");

            JsonArray content = new JsonArray();

            // 添加文本部分
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text",
                    "请识别图片中的取餐码和商家名称。直接返回纯JSON格式：{\"pickup_code\":\"取餐码内容\",\"merchant_name\":\"商家名称\"}");
            content.add(textContent);

            // 添加图片部分（Base64格式）
            JsonObject imageContent = new JsonObject();
            imageContent.addProperty("type", "image_url");
            JsonObject imageUrlObj = new JsonObject();
            // 注意：这里使用data URI格式
            imageUrlObj.addProperty("url", "data:image/jpeg;base64," + base64Image);
            imageContent.add("image_url", imageUrlObj);
            content.add(imageContent);

            message.add("content", content);
            messages.add(message);
            requestJson.add("messages", messages);
            requestJson.addProperty("temperature", 0.1);

            RequestBody body = RequestBody.create(
                    requestJson.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://open.bigmodel.cn/api/paas/v4/chat/completions")
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                updateStatus("📝 AI原始响应:");
                updateStatus("==========================================");
                updateStatus(responseBody);
                updateStatus("==========================================");

                if (response.isSuccessful()) {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                    if (json.has("choices")) {
                        JsonArray choices = json.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            if (choice.has("message")) {
                                JsonObject msg = choice.getAsJsonObject("message");
                                if (msg.has("content")) {
                                    String aiContent = msg.get("content").getAsString();
                                    updateStatus("🤖 AI返回内容:");
                                    updateStatus(aiContent);
                                    return parseRecognitionResult(aiContent);
                                }
                            }
                        }
                    } else if (json.has("error")) {
                        JsonObject error = json.getAsJsonObject("error");
                        String errorMsg = error.has("message") ?
                                error.get("message").getAsString() : "未知错误";
                        updateStatus("❌ AI API错误: " + errorMsg);
                    }
                } else {
                    updateStatus("❌ AI API请求失败, HTTP状态码: " + response.code());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "AI recognition error", e);
            updateStatus("❌ AI识别异常: " + e.getMessage());
        }
        return null;
    }

    private RecognitionResult parseRecognitionResult(String content) {
        RecognitionResult result = new RecognitionResult();
        result.pickupCode = "未识别";
        result.merchantName = "未知商家";
        try {
            // 尝试直接解析 JSON
            String cleanedContent = content;
            if (content.contains("```json")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("```json\\s*([\\s\\S]*?)```");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    cleanedContent = matcher.group(1);
                }
            } else if (content.contains("```")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("```\\s*([\\s\\S]*?)```");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    cleanedContent = matcher.group(1);
                }
            }
            cleanedContent = cleanedContent.trim();
            int startIdx = cleanedContent.indexOf('{');
            int endIdx = cleanedContent.lastIndexOf('}');
            if (startIdx >= 0 && endIdx > startIdx) {
                cleanedContent = cleanedContent.substring(startIdx, endIdx + 1);
            }

            com.google.gson.JsonObject json = JsonParser.parseString(cleanedContent).getAsJsonObject();

            String[] codeFields = {"pickup_code", "pickupCode", "code", "取餐码", "取餐号", "number"};
            String[] merchantFields = {"merchant_name", "merchantName", "merchant", "商家", "商家名称", "店铺名称", "store"};

            for (String field : codeFields) {
                if (json.has(field) && !json.get(field).isJsonNull()) {
                    result.pickupCode = json.get(field).getAsString();
                    break;
                }
            }
            for (String field : merchantFields) {
                if (json.has(field) && !json.get(field).isJsonNull()) {
                    result.merchantName = json.get(field).getAsString();
                    break;
                }
            }
            updateStatus("✅ JSON解析成功");

        } catch (Exception e) {
            updateStatus("⚠️ JSON解析失败，尝试正则...");
            result = extractWithRegex(content);
        }
        return result;
    }

    private RecognitionResult extractWithRegex(String content) {
        RecognitionResult result = new RecognitionResult();
        result.pickupCode = "未识别";
        result.merchantName = "未知商家";
        try {
            java.util.regex.Pattern codePattern = java.util.regex.Pattern.compile(
                    "(?:pickup_code|pickupCode|code|取餐码|取餐号)[\"']?\\s*[:：]\\s*[\"']?([A-Za-z0-9\\-]+)[\"']?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher codeMatcher = codePattern.matcher(content);
            if (codeMatcher.find()) {
                result.pickupCode = codeMatcher.group(1);
                updateStatus("✅ 正则提取到取餐码: " + result.pickupCode);
            }

            java.util.regex.Pattern merchantPattern = java.util.regex.Pattern.compile(
                    "(?:merchant_name|merchantName|merchant|商家|商家名称|店铺)[\"']?\\s*[:：]\\s*[\"']?([^\"',\\}]+)[\"']?",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher merchantMatcher = merchantPattern.matcher(content);
            if (merchantMatcher.find()) {
                result.merchantName = merchantMatcher.group(1).trim();
                updateStatus("✅ 正则提取到商家名称: " + result.merchantName);
            }

            if (result.pickupCode.equals("未识别")) {
                java.util.regex.Pattern simpleCodePattern = java.util.regex.Pattern.compile("\\b([A-Z0-9]{3,10})\\b");
                java.util.regex.Matcher simpleCodeMatcher = simpleCodePattern.matcher(content.toUpperCase());
                if (simpleCodeMatcher.find()) {
                    result.pickupCode = simpleCodeMatcher.group(1);
                    updateStatus("✅ 模式匹配到可能的取餐码: " + result.pickupCode);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Regex extraction error", e);
            updateStatus("❌ 正则提取失败: " + e.getMessage());
        }
        return result;
    }

    private void sendFocusNotification(RecognitionResult result) {
        try {
            FocusNotificationHelper.sendFocusNotification(
                    context,
                    result.pickupCode,
                    result.merchantName
            );
        } catch (Exception e) {
            Log.e(TAG, "Send notification error", e);
        }
    }

    private void updateStatus(String status) {
        if (statusCallback != null) {
            statusCallback.onStatusUpdate(status);
        }
        LogManager.getInstance().addLog(status);
    }

    public static class RecognitionResult {
        public String pickupCode;
        public String merchantName;
    }
}