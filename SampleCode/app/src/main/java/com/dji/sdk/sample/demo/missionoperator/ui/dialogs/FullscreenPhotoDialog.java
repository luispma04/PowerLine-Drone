// ==========================================
// FullscreenPhotoDialog.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.missionoperator.storage.PhotoStorageManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Dialog for viewing photos in fullscreen mode with sharing and deletion options.
 */
public class FullscreenPhotoDialog {
    private static final String TAG = "FullscreenPhotoDialog";

    public interface FullscreenPhotoCallback {
        void onPhotoDeleted(PhotoStorageManager.PhotoInfo photoInfo);
        void onPhotoShared(PhotoStorageManager.PhotoInfo photoInfo);
    }

    private final Context context;
    private AlertDialog dialog;

    public FullscreenPhotoDialog(Context context) {
        this.context = context;
    }

    /**
     * Show fullscreen photo dialog
     */
    public void show(PhotoStorageManager.PhotoInfo photoInfo, FullscreenPhotoCallback callback) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View fullscreenView = LayoutInflater.from(context).inflate(R.layout.fullscreen_photo_view, null);
        builder.setView(fullscreenView);

        // Setup views
        ImageView fullscreenImage = fullscreenView.findViewById(R.id.image_fullscreen_photo);
        TextView photoInfoText = fullscreenView.findViewById(R.id.text_fullscreen_photo_info);
        Button closeButton = fullscreenView.findViewById(R.id.btn_close_fullscreen);
        Button shareButton = fullscreenView.findViewById(R.id.btn_share_photo);
        Button deleteButton = fullscreenView.findViewById(R.id.btn_delete_fullscreen);

        // Load and display photo
        if (photoInfo.getFile().exists()) {
            Bitmap photoBitmap = BitmapFactory.decodeFile(photoInfo.getFile().getAbsolutePath());
            if (photoBitmap != null) {
                fullscreenImage.setImageBitmap(photoBitmap);
            }
        }

        // Set photo info
        if (photoInfoText != null) {
            photoInfoText.setText(String.format("Structure %s | Position %s | %s",
                    photoInfo.getStructureId(),
                    photoInfo.getPhotoId(),
                    photoInfo.getTimestamp()));
        }

        // Create dialog
        dialog = builder.create();

        // Setup click listeners
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                dialog.dismiss();
                sharePhoto(photoInfo);
                if (callback != null) {
                    callback.onPhotoShared(photoInfo);
                }
            });
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteConfirmation(photoInfo, callback);
            });
        }

        dialog.show();
    }

    /**
     * Share photo using system share dialog
     */
    private void sharePhoto(PhotoStorageManager.PhotoInfo photoInfo) {
        if (photoInfo == null || !photoInfo.getFile().exists()) {
            Log.e(TAG, "Photo file not found");
            return;
        }

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");

            Uri photoUri;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                String authority = context.getPackageName() + ".fileprovider";
                photoUri = androidx.core.content.FileProvider.getUriForFile(
                        context, authority, photoInfo.getFile());
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                photoUri = Uri.fromFile(photoInfo.getFile());
            }

            shareIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    "Inspection Photo: Structure " + photoInfo.getStructureId() +
                            " Position " + photoInfo.getPhotoId());

            context.startActivity(Intent.createChooser(shareIntent, "Share Photo"));

        } catch (Exception e) {
            Log.e(TAG, "Error sharing photo", e);
            // Fallback: copy to Downloads folder
            copyToDownloads(photoInfo);
        }
    }

    /**
     * Fallback method to copy photo to Downloads folder
     */
    private void copyToDownloads(PhotoStorageManager.PhotoInfo photoInfo) {
        try {
            File downloadDir = new File(Environment.getExternalStorageDirectory(), "Download");
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File destFile = new File(downloadDir, photoInfo.getFilename());
            copyFile(photoInfo.getFile(), destFile);

            Log.d(TAG, "Photo copied to Downloads: " + destFile.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error copying to Downloads", e);
        }
    }

    /**
     * Copy file utility
     */
    private void copyFile(File source, File dest) throws Exception {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dest))) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation(PhotoStorageManager.PhotoInfo photoInfo, FullscreenPhotoCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPhotoDeleted(photoInfo);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Dismiss the dialog if showing
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}