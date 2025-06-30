// ==========================================
// PhotoConfirmationDialog.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dji.sdk.sample.R;

/**
 * Dialog for photo confirmation during mission execution.
 * Extracted from the main view to improve organization.
 */
public class PhotoConfirmationDialog {
    private static final String TAG = "PhotoConfirmationDialog";

    public interface PhotoConfirmationCallback {
        void onPhotoAccepted();
        void onPhotoRetake();
    }

    private final Context context;
    private AlertDialog dialog;

    public PhotoConfirmationDialog(Context context) {
        this.context = context;
    }

    /**
     * Show photo confirmation dialog
     */
    public void show(Bitmap photo, String photoDetails, PhotoConfirmationCallback callback) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Choose layout based on orientation
        int orientation = context.getResources().getConfiguration().orientation;
        View dialogView;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_photo_confirmation_land, null);
        } else {
            dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_photo_confirmation, null);
        }

        builder.setView(dialogView);

        // Setup views
        ImageView photoImageView = dialogView.findViewById(R.id.popup_image_preview);
        TextView photoDetailsText = dialogView.findViewById(R.id.text_photo_details);
        Button retakeButton = dialogView.findViewById(R.id.btn_popup_retake);
        Button acceptButton = dialogView.findViewById(R.id.btn_popup_accept);

        // Set content
        if (photo != null) {
            photoImageView.setImageBitmap(photo);
        }

        if (photoDetailsText != null) {
            photoDetailsText.setText(photoDetails);
        }

        // Create dialog
        dialog = builder.create();
        dialog.setCancelable(false);

        // Setup click listeners
        if (retakeButton != null) {
            retakeButton.setOnClickListener(v -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onPhotoRetake();
                }
            });
        }

        if (acceptButton != null) {
            acceptButton.setOnClickListener(v -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onPhotoAccepted();
                }
            });
        }

        dialog.show();
    }

    /**
     * Dismiss the dialog if showing
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * Check if dialog is currently showing
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}