// ==========================================
// ErrorHandler.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

import android.content.Context;
import android.util.Log;

import com.dji.sdk.sample.demo.missionoperator.ui.dialogs.ErrorDialog;

import dji.common.error.DJIError;

public class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    public interface ErrorCallback {
        void onErrorHandled(String errorMessage);
    }

    public static void handleDJIError(Context context, DJIError error, String operation, ErrorCallback callback) {
        if (error == null) {
            Log.d(TAG, operation + " completed successfully");
            return;
        }

        String errorMessage = getDJIErrorMessage(error, operation);
        Log.e(TAG, operation + " failed: " + errorMessage);

        ErrorDialog.showError(context, "Operation Failed", errorMessage);

        if (callback != null) {
            callback.onErrorHandled(errorMessage);
        }
    }

    public static void handleDJIErrorWithRetry(Context context, DJIError error, String operation,
                                               Runnable retryAction, ErrorCallback callback) {
        if (error == null) {
            Log.d(TAG, operation + " completed successfully");
            return;
        }

        String errorMessage = getDJIErrorMessage(error, operation);
        Log.e(TAG, operation + " failed: " + errorMessage);

        ErrorDialog.showErrorWithRetry(context, "Operation Failed",
                errorMessage + "\n\nWould you like to try again?",
                new ErrorDialog.ErrorDialogCallback() {
                    @Override
                    public void onRetry() {
                        if (retryAction != null) {
                            retryAction.run();
                        }
                    }

                    @Override
                    public void onCancel() {
                        if (callback != null) {
                            callback.onErrorHandled(errorMessage);
                        }
                    }
                });
    }

    public static void handleException(Context context, Exception exception, String operation, ErrorCallback callback) {
        String errorMessage = "Error during " + operation + ": " + exception.getMessage();
        Log.e(TAG, errorMessage, exception);

        ErrorDialog.showError(context, "Error", errorMessage);

        if (callback != null) {
            callback.onErrorHandled(errorMessage);
        }
    }

    private static String getDJIErrorMessage(DJIError error, String operation) {
        if (error == null) {
            return "Unknown error during " + operation;
        }

        String description = error.getDescription();

        switch (description.toLowerCase()) {
            case "aircraft not connected":
                return "Drone is not connected. Please check your connection and try again.";
            case "mission not ready":
                return "Mission is not ready. Please load inspection points and photo positions first.";
            case "no sd card":
                return "No SD card found in the drone. Please insert an SD card.";
            case "insufficient battery":
                return "Drone battery is too low for mission execution.";
            case "gps signal weak":
                return "GPS signal is too weak. Please wait for better signal or move to an open area.";
            case "motors not started":
                return "Drone motors are not started. Please start the motors first.";
            default:
                return operation + " failed: " + description;
        }
    }

    public static void logError(String tag, String message, Throwable throwable) {
        if (throwable != null) {
            Log.e(tag, message, throwable);
        } else {
            Log.e(tag, message);
        }
    }

    public static void logWarning(String tag, String message) {
        Log.w(tag, message);
    }

    public static void logInfo(String tag, String message) {
        Log.i(tag, message);
    }
}