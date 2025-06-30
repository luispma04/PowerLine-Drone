package com.dji.sdk.sample.demo.missionoperator.service;

import android.util.Log;

import com.dji.sdk.sample.demo.missionoperator.model.InspectionPoint;
import com.dji.sdk.sample.demo.missionoperator.model.RelativePhotoPoint;
import com.dji.sdk.sample.demo.missionoperator.model.GPSCoordinate;
import com.dji.sdk.sample.demo.missionoperator.utils.Constants;
import com.dji.sdk.sample.demo.missionoperator.utils.CoordinateUtils;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.gimbal.GimbalMode;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;

public class FlightService {
    private static final String TAG = "FlightService";

    private final FlightController flightController;
    private final FlightAssistant flightAssistant;
    private final Gimbal gimbal;
    private final WaypointMissionOperator missionOperator;

    private double homeLatitude = 0;
    private double homeLongitude = 0;
    private boolean obstacleAvoidanceEnabled = false;
    private float closestObstacleDistance = Float.MAX_VALUE;

    // Photo waypoint tracking
    private List<Integer> photoWaypointIndices;
    private int lastPhotoWaypointProcessed = -1;
    private int photosPerStructure = 0; // Track photos per structure

    private FlightServiceCallback callback;
    private WaypointMissionOperatorListener missionListener;

    public interface FlightServiceCallback {
        void onMissionProgress(int currentWaypoint, int totalWaypoints);
        void onMissionCompleted(boolean success, String message);
        void onObstacleDetected(float distance, String details);
        void onFlightStateChanged(String state);
        void onPhotoWaypointReached(int waypointIndex, int structureIndex, int photoIndex);
    }

    public FlightService(FlightController flightController,
                         FlightAssistant flightAssistant,
                         Gimbal gimbal) {
        this.flightController = flightController;
        this.flightAssistant = flightAssistant;
        this.gimbal = gimbal;
        this.missionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        this.photoWaypointIndices = new ArrayList<>();

        initializeFlightSystems();
        setupMissionListener();
    }

    public void setCallback(FlightServiceCallback callback) {
        this.callback = callback;
    }

    private void initializeFlightSystems() {
        Log.d(TAG, "Initializing flight systems");

        setupFlightController();
        setupObstacleAvoidance();
        setupGimbal();
    }

    private void setupFlightController() {
        if (flightController == null) {
            Log.e(TAG, "FlightController is null");
            return;
        }

        flightController.setStateCallback(flightControllerState -> {
            homeLatitude = flightControllerState.getHomeLocation().getLatitude();
            homeLongitude = flightControllerState.getHomeLocation().getLongitude();

            if (callback != null) {
                String state = flightControllerState.getFlightMode().toString();
                callback.onFlightStateChanged(state);
            }
        });
    }

    private void setupObstacleAvoidance() {
        if (flightAssistant == null) {
            Log.e(TAG, "FlightAssistant is null");
            return;
        }

        enableObstacleAvoidance(true);

        flightAssistant.setVisionDetectionStateUpdatedCallback(this::handleObstacleDetection);
    }

    private void handleObstacleDetection(VisionDetectionState visionDetectionState) {
        ObstacleDetectionSector[] sectors = visionDetectionState.getDetectionSectors();

        StringBuilder obstacleInfo = new StringBuilder();
        closestObstacleDistance = Float.MAX_VALUE;

        for (int i = 0; i < sectors.length; i++) {
            ObstacleDetectionSector sector = sectors[i];
            float distance = sector.getObstacleDistanceInMeters();

            if (distance < closestObstacleDistance && distance > 0) {
                closestObstacleDistance = distance;
            }

            if (distance < Constants.OBSTACLE_WARNING_DISTANCE && distance > 0) {
                obstacleInfo.append("Sector ").append(i + 1)
                        .append(": ").append(String.format("%.2f", distance))
                        .append("m (").append(sector.getWarningLevel().toString()).append(")\n");
            }
        }

        if (callback != null && closestObstacleDistance < Constants.OBSTACLE_WARNING_DISTANCE) {
            callback.onObstacleDetected(closestObstacleDistance, obstacleInfo.toString());
        }
    }

    public void enableObstacleAvoidance(boolean enable) {
        if (flightAssistant == null) return;

        Log.d(TAG, "Setting obstacle avoidance: " + enable);

        flightAssistant.setCollisionAvoidanceEnabled(enable, result -> {
            if (result == null) {
                Log.d(TAG, "Collision avoidance " + (enable ? "enabled" : "disabled"));
            } else {
                Log.e(TAG, "Failed to set collision avoidance: " + result.getDescription());
            }
        });

        flightAssistant.setUpwardVisionObstacleAvoidanceEnabled(enable, result -> {
            if (result == null) {
                Log.d(TAG, "Upward vision obstacle avoidance " + (enable ? "enabled" : "disabled"));
            } else {
                Log.e(TAG, "Failed to set upward vision obstacle avoidance: " + result.getDescription());
            }
        });

        obstacleAvoidanceEnabled = enable;
    }

