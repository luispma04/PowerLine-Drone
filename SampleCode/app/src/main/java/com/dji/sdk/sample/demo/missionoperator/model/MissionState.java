// ==========================================
// MissionState.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.model;

import java.util.List;

public class MissionState {

    // Mission data
    private List<InspectionPoint> inspectionPoints;
    private List<RelativePhotoPoint> photoPoints;

    // Mission progress
    private int currentStructureIndex = 0;
    private int currentPhotoIndex = 0;
    private boolean missionInProgress = false;
    private boolean missionPaused = false;

    // Mission statistics
    private int totalWaypoints = 0;
    private int completedWaypoints = 0;
    private long missionStartTime = 0;
    private long missionPauseTime = 0;

    // Mission results
    private int successfulPhotos = 0;
    private int failedPhotos = 0;

    public MissionState() {
        reset();
    }

    public void reset() {
        inspectionPoints = null;
        photoPoints = null;
        currentStructureIndex = 0;
        currentPhotoIndex = 0;
        missionInProgress = false;
        missionPaused = false;
        totalWaypoints = 0;
        completedWaypoints = 0;
        missionStartTime = 0;
        missionPauseTime = 0;
        successfulPhotos = 0;
        failedPhotos = 0;
    }

    public void startMission() {
        missionInProgress = true;
        missionPaused = false;
        missionStartTime = System.currentTimeMillis();
        currentStructureIndex = 0;
        currentPhotoIndex = 0;
        completedWaypoints = 0;
    }

    public void pauseMission() {
        missionPaused = true;
        missionPauseTime = System.currentTimeMillis();
    }

    public void resumeMission() {
        missionPaused = false;
        missionPauseTime = 0;
    }

    public void stopMission() {
        missionInProgress = false;
        missionPaused = false;
    }

    public void updateProgress(int structureIndex, int photoIndex, int completedWaypoints) {
        this.currentStructureIndex = structureIndex;
        this.currentPhotoIndex = photoIndex;
        this.completedWaypoints = completedWaypoints;
    }

    public void recordSuccessfulPhoto() {
        successfulPhotos++;
    }

    public void recordFailedPhoto() {
        failedPhotos++;
    }

    // === GETTERS AND SETTERS ===

    public List<InspectionPoint> getInspectionPoints() {
        return inspectionPoints;
    }

    public void setInspectionPoints(List<InspectionPoint> inspectionPoints) {
        this.inspectionPoints = inspectionPoints;
    }

    public List<RelativePhotoPoint> getPhotoPoints() {
        return photoPoints;
    }

    public void setPhotoPoints(List<RelativePhotoPoint> photoPoints) {
        this.photoPoints = photoPoints;
    }

    public int getCurrentStructureIndex() {
        return currentStructureIndex;
    }

    public int getCurrentPhotoIndex() {
        return currentPhotoIndex;
    }

    public boolean isMissionInProgress() {
        return missionInProgress;
    }

    public boolean isMissionPaused() {
        return missionPaused;
    }

    public int getTotalWaypoints() {
        return totalWaypoints;
    }

    public void setTotalWaypoints(int totalWaypoints) {
        this.totalWaypoints = totalWaypoints;
    }

    public int getCompletedWaypoints() {
        return completedWaypoints;
    }

    public long getMissionStartTime() {
        return missionStartTime;
    }

    public long getMissionDuration() {
        if (missionStartTime == 0) {
            return 0;
        }

        long endTime = missionInProgress ? System.currentTimeMillis() : missionPauseTime;
        return endTime - missionStartTime;
    }

    public int getSuccessfulPhotos() {
        return successfulPhotos;
    }

    public int getFailedPhotos() {
        return failedPhotos;
    }

    public int getTotalStructures() {
        return inspectionPoints != null ? inspectionPoints.size() : 0;
    }

    public int getTotalPhotoPositions() {
        return photoPoints != null ? photoPoints.size() : 0;
    }

    public int getTotalPhotos() {
        return getTotalStructures() * getTotalPhotoPositions();
    }

    public int getProgressPercentage() {
        int totalPhotos = getTotalPhotos();
        if (totalPhotos == 0) return 0;

        int completedPhotos = currentStructureIndex * getTotalPhotoPositions() + currentPhotoIndex;
        return (completedPhotos * 100) / totalPhotos;
    }

    public boolean isDataReady() {
        return inspectionPoints != null && !inspectionPoints.isEmpty() &&
                photoPoints != null && !photoPoints.isEmpty();
    }

    public String getMissionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Mission Summary:\n");
        summary.append("Structures: ").append(getTotalStructures()).append("\n");
        summary.append("Photo positions: ").append(getTotalPhotoPositions()).append("\n");
        summary.append("Total photos: ").append(getTotalPhotos()).append("\n");
        summary.append("Successful photos: ").append(successfulPhotos).append("\n");
        summary.append("Failed photos: ").append(failedPhotos).append("\n");
        summary.append("Duration: ").append(getMissionDuration() / 1000).append(" seconds\n");
        summary.append("Progress: ").append(getProgressPercentage()).append("%");
        return summary.toString();
    }
}