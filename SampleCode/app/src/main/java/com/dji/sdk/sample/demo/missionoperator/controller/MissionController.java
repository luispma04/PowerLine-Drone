package com.dji.sdk.sample.demo.missionoperator.controller;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.dji.sdk.sample.demo.missionoperator.model.InspectionPoint;
import com.dji.sdk.sample.demo.missionoperator.model.RelativePhotoPoint;
import com.dji.sdk.sample.demo.missionoperator.service.FlightService;
import com.dji.sdk.sample.demo.missionoperator.service.PhotoService;
import com.dji.sdk.sample.demo.missionoperator.utils.Constants;
import com.dji.sdk.sample.demo.missionoperator.utils.ErrorHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.util.CommonCallbacks;

public class MissionController {
    private static final String TAG = "MissionController";

    // Services
    private final FlightService flightService;
    private final PhotoService photoService;
    private final Context context;

    // Mission data
    private List<InspectionPoint> inspectionPoints;
    private List<RelativePhotoPoint> photoPoints;
    private WaypointMission currentMission;

    // Mission state
    private boolean missionInProgress = false;
    private int currentStructureIndex = 0;
    private int currentPhotoIndex = 0;

    // UI callback
    private MissionControllerCallback uiCallback;

    public interface MissionControllerCallback {
        void onStatusUpdate(String status);
        void onMissionProgress(int currentStructure, int totalStructures, int currentPhoto, int totalPhotos);
        void onMissionCompleted(boolean success, String message);
        void onPhotoReviewRequired(Bitmap photo);
        void onError(String error);
    }

    public MissionController(Context context, FlightService flightService, PhotoService photoService) {
        this.context = context;
        this.flightService = flightService;
        this.photoService = photoService;
        this.inspectionPoints = new ArrayList<>();
        this.photoPoints = new ArrayList<>();

        setupFlightServiceCallback();
    }

    public void setUiCallback(MissionControllerCallback callback) {
        this.uiCallback = callback;
    }

    private void setupFlightServiceCallback() {
        if (flightService != null) {
            flightService.setCallback(new FlightService.FlightServiceCallback() {
                @Override
                public void onMissionProgress(int currentWaypoint, int totalWaypoints) {
                    updateMissionProgress(currentWaypoint, totalWaypoints);
                }

                @Override
                public void onMissionCompleted(boolean success, String message) {
                    missionInProgress = false;
                    if (uiCallback != null) {
                        uiCallback.onMissionCompleted(success, message);
                    }
                }

                @Override
                public void onObstacleDetected(float distance, String details) {
                    if (uiCallback != null) {
                        uiCallback.onStatusUpdate("Obstacle detected at " + distance + "m");
                    }
                }

                @Override
                public void onFlightStateChanged(String state) {
                    if (uiCallback != null) {
                        uiCallback.onStatusUpdate("Flight state: " + state);
                    }
                }

                @Override
                public void onPhotoWaypointReached(int waypointIndex, int structureIndex, int photoIndex) {
                    Log.d(TAG, "📸 Photo waypoint reached: waypoint=" + waypointIndex +
                            ", structure=" + structureIndex + ", photo=" + photoIndex);

                    currentStructureIndex = structureIndex;
                    currentPhotoIndex = photoIndex;

                    // Pause mission for photo review
                    pauseMissionForPhotoReview();
                }
            });
        }
    }

    // ==========================================
    // FILE LOADING METHODS
    // ==========================================

    public void loadInspectionPoints(Uri fileUri) {
        Log.d(TAG, "Loading inspection points from: " + fileUri);

        if (!isValidCsvFile(fileUri)) {
            notifyError("Please select a valid CSV file");
            return;
        }

        try {
            List<InspectionPoint> newPoints = parseInspectionPointsCsv(fileUri);

            if (newPoints.isEmpty()) {
                notifyError("No valid inspection points found in file");
                return;
            }

            this.inspectionPoints = newPoints;

            String message = "Loaded " + inspectionPoints.size() + " structures";
            Log.d(TAG, message);
            notifyStatusUpdate(message);

            checkIfReadyToStart();

        } catch (Exception e) {
            Log.e(TAG, "Error loading inspection points", e);
            notifyError("Error loading structures: " + e.getMessage());
        }
    }

    public void loadPhotoPositions(Uri fileUri) {
        Log.d(TAG, "Loading photo positions from: " + fileUri);

        if (!isValidCsvFile(fileUri)) {
            notifyError("Please select a valid CSV file");
            return;
        }

        try {
            List<RelativePhotoPoint> newPoints = parsePhotoPositionsCsv(fileUri);

            if (newPoints.isEmpty()) {
                notifyError("No valid photo positions found in file");
                return;
            }

            this.photoPoints = newPoints;

            String message = "Loaded " + photoPoints.size() + " photo positions";
            Log.d(TAG, message);
            notifyStatusUpdate(message);

            checkIfReadyToStart();

        } catch (Exception e) {
            Log.e(TAG, "Error loading photo positions", e);
            notifyError("Error loading photo positions: " + e.getMessage());
        }
    }