    private void setupGimbal() {
        if (gimbal == null) {
            Log.e(TAG, "Gimbal is null");
            return;
        }

        Log.d(TAG, "Setting up gimbal for independent control");

        gimbal.setMode(GimbalMode.YAW_FOLLOW, result -> {
            if (result == null) {
                Log.d(TAG, "Gimbal set to YAW_FOLLOW mode successfully");
            } else {
                Log.e(TAG, "Failed to set gimbal mode: " + result.getDescription());
                tryFreeMode();
            }
        });
    }

    private void tryFreeMode() {
        if (gimbal == null) return;

        gimbal.setMode(GimbalMode.FREE, result -> {
            if (result == null) {
                Log.d(TAG, "Gimbal set to FREE mode successfully");
            } else {
                Log.e(TAG, "Failed to set FREE mode: " + result.getDescription());
            }
        });
    }

    private void setupMissionListener() {
        missionListener = new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(WaypointMissionDownloadEvent event) {
                // Not used
            }

            @Override
            public void onUploadUpdate(WaypointMissionUploadEvent event) {
                // Not used in this basic implementation
            }

            @Override
            public void onExecutionUpdate(WaypointMissionExecutionEvent event) {
                if (event.getProgress() != null) {
                    final int currentWaypointIndex = event.getProgress().targetWaypointIndex;
                    final int totalWaypoints = event.getProgress().totalWaypointCount;

                    Log.d(TAG, "Mission progress: waypoint " + currentWaypointIndex + "/" + totalWaypoints);

                    if (callback != null) {
                        callback.onMissionProgress(currentWaypointIndex, totalWaypoints);
                    }

                    // Check if this is a photo waypoint
                    boolean isPhotoWaypoint = photoWaypointIndices.contains(currentWaypointIndex);
                    boolean waypointReached = event.getProgress().isWaypointReached;
                    WaypointMissionState currentState = event.getCurrentState();

                    Log.d(TAG, "Waypoint " + currentWaypointIndex + " - isPhoto: " + isPhotoWaypoint +
                            ", reached: " + waypointReached + ", state: " + currentState);

                    if (isPhotoWaypoint && waypointReached &&
                            currentState == WaypointMissionState.EXECUTING &&
                            currentWaypointIndex != lastPhotoWaypointProcessed) {

                        Log.d(TAG, "🔥 PHOTO WAYPOINT REACHED: " + currentWaypointIndex);
                        lastPhotoWaypointProcessed = currentWaypointIndex;

                        // Calculate structure and photo indices
                        int photoWaypointPosition = photoWaypointIndices.indexOf(currentWaypointIndex);
                        int photosPerStructure = getPhotosPerStructure();
                        int structureIndex = photoWaypointPosition / photosPerStructure;
                        int photoIndex = photoWaypointPosition % photosPerStructure;

                        if (callback != null) {
                            callback.onPhotoWaypointReached(currentWaypointIndex, structureIndex, photoIndex);
                        }
                    }
                }
            }

            @Override
            public void onExecutionStart() {
                Log.d(TAG, "Mission execution started");
            }

            @Override
            public void onExecutionFinish(DJIError error) {
                Log.d(TAG, "Mission execution finished");
                if (callback != null) {
                    callback.onMissionCompleted(error == null,
                            error == null ? "Mission completed successfully" : error.getDescription());
                }
            }
        };

