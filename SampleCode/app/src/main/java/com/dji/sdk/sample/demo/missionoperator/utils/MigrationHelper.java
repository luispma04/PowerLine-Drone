// ==========================================
// MigrationHelper.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.dji.sdk.sample.demo.missionoperator.model.MissionConfiguration;
import com.dji.sdk.sample.demo.missionoperator.storage.PhotoStorageManager;

import java.io.File;

public class MigrationHelper {
    private static final String TAG = "MigrationHelper";
    private static final String MIGRATION_PREFS = "migration_status";
    private static final String KEY_MIGRATION_VERSION = "migration_version";
    private static final int CURRENT_MIGRATION_VERSION = 1;

    public static void performMigrationIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE);
        int currentVersion = prefs.getInt(KEY_MIGRATION_VERSION, 0);

        if (currentVersion < CURRENT_MIGRATION_VERSION) {
            Logger.i(TAG, "Starting migration from version " + currentVersion + " to " + CURRENT_MIGRATION_VERSION);

            try {
                performMigration(context, currentVersion);

                prefs.edit().putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION).apply();
                Logger.i(TAG, "Migration completed successfully");

            } catch (Exception e) {
                Logger.e(TAG, "Migration failed", e);
            }
        }
    }

    private static void performMigration(Context context, int fromVersion) {
        if (fromVersion < 1) {
            migrateFromMonolithicStructure(context);
        }
    }

    private static void migrateFromMonolithicStructure(Context context) {
        Logger.i(TAG, "Migrating from monolithic structure");

        migratePreferences(context);
        cleanupOldTempFiles(context);
        migratePhotoStorage(context);
    }

    private static void migratePreferences(Context context) {
        SharedPreferences oldPrefs = context.getSharedPreferences("old_mission_prefs", Context.MODE_PRIVATE);

        if (oldPrefs.getAll().isEmpty()) {
            return;
        }

        Logger.i(TAG, "Migrating preferences");

        MissionConfiguration config = new MissionConfiguration(context);

        float oldSpeed = oldPrefs.getFloat("mission_speed", Constants.DEFAULT_SPEED);
        config.setDefaultSpeed(oldSpeed);

        float oldAltitude = oldPrefs.getFloat("safety_altitude", Constants.SAFETY_ALTITUDE);
        config.setSafetyAltitude(oldAltitude);

        String oldStreamUrl = oldPrefs.getString("stream_url", Constants.DEFAULT_STREAM_URL);
        config.setLastStreamUrl(oldStreamUrl);

        config.saveConfiguration();

        oldPrefs.edit().clear().apply();

        Logger.i(TAG, "Preferences migrated successfully");
    }

    private static void cleanupOldTempFiles(Context context) {
        Logger.i(TAG, "Cleaning up old temporary files");

        File cacheDir = context.getCacheDir();
        File[] oldFiles = cacheDir.listFiles((dir, name) ->
                name.startsWith("old_mission_") || name.startsWith("temp_photo_"));

        if (oldFiles != null) {
            for (File file : oldFiles) {
                if (file.delete()) {
                    Logger.d(TAG, "Deleted old file: " + file.getName());
                }
            }
        }
    }

    private static void migratePhotoStorage(Context context) {
        Logger.i(TAG, "Checking photo storage structure");

        PhotoStorageManager manager = new PhotoStorageManager(context);
        manager.refresh();

        Logger.i(TAG, "Photo storage structure verified");
    }

    public static String getMigrationStatus(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE);
        int currentVersion = prefs.getInt(KEY_MIGRATION_VERSION, 0);

        return String.format("Migration Status:\n" +
                        "Current Version: %d\n" +
                        "Target Version: %d\n" +
                        "Status: %s",
                currentVersion,
                CURRENT_MIGRATION_VERSION,
                currentVersion >= CURRENT_MIGRATION_VERSION ? "Up to date" : "Migration needed");
    }
}