// ==========================================
// FileService.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.service;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.dji.sdk.sample.demo.missionoperator.model.InspectionPoint;
import com.dji.sdk.sample.demo.missionoperator.model.RelativePhotoPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final String TAG = "FileService";

    private final Context context;
    private FileServiceCallback callback;

    public interface FileServiceCallback {
        void onInspectionPointsLoaded(List<InspectionPoint> points);
        void onPhotoPointsLoaded(List<RelativePhotoPoint> points);
        void onFileLoadError(String error);
    }

    public FileService(Context context) {
        this.context = context;
    }

    public void setCallback(FileServiceCallback callback) {
        this.callback = callback;
    }

    public void loadInspectionPointsFromCsv(Uri fileUri) {
        Log.d(TAG, "Loading inspection points from CSV: " + fileUri);

        if (!validateCsvFile(fileUri)) {
            return;
        }

        new Thread(() -> {
            try {
                List<InspectionPoint> points = parseInspectionPointsCsv(fileUri);

                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onInspectionPointsLoaded(points);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading inspection points", e);
                notifyError("Error loading inspection points: " + e.getMessage());
            }
        }).start();
    }

    public void loadPhotoPositionsFromCsv(Uri fileUri) {
        Log.d(TAG, "Loading photo positions from CSV: " + fileUri);

        if (!validateCsvFile(fileUri)) {
            return;
        }

        new Thread(() -> {
            try {
                List<RelativePhotoPoint> points = parsePhotoPointsCsv(fileUri);

                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onPhotoPointsLoaded(points);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading photo positions", e);
                notifyError("Error loading photo positions: " + e.getMessage());
            }
        }).start();
    }

    private boolean validateCsvFile(Uri fileUri) {
        if (fileUri == null) {
            notifyError("No file selected");
            return false;
        }

        String fileName = getFileNameFromUri(fileUri);
        if (!fileName.toLowerCase().endsWith(".csv")) {
            notifyError("Please select only CSV files");
            return false;
        }

        return true;
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
                    Log.d(TAG, "Skipping header line: " + line);
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    InspectionPoint point = parseInspectionPointLine(line, lineNumber);
                    if (point != null) {
                        points.add(point);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing line " + lineNumber + ": " + line + " - " + e.getMessage());
                }
            }
        }

        Log.d(TAG, "Parsed " + points.size() + " inspection points");
        return points;
    }

    private InspectionPoint parseInspectionPointLine(String line, int lineNumber) {
        String[] values = line.split(",");

        if (values.length < 4) {
            throw new IllegalArgumentException("Line " + lineNumber + " must have at least 4 values (lat,lon,elevation_diff,height)");
        }

        try {
            double latitude = Double.parseDouble(values[0].trim());
            double longitude = Double.parseDouble(values[1].trim());
            float elevationDiff = Float.parseFloat(values[2].trim());
            float height = Float.parseFloat(values[3].trim());

            return new InspectionPoint.Builder()
                    .setLocation(latitude, longitude)
                    .setAltitude(elevationDiff)
                    .setHeight(height)
                    .build();

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Line " + lineNumber + " contains invalid numbers: " + e.getMessage());
        }
    }

    private List<RelativePhotoPoint> parsePhotoPointsCsv(Uri fileUri) throws IOException {
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
                    Log.d(TAG, "Skipping header line: " + line);
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    RelativePhotoPoint point = parsePhotoPointLine(line, lineNumber);
                    if (point != null) {
                        points.add(point);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing line " + lineNumber + ": " + line + " - " + e.getMessage());
                }
            }
        }

        Log.d(TAG, "Parsed " + points.size() + " photo points");
        return points;
    }

    private RelativePhotoPoint parsePhotoPointLine(String line, int lineNumber) {
        String[] values = line.split(",");

        if (values.length < 4) {
            throw new IllegalArgumentException("Line " + lineNumber + " must have at least 4 values (offset_x,offset_y,offset_z,gimbal_pitch)");
        }

        try {
            float offsetX = Float.parseFloat(values[0].trim());
            float offsetY = Float.parseFloat(values[1].trim());
            float offsetZ = Float.parseFloat(values[2].trim());
            float gimbalPitch = Float.parseFloat(values[3].trim());

            return new RelativePhotoPoint.Builder()
                    .setOffset(offsetX, offsetY, offsetZ)
                    .setGimbalPitch(gimbalPitch)
                    .build();

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Line " + lineNumber + " contains invalid numbers: " + e.getMessage());
        }
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
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result != null ? result : "unknown_file";
    }

    private void notifyError(String error) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onFileLoadError(error);
            }
        });
    }

    public void cleanup() {
        Log.d(TAG, "Cleaning up FileService");
        callback = null;
    }
}