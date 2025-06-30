// ==========================================
// Constants.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

public final class Constants {
    private Constants() {}

    // GPS AND COORDINATES
    public static final double ONE_METER_OFFSET = 0.00000899322;

    // FLIGHT PARAMETERS
    public static final float DEFAULT_SPEED = 5.0f;
    public static final float DEFAULT_ALTITUDE = 0.0f;
    public static final float SAFE_DISTANCE = 2.5f;
    public static final float SAFETY_ALTITUDE = 25.0f;

    // OBSTACLE AVOIDANCE
    public static final float OBSTACLE_WARNING_DISTANCE = 10.0f;

    // PHOTO CAPTURE
    public static final int MAX_PHOTO_FETCH_RETRIES = 5;
    public static final long PHOTO_FETCH_RETRY_DELAY = 2000;
    public static final long PHOTO_REVIEW_TIMEOUT = 3000;

    // FILE OPERATIONS
    public static final int REQUEST_STRUCTURES_CSV = 1001;
    public static final int REQUEST_PHOTO_POSITIONS_CSV = 1002;

    // UI CONSTANTS
    public static final int GRID_SPAN_PORTRAIT = 2;
    public static final int GRID_SPAN_LANDSCAPE = 3;

    // LIVE STREAM
    public static final String DEFAULT_STREAM_URL = "rtmp://your-streaming-server-url.com/live/drone";
    public static final String STREAM_URL_KEY = "sp_structure_stream_url";

    // GIMBAL LIMITS
    public static final float MIN_GIMBAL_PITCH = -90f;
    public static final float MAX_GIMBAL_PITCH = 30f;

    // MISSION LIMITS
    public static final int MAX_INSPECTION_POINTS = 50;
    public static final int MAX_PHOTO_POINTS = 20;
}