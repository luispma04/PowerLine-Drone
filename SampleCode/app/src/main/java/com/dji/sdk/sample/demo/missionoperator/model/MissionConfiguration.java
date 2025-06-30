// ==========================================
// MissionConfiguration.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.dji.sdk.sample.demo.missionoperator.utils.Constants;

public class MissionConfiguration {
    private static final String PREFS_NAME = "mission_config";
    private static final String KEY_DEFAULT_SPEED = "default_speed";
    private static final String KEY_SAFETY_ALTITUDE = "safety_altitude";
    private static final String KEY_SAFE_DISTANCE = "safe_distance";
    private static final String KEY_SIMULATOR_MODE = "simulator_mode";
    private static final String KEY_AUTO_RESUME = "auto_resume";
    private static final String KEY_PHOTO_TIMEOUT = "photo_timeout";
    private static final String KEY_LAST_STREAM_URL = "last_stream_url";

    private final SharedPreferences prefs;

    // Configuration values
    private float defaultSpeed;
    private float safetyAltitude;
    private float safeDistance;
    private boolean simulatorMode;
    private boolean autoResumeAfterPhoto;
    private long photoTimeoutMs;
    private String lastStreamUrl;

    public MissionConfiguration(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadConfiguration();
    }

    private void loadConfiguration() {
        defaultSpeed = prefs.getFloat(KEY_DEFAULT_SPEED, Constants.DEFAULT_SPEED);
        safetyAltitude = prefs.getFloat(KEY_SAFETY_ALTITUDE, Constants.SAFETY_ALTITUDE);
        safeDistance = prefs.getFloat(KEY_SAFE_DISTANCE, Constants.SAFE_DISTANCE);
        simulatorMode = prefs.getBoolean(KEY_SIMULATOR_MODE, false);
        autoResumeAfterPhoto = prefs.getBoolean(KEY_AUTO_RESUME, true);
        photoTimeoutMs = prefs.getLong(KEY_PHOTO_TIMEOUT, Constants.PHOTO_REVIEW_TIMEOUT);
        lastStreamUrl = prefs.getString(KEY_LAST_STREAM_URL, Constants.DEFAULT_STREAM_URL);
    }

    public void saveConfiguration() {
        prefs.edit()
                .putFloat(KEY_DEFAULT_SPEED, defaultSpeed)
                .putFloat(KEY_SAFETY_ALTITUDE, safetyAltitude)
                .putFloat(KEY_SAFE_DISTANCE, safeDistance)
                .putBoolean(KEY_SIMULATOR_MODE, simulatorMode)
                .putBoolean(KEY_AUTO_RESUME, autoResumeAfterPhoto)
                .putLong(KEY_PHOTO_TIMEOUT, photoTimeoutMs)
                .putString(KEY_LAST_STREAM_URL, lastStreamUrl)
                .apply();
    }

    public void resetToDefaults() {
        defaultSpeed = Constants.DEFAULT_SPEED;
        safetyAltitude = Constants.SAFETY_ALTITUDE;
        safeDistance = Constants.SAFE_DISTANCE;
        simulatorMode = false;
        autoResumeAfterPhoto = true;
        photoTimeoutMs = Constants.PHOTO_REVIEW_TIMEOUT;
        lastStreamUrl = Constants.DEFAULT_STREAM_URL;
        saveConfiguration();
    }

    // === GETTERS AND SETTERS ===

    public float getDefaultSpeed() {
        return defaultSpeed;
    }

    public void setDefaultSpeed(float defaultSpeed) {
        this.defaultSpeed = Math.max(1.0f, Math.min(15.0f, defaultSpeed));
    }

    public float getSafetyAltitude() {
        return safetyAltitude;
    }

    public void setSafetyAltitude(float safetyAltitude) {
        this.safetyAltitude = Math.max(10.0f, Math.min(120.0f, safetyAltitude));
    }

    public float getSafeDistance() {
        return safeDistance;
    }

    public void setSafeDistance(float safeDistance) {
        this.safeDistance = Math.max(1.0f, Math.min(10.0f, safeDistance));
    }

    public boolean isSimulatorMode() {
        return simulatorMode;
    }

    public void setSimulatorMode(boolean simulatorMode) {
        this.simulatorMode = simulatorMode;
    }

    public boolean isAutoResumeAfterPhoto() {
        return autoResumeAfterPhoto;
    }

    public void setAutoResumeAfterPhoto(boolean autoResumeAfterPhoto) {
        this.autoResumeAfterPhoto = autoResumeAfterPhoto;
    }

    public long getPhotoTimeoutMs() {
        return photoTimeoutMs;
    }

    public void setPhotoTimeoutMs(long photoTimeoutMs) {
        this.photoTimeoutMs = Math.max(1000, Math.min(30000, photoTimeoutMs));
    }

    public String getLastStreamUrl() {
        return lastStreamUrl;
    }

    public void setLastStreamUrl(String lastStreamUrl) {
        this.lastStreamUrl = lastStreamUrl;
    }

    public String getConfigurationSummary() {
        return String.format(
                "Mission Configuration:\n" +
                        "Default Speed: %.1f m/s\n" +
                        "Safety Altitude: %.1f m\n" +
                        "Safe Distance: %.1f m\n" +
                        "Simulator Mode: %s\n" +
                        "Auto Resume: %s\n" +
                        "Photo Timeout: %d ms\n" +
                        "Stream URL: %s",
                defaultSpeed, safetyAltitude, safeDistance,
                simulatorMode ? "Yes" : "No",
                autoResumeAfterPhoto ? "Yes" : "No",
                photoTimeoutMs, lastStreamUrl
        );
    }
}