// ==========================================
// PerformanceMonitor.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";

    private static final Map<String, Long> startTimes = new HashMap<>();
    private static final Map<String, Long> totalTimes = new HashMap<>();
    private static final Map<String, Integer> counters = new HashMap<>();

    public static void startTiming(String operation) {
        startTimes.put(operation, System.currentTimeMillis());
    }

    public static long endTiming(String operation) {
        Long startTime = startTimes.get(operation);
        if (startTime == null) {
            Log.w(TAG, "No start time found for operation: " + operation);
            return 0;
        }

        long duration = System.currentTimeMillis() - startTime;
        startTimes.remove(operation);

        totalTimes.put(operation, totalTimes.getOrDefault(operation, 0L) + duration);
        counters.put(operation, counters.getOrDefault(operation, 0) + 1);

        Log.d(TAG, String.format("%s completed in %d ms", operation, duration));
        return duration;
    }

    public static long getAverageTime(String operation) {
        Long totalTime = totalTimes.get(operation);
        Integer count = counters.get(operation);

        if (totalTime == null || count == null || count == 0) {
            return 0;
        }

        return totalTime / count;
    }

    public static long getTotalTime(String operation) {
        return totalTimes.getOrDefault(operation, 0L);
    }

    public static int getOperationCount(String operation) {
        return counters.getOrDefault(operation, 0);
    }

    public static String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("Performance Report:\n");
        report.append("==================\n");

        for (String operation : totalTimes.keySet()) {
            long totalTime = getTotalTime(operation);
            int count = getOperationCount(operation);
            long avgTime = getAverageTime(operation);

            report.append(String.format("%s:\n", operation));
            report.append(String.format("  Count: %d\n", count));
            report.append(String.format("  Total: %d ms\n", totalTime));
            report.append(String.format("  Average: %d ms\n", avgTime));
            report.append("\n");
        }

        return report.toString();
    }

    public static void clear() {
        startTimes.clear();
        totalTimes.clear();
        counters.clear();
    }

    public static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        Log.d(TAG, String.format("%s - Memory: %d MB / %d MB (%.1f%%)",
                context,
                usedMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                (usedMemory * 100.0) / maxMemory));
    }
}
