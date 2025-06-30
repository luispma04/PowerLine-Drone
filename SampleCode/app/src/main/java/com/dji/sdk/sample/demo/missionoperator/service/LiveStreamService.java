// ==========================================
// LiveStreamService.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dji.sdk.sample.demo.missionoperator.utils.Constants;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;

public class LiveStreamService {
    private static final String TAG = "LiveStreamService";

    private final Context context;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private String streamUrl = Constants.DEFAULT_STREAM_URL;
    private LiveStreamManager.OnLiveChangeListener streamListener;

    private boolean isStreaming = false;
    private boolean isAudioMuted = false;
    private Timer streamDurationTimer;
    private long streamStartTime = 0;

    private LiveStreamServiceCallback callback;

    public interface LiveStreamServiceCallback {
        void onStreamStatusChanged(boolean isStreaming, String statusMessage);
        void onStreamDurationUpdate(long durationSeconds);
        void onStreamQualityUpdate(String quality);
        void onStreamError(String error);
    }

    public LiveStreamService(Context context, boolean simulatorMode) {
        this.context = context;
        loadStreamUrl();
        initializeStreamManager();
    }

    public void setCallback(LiveStreamServiceCallback callback) {
        this.callback = callback;
    }

    private void loadStreamUrl() {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        streamUrl = prefs.getString(Constants.STREAM_URL_KEY, Constants.DEFAULT_STREAM_URL);
        Log.d(TAG, "Loaded stream URL: " + streamUrl);
    }

    private void saveStreamUrl() {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.STREAM_URL_KEY, streamUrl).apply();
        Log.d(TAG, "Saved stream URL: " + streamUrl);
    }

    private void initializeStreamManager() {
        if (!isLiveStreamManagerAvailable()) {
            Log.e(TAG, "LiveStreamManager not available");
            return;
        }

        streamListener = status -> {
            String statusText;
            boolean streaming = DJISDKManager.getInstance().getLiveStreamManager().isStreaming();

            if (streaming) {
                statusText = "Streaming";
                isStreaming = true;
                startStreamDurationTimer();
            } else if (status < 0) {
                statusText = "Connection failed (Code: " + status + ")";
                isStreaming = false;
                stopStreamDurationTimer();
            } else if (status == 0) {
                statusText = "Disconnected";
                isStreaming = false;
                stopStreamDurationTimer();
            } else {
                statusText = "Connecting... (Status: " + status + ")";
            }

            notifyStatusChange(streaming, statusText);
        };

        DJISDKManager.getInstance().getLiveStreamManager().registerListener(streamListener);
    }

    public void startStream() {
        Log.d(TAG, "Starting live stream to: " + streamUrl);

        if (!isLiveStreamManagerAvailable()) {
            notifyError("Live Stream Manager not available");
            return;
        }

        if (isStreaming) {
            Log.d(TAG, "Stream already active");
            return;
        }

        new Thread(() -> {
            try {
                LiveStreamManager liveStreamManager = DJISDKManager.getInstance().getLiveStreamManager();

                liveStreamManager.setLiveUrl(streamUrl);
                liveStreamManager.setVideoEncodingEnabled(true);

                saveStreamUrl();

                int result = liveStreamManager.startStream();
                liveStreamManager.setStartTime();

                Log.d(TAG, "Stream start result: " + result);

                uiHandler.post(this::updateStreamQuality);

                if (result != 0) {
                    notifyError("Failed to start stream: " + result);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error starting stream", e);
                notifyError("Error starting stream: " + e.getMessage());
            }
        }).start();
    }

    public void stopStream() {
        Log.d(TAG, "Stopping live stream");

        if (!isLiveStreamManagerAvailable()) {
            return;
        }

        if (isStreaming) {
            DJISDKManager.getInstance().getLiveStreamManager().stopStream();
        }

        stopStreamDurationTimer();
        isStreaming = false;
        notifyStatusChange(false, "Stopped");
    }

    public void setAudioMuted(boolean muted) {
        if (!isLiveStreamManagerAvailable()) {
            return;
        }

        DJISDKManager.getInstance().getLiveStreamManager().setAudioMuted(muted);
        isAudioMuted = muted;

        Log.d(TAG, "Audio " + (muted ? "muted" : "unmuted"));
    }

    public void setStreamUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            this.streamUrl = url.trim();
            saveStreamUrl();
            Log.d(TAG, "Stream URL updated: " + streamUrl);
        }
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public boolean isAudioMuted() {
        return isAudioMuted;
    }

    private void updateStreamQuality() {
        if (!isLiveStreamManagerAvailable() || !isStreaming) {
            return;
        }

        try {
            LiveStreamManager liveStreamManager = DJISDKManager.getInstance().getLiveStreamManager();
            int bitRate = liveStreamManager.getLiveVideoBitRate();
            String qualityText = bitRate + " kbps";

            if (callback != null) {
                callback.onStreamQualityUpdate(qualityText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating stream quality", e);
        }
    }

    private void startStreamDurationTimer() {
        stopStreamDurationTimer();

        streamStartTime = System.currentTimeMillis();
        streamDurationTimer = new Timer();

        streamDurationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long durationSeconds = (System.currentTimeMillis() - streamStartTime) / 1000;

                uiHandler.post(() -> {
                    if (callback != null) {
                        callback.onStreamDurationUpdate(durationSeconds);
                    }

                    updateStreamQuality();
                });
            }
        }, 0, 1000);
    }

    private void stopStreamDurationTimer() {
        if (streamDurationTimer != null) {
            streamDurationTimer.cancel();
            streamDurationTimer = null;
        }
    }

    private boolean isLiveStreamManagerAvailable() {
        BaseProduct product = DJISampleApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            return false;
        }

        return DJISDKManager.getInstance().getLiveStreamManager() != null;
    }

    private void notifyStatusChange(boolean streaming, String message) {
        uiHandler.post(() -> {
            if (callback != null) {
                callback.onStreamStatusChanged(streaming, message);
            }
        });
    }

    private void notifyError(String error) {
        uiHandler.post(() -> {
            if (callback != null) {
                callback.onStreamError(error);
            }
        });
    }

    public void cleanup() {
        Log.d(TAG, "Cleaning up LiveStreamService");

        stopStream();

        if (isLiveStreamManagerAvailable() && streamListener != null) {
            DJISDKManager.getInstance().getLiveStreamManager().unregisterListener(streamListener);
        }

        callback = null;
        streamListener = null;
    }
}