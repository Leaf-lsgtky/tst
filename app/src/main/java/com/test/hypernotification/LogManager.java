package com.test.hypernotification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 日志管理器
 */
public class LogManager {
    private static LogManager instance;
    private List<String> logs;
    private static final int MAX_LOGS = 500;

    private LogManager() {
        logs = new ArrayList<>();
    }

    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    public void addLog(String message) {
        synchronized (logs) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
            logs.add(timestamp + " - " + message);

            // 限制日志数量
            if (logs.size() > MAX_LOGS) {
                logs.remove(0);
            }
        }
    }

    public String getAllLogs() {
        synchronized (logs) {
            StringBuilder sb = new StringBuilder();
            for (int i = logs.size() - 1; i >= 0; i--) {
                sb.append(logs.get(i)).append("\n");
            }
            return sb.toString();
        }
    }

    public void clearLogs() {
        synchronized (logs) {
            logs.clear();
        }
    }
}