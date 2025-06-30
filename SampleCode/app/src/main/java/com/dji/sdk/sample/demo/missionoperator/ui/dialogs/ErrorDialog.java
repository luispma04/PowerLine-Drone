// ==========================================
// ErrorDialog.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Utility class for showing error dialogs with consistent styling.
 */
public class ErrorDialog {

    public interface ErrorDialogCallback {
        void onRetry();
        void onCancel();
    }

    /**
     * Show a simple error message
     */
    public static void showError(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Show error with retry option
     */
    public static void showErrorWithRetry(Context context, String title, String message, ErrorDialogCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (callback != null) {
                        callback.onRetry();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (callback != null) {
                        callback.onCancel();
                    }
                })
                .show();
    }

    /**
     * Show critical error that requires user action
     */
    public static void showCriticalError(Context context, String title, String message, Runnable onOk) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (onOk != null) {
                        onOk.run();
                    }
                })
                .setCancelable(false)
                .show();
    }
}