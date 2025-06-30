package com.dji.sdk.sample.demo.missionoperator.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.dji.sdk.sample.demo.missionoperator.storage.PhotoStorageManager;

import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.media.FetchMediaTaskContent;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

public class PhotoService {
    private static final String TAG = "PhotoService";

    private final Context context;
    private final Camera camera;
    private final boolean isSimulatorMode;
    private final PhotoStorageManager photoStorageManager;

    private MediaManager mediaManager;
    private FetchMediaTaskScheduler scheduler;
    private SettingsDefinitions.StorageLocation storageLocation = SettingsDefinitions.StorageLocation.INTERNAL_STORAGE;

    public interface PhotoCallback {
        void onPhotoReceived(Bitmap photo);
        void onPhotoError(String error);
    }

    public PhotoService(Context context, Camera camera, boolean simulatorMode) {
        this.context = context;
        this.camera = camera;
        this.isSimulatorMode = simulatorMode;
        this.photoStorageManager = new PhotoStorageManager(context);

        if (!simulatorMode && camera != null) {
            setupMediaManager();
        }

        Log.d(TAG, "PhotoService initialized - simulator mode: " + simulatorMode);
    }

    private void setupMediaManager() {
        if (camera == null) {
            Log.e(TAG, "Camera is null, cannot setup media manager");
            return;
        }

        mediaManager = camera.getMediaManager();
        if (mediaManager != null) {
            scheduler = mediaManager.getScheduler();
            Log.d(TAG, "MediaManager and scheduler initialized");

            // Get storage location
            camera.getStorageLocation(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.StorageLocation>() {
                @Override
                public void onSuccess(SettingsDefinitions.StorageLocation value) {
                    storageLocation = value;
                    Log.d(TAG, "Storage location: " + value.toString());
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Log.e(TAG, "Failed to get storage location: " + djiError.getDescription());
                    storageLocation = SettingsDefinitions.StorageLocation.INTERNAL_STORAGE;
                }
            });
        } else {
            Log.e(TAG, "MediaManager is null");
        }
    }

    public void getLatestPhoto(PhotoCallback callback) {
        Log.d(TAG, "Getting latest photo - simulator mode: " + isSimulatorMode);

        if (isSimulatorMode) {
            // In simulator mode, create a dummy photo
            createSimulatorPhoto(callback);
        } else {
            // In real mode, fetch from drone camera
            fetchLatestDronePhoto(callback);
        }
    }

    private void createSimulatorPhoto(PhotoCallback callback) {
        Log.d(TAG, "Creating simulator photo");

        try {
            // Create a simple colored bitmap as a simulator photo
            Bitmap simulatorPhoto = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
            simulatorPhoto.eraseColor(android.graphics.Color.BLUE);

            // Add some text to indicate it's a simulator photo
            android.graphics.Canvas canvas = new android.graphics.Canvas(simulatorPhoto);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(android.graphics.Color.WHITE);
            paint.setTextSize(30);
            canvas.drawText("Simulator Photo", 50, 150, paint);
            canvas.drawText("Structure: " + getCurrentStructureId(), 50, 200, paint);
            canvas.drawText("Position: " + getCurrentPhotoId(), 50, 250, paint);

            if (callback != null) {
                callback.onPhotoReceived(simulatorPhoto);
            }

            Log.d(TAG, "Simulator photo created successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error creating simulator photo", e);
            if (callback != null) {
                callback.onPhotoError("Failed to create simulator photo: " + e.getMessage());
            }
        }
    }

