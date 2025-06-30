// ==========================================
// Logger.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    private static final String TAG = "MissionOperator";
    private static final boolean ENABLE_FILE_LOGGING = true;
    private static final String LOG_FILE_NAME = "mission_log.txt";

    private static File logFile;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    public static void initialize(File logDirectory) {
        if (ENABLE_FILE_LOGGING && logDirectory != null) {
            logFile = new File(logDirectory, LOG_FILE_NAME);
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                writeToFile("Logger initialized at " + dateFormat.format(new Date()));
            } catch (IOException e) {
                Log.e(TAG, "Failed to initialize log file", e);
            }
        }
    }

    public static void d(String tag, String message) {
        Log.d(tag, message);
        writeToFile("D/" + tag + ": " + message);
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
        writeToFile("I/" + tag + ": " + message);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
        writeToFile("W/" + tag + ": " + message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        writeToFile("E/" + tag + ": " + message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        writeToFile("E/" + tag + ": " + message + " - " + throwable.toString());
    }

    public static void mission(String event, String details) {
        String message = "[MISSION] " + event + ": " + details;
        Log.i(TAG, message);
        writeToFile("I/" + TAG + ": " + message);
    }

    public static void photo(String operation, String details) {
        String message = "[PHOTO] " + operation + ": " + details;
        Log.i(TAG, message);
        writeToFile("I/" + TAG + ": " + message);
    }

    public static void flight(String operation, String details) {
        String message = "[FLIGHT] " + operation + ": " + details;
        Log.i(TAG, message);
        writeToFile("I/" + TAG + ": " + message);
    }

    public static void performance(String operation, long durationMs) {
        String message = "[PERFORMANCE] " + operation + " completed in " + durationMs + "ms";
        Log.d(TAG, message);
        writeToFile("D/" + TAG + ": " + message);
    }

    private static void writeToFile(String message) {
        if (!ENABLE_FILE_LOGGING || logFile == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(dateFormat.format(new Date()) + " " + message + "\n");
            writer.flush();
        } catch (IOException e) {
            // Don't log this error to avoid infinite loop
        }
    }

    public static File getLogFile() {
        return logFile;
    }

    public static void clearLogFile() {
        if (logFile != null && logFile.exists()) {
            try {
                new FileWriter(logFile).close();
                writeToFile("Log file cleared");
            } catch (IOException e) {
                Log.e(TAG, "Failed to clear log file", e);
            }
        }
    }
}