    private boolean isValidCsvFile(Uri fileUri) {
        if (fileUri == null) return false;

        String fileName = getFileNameFromUri(fileUri);
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private List<InspectionPoint> parseInspectionPointsCsv(Uri fileUri) throws IOException {
        List<InspectionPoint> points = new ArrayList<>();

        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            boolean isHeader = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) continue;

                try {
                    String[] values = line.split(",");
                    if (values.length >= 4) {
                        double lat = Double.parseDouble(values[0].trim());
                        double lon = Double.parseDouble(values[1].trim());
                        float elevationDiff = Float.parseFloat(values[2].trim());
                        float height = Float.parseFloat(values[3].trim());

                        InspectionPoint point = new InspectionPoint(lat, lon, elevationDiff, height);
                        points.add(point);

                        Log.d(TAG, "Parsed structure: lat=" + lat + ", lon=" + lon +
                                ", elevation=" + elevationDiff + ", height=" + height);
                    } else {
                        Log.w(TAG, "Invalid line " + lineNumber + ": insufficient columns");
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid number format on line " + lineNumber + ": " + line);
                }
            }
        }

        Log.d(TAG, "Parsed " + points.size() + " inspection points");
        return points;
    }

    private List<RelativePhotoPoint> parsePhotoPositionsCsv(Uri fileUri) throws IOException {
        List<RelativePhotoPoint> points = new ArrayList<>();

        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            boolean isHeader = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) continue;

                try {
                    String[] values = line.split(",");
                    if (values.length >= 4) {
                        float x = Float.parseFloat(values[0].trim());
                        float y = Float.parseFloat(values[1].trim());
                        float z = Float.parseFloat(values[2].trim());
                        float pitch = Float.parseFloat(values[3].trim());

                        RelativePhotoPoint point = new RelativePhotoPoint(x, y, z, pitch);
                        points.add(point);

                        Log.d(TAG, "Parsed photo position: x=" + x + ", y=" + y +
                                ", z=" + z + ", pitch=" + pitch);
                    } else {
                        Log.w(TAG, "Invalid line " + lineNumber + ": insufficient columns");
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid number format on line " + lineNumber + ": " + line);
                }
            }
        }

        Log.d(TAG, "Parsed " + points.size() + " photo positions");
        return points;
    }

    private void checkIfReadyToStart() {
        if (!inspectionPoints.isEmpty() && !photoPoints.isEmpty()) {
            String message = "Ready to start mission: " + inspectionPoints.size() +
                    " structures, " + photoPoints.size() + " photo positions";
            notifyStatusUpdate(message);
        }
    }

    // ==========================================
    // MISSION CONTROL METHODS
    // ==========================================

    public void startMission() {
        Log.d(TAG, "Starting mission");

        if (inspectionPoints.isEmpty() || photoPoints.isEmpty()) {
            notifyError("No inspection points or photo positions loaded");
            return;
        }

        if (missionInProgress) {
            notifyError("Mission already in progress");
            return;
        }

        try {
            // Create mission using FlightService
            currentMission = flightService.createInspectionMission(inspectionPoints, photoPoints);

            if (currentMission == null) {
                notifyError("Failed to create mission");
                return;
            }

            // Execute mission
            flightService.executeMission(currentMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        missionInProgress = true;
                        currentStructureIndex = 0;
                        currentPhotoIndex = 0;
                        notifyStatusUpdate("Mission started successfully");
                        updateMissionProgress(0, currentMission.getWaypointCount());
                    } else {
                        notifyError("Failed to start mission: " + djiError.getDescription());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error starting mission", e);
            notifyError("Error starting mission: " + e.getMessage());
        }
    }

    public void pauseMission() {
        Log.d(TAG, "Pausing mission");

        if (!missionInProgress) {
            notifyError("No mission in progress");
            return;
        }

        flightService.pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    notifyStatusUpdate("Mission paused");
                } else {
                    notifyError("Failed to pause mission: " + djiError.getDescription());
                }
            }
        });
    }

    public void resumeMission() {
        Log.d(TAG, "Resuming mission");

        flightService.resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    notifyStatusUpdate("Mission resumed");
                } else {
                    notifyError("Failed to resume mission: " + djiError.getDescription());
                }
            }
        });
    }

    public void stopMission() {
        Log.d(TAG, "Stopping mission");

        flightService.stopMissionAndReturnHome(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                missionInProgress = false;
                if (djiError == null) {
                    notifyStatusUpdate("Mission stopped, returning home");
                } else {
                    notifyError("Failed to stop mission: " + djiError.getDescription());
                }
            }
        });
    }

    public void forcePhotoReview() {
        Log.d(TAG, "Force photo review requested");

        // Trigger photo service to get latest photo
        if (photoService != null) {
            photoService.getLatestPhoto(new PhotoService.PhotoCallback() {
                @Override
                public void onPhotoReceived(Bitmap photo) {
                    if (uiCallback != null && photo != null) {
                        uiCallback.onPhotoReviewRequired(photo);
                    } else {
                        notifyError("No photo available for review");
                    }
                }

                @Override
                public void onPhotoError(String error) {
                    notifyError("Failed to get photo: " + error);
                }
            });
        } else {
            notifyError("Photo service not available");
        }
    }

    public void retakePhoto() {
        Log.d(TAG, "Retaking photo");
        notifyStatusUpdate("Retaking photo...");

        // Trigger photo service to take another photo
        if (photoService != null) {
            photoService.getLatestPhoto(new PhotoService.PhotoCallback() {
                @Override
                public void onPhotoReceived(Bitmap photo) {
                    if (uiCallback != null && photo != null) {
                        uiCallback.onPhotoReviewRequired(photo);
                    } else {
                        notifyError("No photo available after retake");
                    }
                }

                @Override
                public void onPhotoError(String error) {
                    notifyError("Failed to retake photo: " + error);
                }
            });
        } else {
            notifyError("Photo service not available");
        }
    }

    public void acceptPhoto() {
        Log.d(TAG, "Photo accepted, resuming mission");
        notifyStatusUpdate("Photo accepted, continuing mission");

        // Resume mission automatically
        resumeMission();
    }

    private void pauseMissionForPhotoReview() {
        Log.d(TAG, "Pausing mission for photo review");

        flightService.pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    notifyStatusUpdate("Mission paused for photo review");

                    // Wait a moment for photo to be taken, then fetch it
                    android.os.Handler handler = new android.os.Handler();
                    handler.postDelayed(() -> {
                        fetchPhotoForReview();
                    }, 3000); // Wait 3 seconds for photo to be processed

                } else {
                    notifyError("Failed to pause mission: " + djiError.getDescription());
                }
            }
        });
    }

    private void fetchPhotoForReview() {
        Log.d(TAG, "Fetching photo for review");

        if (photoService != null) {
            photoService.getLatestPhoto(new PhotoService.PhotoCallback() {
                @Override
                public void onPhotoReceived(Bitmap photo) {
                    if (uiCallback != null && photo != null) {
                        Log.d(TAG, "Photo fetched successfully, showing review dialog");
                        uiCallback.onPhotoReviewRequired(photo);
                    } else {
                        Log.e(TAG, "Photo is null or callback not available");
                        // Auto-continue if photo fetch fails
                        notifyStatusUpdate("Photo not available, continuing mission");
                        resumeMission();
                    }
                }

                @Override
                public void onPhotoError(String error) {
                    Log.e(TAG, "Photo fetch error: " + error);
                    notifyError("Failed to get photo: " + error);
                    // Auto-continue if photo fetch fails
                    resumeMission();
                }
            });
        } else {
            Log.e(TAG, "Photo service not available");
            notifyError("Photo service not available");
            // Auto-continue if photo service not available
            resumeMission();
        }
    }

    private void updateMissionProgress(int currentWaypoint, int totalWaypoints) {
        // Calculate current structure and photo based on waypoint
        if (!photoPoints.isEmpty()) {
            int photosPerStructure = photoPoints.size();
            currentStructureIndex = currentWaypoint / (photosPerStructure + 1); // +1 for approach waypoint
            currentPhotoIndex = currentWaypoint % (photosPerStructure + 1);

            if (currentPhotoIndex > 0) {
                currentPhotoIndex--; // Adjust for approach waypoint
            }
        }

        if (uiCallback != null) {
            uiCallback.onMissionProgress(
                    Math.min(currentStructureIndex + 1, inspectionPoints.size()),
                    inspectionPoints.size(),
                    Math.min(currentPhotoIndex + 1, photoPoints.size()),
                    photoPoints.size()
            );
        }
    }

    // ==========================================
    // GETTER METHODS
    // ==========================================

    public int getTotalStructures() {
        return inspectionPoints.size();
    }

    public int getTotalPhotoPoints() {
        return photoPoints.size();
    }

    public boolean isMissionInProgress() {
        return missionInProgress;
    }

    public int getCurrentStructureIndex() {
        return currentStructureIndex;
    }

    public int getCurrentPhotoIndex() {
        return currentPhotoIndex;
    }

    public boolean isObstacleAvoidanceEnabled() {
        return flightService != null && flightService.isObstacleAvoidanceEnabled();
    }

    public float getClosestObstacleDistance() {
        return flightService != null ? flightService.getClosestObstacleDistance() : Float.MAX_VALUE;
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    private void notifyStatusUpdate(String status) {
        Log.d(TAG, "Status: " + status);
        if (uiCallback != null) {
            uiCallback.onStatusUpdate(status);
        }
    }

    private void notifyError(String error) {
        Log.e(TAG, "Error: " + error);
        if (uiCallback != null) {
            uiCallback.onError(error);
        }
    }

    public void cleanup() {
        Log.d(TAG, "Cleaning up MissionController");

        if (flightService != null) {
            flightService.cleanup();
        }

        if (photoService != null) {
            photoService.cleanup();
        }

        inspectionPoints.clear();
        photoPoints.clear();
        currentMission = null;
        missionInProgress = false;
        uiCallback = null;
    }
}