        if (missionOperator != null) {
            missionOperator.addListener(missionListener);
        }
    }

    public WaypointMission createInspectionMission(List<InspectionPoint> inspectionPoints,
                                                   List<RelativePhotoPoint> photoPoints) {
        Log.d(TAG, "Creating inspection mission with " + inspectionPoints.size() +
                " structures and " + photoPoints.size() + " photo positions");

        WaypointMission.Builder builder = new WaypointMission.Builder();

        builder.autoFlightSpeed(Constants.DEFAULT_SPEED);
        builder.maxFlightSpeed(Constants.DEFAULT_SPEED * 2);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
        builder.setGimbalPitchRotationEnabled(true);

        photoWaypointIndices.clear();
        photosPerStructure = photoPoints.size(); // Set photos per structure
        int waypointCount = 0;

        // Add initial safety waypoint
        addSafetyWaypoint(builder, homeLatitude, homeLongitude, Constants.SAFETY_ALTITUDE);
        waypointCount++;

        // Add waypoints for each structure
        for (int i = 0; i < inspectionPoints.size(); i++) {
            InspectionPoint point = inspectionPoints.get(i);

            // Add approach waypoint
            waypointCount += addStructureApproachWaypoint(builder, point);

            // Add photo waypoints
            for (int j = 0; j < photoPoints.size(); j++) {
                RelativePhotoPoint photoPoint = photoPoints.get(j);
                photoWaypointIndices.add(waypointCount);
                waypointCount += addPhotoWaypoint(builder, point, photoPoint);
            }

            // Add safety waypoints between structures
            if (i < inspectionPoints.size() - 1) {
                InspectionPoint nextPoint = inspectionPoints.get(i + 1);
                waypointCount += addInterStructureSafetyWaypoints(builder, point, nextPoint);
            }
        }

        Log.d(TAG, "Created mission with " + waypointCount + " waypoints, " +
                photoWaypointIndices.size() + " photo waypoints, " +
                photosPerStructure + " photos per structure");

        return builder.build();
    }

    private void addSafetyWaypoint(WaypointMission.Builder builder, double lat, double lon, float altitude) {
        Waypoint waypoint = new Waypoint(lat, lon, altitude);
        waypoint.heading = 0;
        builder.addWaypoint(waypoint);
    }

    private int addStructureApproachWaypoint(WaypointMission.Builder builder, InspectionPoint point) {
        float safeAltitude = point.getTotalInspectionAltitude(Constants.SAFE_DISTANCE);

        Waypoint approachWaypoint = new Waypoint(
                point.getLatitude(),
                point.getLongitude(),
                safeAltitude
        );
        approachWaypoint.heading = 0;
        builder.addWaypoint(approachWaypoint);

        return 1;
    }

    private int addPhotoWaypoint(WaypointMission.Builder builder,
                                 InspectionPoint structure,
                                 RelativePhotoPoint photoPoint) {

        GPSCoordinate photoCoord = photoPoint.calculateAbsolutePosition(structure, Constants.ONE_METER_OFFSET);

        Waypoint waypoint = new Waypoint(
                photoCoord.getLatitude(),
                photoCoord.getLongitude(),
                photoCoord.getAltitude()
        );

        // Calculate heading to point toward structure
        float heading = CoordinateUtils.calculateHeadingToStructure(
                photoPoint.getOffsetX(), photoPoint.getOffsetY()
        );
        waypoint.heading = (int) heading;

        // Add gimbal and photo actions
        int gimbalPitch = Math.round(photoPoint.getGimbalPitch());
        waypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, gimbalPitch));
        waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));

        builder.addWaypoint(waypoint);
        return 1;
    }

    private int addInterStructureSafetyWaypoints(WaypointMission.Builder builder,
                                                 InspectionPoint currentPoint,
                                                 InspectionPoint nextPoint) {
        // Safety waypoint at current position
        addSafetyWaypoint(builder,
                currentPoint.getLatitude(),
                currentPoint.getLongitude(),
                Constants.SAFETY_ALTITUDE + currentPoint.getGroundAltitude());

        // Safety waypoint at next position
        addSafetyWaypoint(builder,
                nextPoint.getLatitude(),
                nextPoint.getLongitude(),
                Constants.SAFETY_ALTITUDE + nextPoint.getGroundAltitude());

        return 2;
    }

    private int getPhotosPerStructure() {
        return photosPerStructure;
    }

    public void executeMission(WaypointMission mission, CommonCallbacks.CompletionCallback callback) {
        if (missionOperator == null) {
            Log.e(TAG, "Mission operator not available");
            return;
        }

        DJIError loadError = missionOperator.loadMission(mission);
        if (loadError != null) {
            callback.onResult(loadError);
            return;
        }

        missionOperator.uploadMission(uploadResult -> {
            if (uploadResult != null) {
                callback.onResult(uploadResult);
                return;
            }

            missionOperator.startMission(callback);
        });
    }

    public void pauseMission(CommonCallbacks.CompletionCallback callback) {
        if (missionOperator != null) {
            missionOperator.pauseMission(callback);
        } else {
            Log.e(TAG, "Mission operator not available");
        }
    }

    public void resumeMission(CommonCallbacks.CompletionCallback callback) {
        if (missionOperator != null) {
            missionOperator.resumeMission(callback);
        } else {
            Log.e(TAG, "Mission operator not available");
        }
    }

    public void stopMissionAndReturnHome(CommonCallbacks.CompletionCallback callback) {
        if (missionOperator != null) {
            missionOperator.stopMission(stopResult -> {
                if (stopResult != null) {
                    callback.onResult(stopResult);
                    return;
                }

                returnToHome(callback);
            });
        } else {
            Log.e(TAG, "Mission operator not available");
        }
    }

    private void returnToHome(CommonCallbacks.CompletionCallback callback) {
        if (flightController == null) {
            Log.e(TAG, "Flight controller not available");
            return;
        }

        flightController.setGoHomeHeightInMeters((int) Constants.SAFETY_ALTITUDE, altitudeResult -> {
            if (altitudeResult == null) {
                Log.d(TAG, "RTH altitude set to: " + Constants.SAFETY_ALTITUDE + "m");
            } else {
                Log.w(TAG, "Failed to set RTH altitude: " + altitudeResult.getDescription());
            }

            flightController.startGoHome(callback);
        });
    }

    public boolean isObstacleAvoidanceEnabled() {
        return obstacleAvoidanceEnabled;
    }

    public float getClosestObstacleDistance() {
        return closestObstacleDistance;
    }

    public void cleanup() {
        Log.d(TAG, "Cleaning up FlightService");

        if (missionOperator != null && missionListener != null) {
            missionOperator.removeListener(missionListener);
        }

        if (flightAssistant != null) {
            flightAssistant.setVisionDetectionStateUpdatedCallback(null);
        }

        if (flightController != null) {
            flightController.setStateCallback(null);
        }

        callback = null;
    }
}