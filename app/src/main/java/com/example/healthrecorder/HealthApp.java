package com.example.healthrecorder;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HealthApp extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        // 全局未捕获异常处理器 - 写文件 + 默认处理
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            saveCrashLog(throwable);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
    }

    public static Context getAppContext() {
        return appContext;
    }

    private void saveCrashLog(Throwable throwable) {
        try {
            // 优先写到 /sdcard/（始终可写），备用 getFilesDir()
            File sdcardDir = new File("/sdcard");
            File dir = new File(sdcardDir, "HealthRecorder_crashes");
            if (!dir.exists()) dir.mkdirs();
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
            File file = new File(dir, "crash_" + ts + ".txt");
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("=== Crash Log ===");
                pw.println("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                pw.println("Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (API " + Build.VERSION.SDK_INT + ")");
                pw.println("App: HealthRecorder V1.4.3");
                pw.println();
                throwable.printStackTrace(pw);
            }
            android.util.Log.e("HealthApp", "Crash log saved to: " + file.getAbsolutePath());
        } catch (Exception e) {
            android.util.Log.e("HealthApp", "Failed to save crash log", e);
        }
    }
}
