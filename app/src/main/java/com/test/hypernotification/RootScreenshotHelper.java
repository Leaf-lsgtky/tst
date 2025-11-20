package com.test.hypernotification;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class RootScreenshotHelper {
    private static final String TAG = "RootScreenshotHelper";

    public static boolean takeScreenshot(Context context) {
        try {
            // 使用应用的外部存储目录
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }

            File screenshotFile = new File(dir, "pickup_screen.png");

            // 使用 screencap 命令截图
            String command = "su -c screencap -p " + screenshotFile.getAbsolutePath();

            Process process = Runtime.getRuntime().exec(command);

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && screenshotFile.exists()) {
                Log.d(TAG, "Root screenshot saved: " + screenshotFile.getAbsolutePath());
                // 保存到静态变量以便后续使用
                ScreenCaptureService.setLastScreenshot(screenshotFile);
                return true;
            } else {
                Log.e(TAG, "Root screenshot failed. Exit code: " + exitCode);
                if (errorOutput.length() > 0) {
                    Log.e(TAG, "Error output: " + errorOutput.toString());
                }
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Root screenshot error", e);
            return false;
        }
    }

    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            reader.close();

            int exitCode = process.waitFor();
            return exitCode == 0 && output != null && output.contains("uid=0");
        } catch (Exception e) {
            Log.e(TAG, "Root check failed", e);
            return false;
        }
    }
}