    private void fetchLatestDronePhoto(PhotoCallback callback) {
        Log.d(TAG, "Fetching latest drone photo");

        if (mediaManager == null || scheduler == null) {
            Log.e(TAG, "MediaManager or scheduler not available");
            if (callback != null) {
                callback.onPhotoError("MediaManager not available");
            }
            return;
        }

        // Refresh file list first
        mediaManager.refreshFileListOfStorageLocation(storageLocation, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    Log.d(TAG, "File list refreshed successfully");
                    getLatestPhotoFromFileList(callback);
                } else {
                    Log.e(TAG, "Failed to refresh file list: " + djiError.getDescription());
                    if (callback != null) {
                        callback.onPhotoError("Failed to refresh file list: " + djiError.getDescription());
                    }
                }
            }
        });
    }

    private void getLatestPhotoFromFileList(PhotoCallback callback) {
        Log.d(TAG, "Getting latest photo from file list");

        List<MediaFile> mediaFiles;
        if (storageLocation == SettingsDefinitions.StorageLocation.SDCARD) {
            mediaFiles = mediaManager.getSDCardFileListSnapshot();
        } else {
            mediaFiles = mediaManager.getInternalStorageFileListSnapshot();
        }

        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            Log.d(TAG, "Found " + mediaFiles.size() + " media files");

            // Find the most recent photo
            MediaFile latestPhoto = null;
            long mostRecentTime = 0;

            for (MediaFile mediaFile : mediaFiles) {
                if (mediaFile.getMediaType() == MediaFile.MediaType.JPEG ||
                        mediaFile.getMediaType() == MediaFile.MediaType.RAW_DNG) {

                    long fileTime = mediaFile.getTimeCreated();
                    if (fileTime > mostRecentTime) {
                        latestPhoto = mediaFile;
                        mostRecentTime = fileTime;
                    }
                }
            }

            if (latestPhoto != null) {
                Log.d(TAG, "Latest photo found: " + latestPhoto.getFileName());
                fetchPhotoContent(latestPhoto, callback);
            } else {
                Log.w(TAG, "No photos found in media files");
                if (callback != null) {
                    callback.onPhotoError("No photos found");
                }
            }
        } else {
            Log.w(TAG, "Media file list is empty");
            if (callback != null) {
                callback.onPhotoError("No media files found");
            }
        }
    }

    private void fetchPhotoContent(MediaFile mediaFile, PhotoCallback callback) {
        Log.d(TAG, "Fetching content for: " + mediaFile.getFileName());

        // Try to fetch thumbnail first (faster)
        FetchMediaTask thumbnailTask = new FetchMediaTask(mediaFile, FetchMediaTaskContent.THUMBNAIL,
                new FetchMediaTask.Callback() {
                    @Override
                    public void onUpdate(MediaFile file, FetchMediaTaskContent content, DJIError error) {
                        if (error == null && content == FetchMediaTaskContent.THUMBNAIL) {
                            Bitmap thumbnail = file.getThumbnail();
                            if (thumbnail != null) {
                                Log.d(TAG, "Thumbnail fetched successfully");
                                if (callback != null) {
                                    callback.onPhotoReceived(thumbnail);
                                }
                            } else {
                                Log.w(TAG, "Thumbnail is null, trying preview");
                                fetchPhotoPreview(mediaFile, callback);
                            }
                        } else {
                            Log.e(TAG, "Thumbnail fetch failed: " + (error != null ? error.getDescription() : "Unknown"));
                            fetchPhotoPreview(mediaFile, callback);
                        }
                    }
                });

        scheduler.moveTaskToNext(thumbnailTask);
    }

    private void fetchPhotoPreview(MediaFile mediaFile, PhotoCallback callback) {
        Log.d(TAG, "Fetching preview for: " + mediaFile.getFileName());

        FetchMediaTask previewTask = new FetchMediaTask(mediaFile, FetchMediaTaskContent.PREVIEW,
                new FetchMediaTask.Callback() {
                    @Override
                    public void onUpdate(MediaFile file, FetchMediaTaskContent content, DJIError error) {
                        if (error == null && content == FetchMediaTaskContent.PREVIEW) {
                            Bitmap preview = file.getPreview();
                            if (preview != null) {
                                Log.d(TAG, "Preview fetched successfully");
                                if (callback != null) {
                                    callback.onPhotoReceived(preview);
                                }
                            } else {
                                Log.w(TAG, "Preview is also null");
                                if (callback != null) {
                                    callback.onPhotoError("Both thumbnail and preview are null");
                                }
                            }
                        } else {
                            Log.e(TAG, "Preview fetch failed: " + (error != null ? error.getDescription() : "Unknown"));
                            if (callback != null) {
                                callback.onPhotoError("Failed to fetch photo: " + (error != null ? error.getDescription() : "Unknown"));
                            }
                        }
                    }
                });

        scheduler.moveTaskToNext(previewTask);
    }

    public List<Integer> getStructureIdsWithPhotos() {
        return photoStorageManager.getStructureIdsWithPhotos();
    }

    public List<PhotoStorageManager.PhotoInfo> getPhotosForStructure(int structureId) {
        return photoStorageManager.getPhotosForStructure(structureId);
    }

    public boolean deletePhoto(PhotoStorageManager.PhotoInfo photoInfo) {
        return photoStorageManager.deletePhoto(photoInfo);
    }

    public PhotoStorageManager.PhotoInfo savePhoto(Bitmap photo, int structureId, int photoId) {
        return photoStorageManager.savePhoto(photo, structureId, photoId);
    }

    private int getCurrentStructureId() {
        // This should be set by the mission controller
        // For now, return a default value
        return 1;
    }

    private int getCurrentPhotoId() {
        // This should be set by the mission controller
        // For now, return a default value
        return 1;
    }

    public void cleanup() {
        Log.d(TAG, "Cleaning up PhotoService");

        if (scheduler != null) {
            // Suspend the scheduler instead of suspending all tasks
            scheduler.suspend(null);
        }

        // Clear any references
        mediaManager = null;
        scheduler = null;
    }
}