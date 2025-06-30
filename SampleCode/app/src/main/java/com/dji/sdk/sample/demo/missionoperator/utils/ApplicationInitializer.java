// ==========================================
// ApplicationInitializer.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

import android.content.Context;
import java.io.File;

public class ApplicationInitializer {
    private static final String TAG = "ApplicationInitializer";
    private static boolean initialized = false;

    public static void initialize(Context context) {
        if (initialized) {
            return;
        }

        Logger.i(TAG, "Initializing Mission Operator Application");

        try {
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            Logger.initialize(logDir);

            MigrationHelper.performMigrationIfNeeded(context);

            PerformanceMonitor.clear();

            Logger.i(TAG, ServiceFactory.getSystemStatus());
            Logger.i(TAG, ConnectionManager.getSystemReadinessStatus());

            initialized = true;
            Logger.i(TAG, "Mission Operator Application initialized successfully");

        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize application", e);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getInitializationStatus() {
        return "Application Initialized: " + (initialized ? "Yes" : "No");
    }
}