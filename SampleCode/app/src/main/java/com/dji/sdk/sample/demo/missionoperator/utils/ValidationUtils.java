// ==========================================
// ValidationUtils.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

import com.dji.sdk.sample.demo.missionoperator.model.InspectionPoint;
import com.dji.sdk.sample.demo.missionoperator.model.RelativePhotoPoint;

import java.util.List;

public final class ValidationUtils {
    private ValidationUtils() {}

    public static ValidationResult validateInspectionPoints(List<InspectionPoint> points) {
        if (points == null || points.isEmpty()) {
            return ValidationResult.failure("No inspection points provided");
        }

        if (points.size() > Constants.MAX_INSPECTION_POINTS) {
            return ValidationResult.failure("Too many inspection points. Maximum: " + Constants.MAX_INSPECTION_POINTS);
        }

        for (int i = 0; i < points.size(); i++) {
            InspectionPoint point = points.get(i);
            if (point == null) {
                return ValidationResult.failure("Inspection point " + (i + 1) + " is null");
            }

            if (!CoordinateUtils.areCoordinatesValid(point.getLatitude(), point.getLongitude())) {
                return ValidationResult.failure("Invalid coordinates for inspection point " + (i + 1));
            }

            if (point.getStructureHeight() < 0) {
                return ValidationResult.failure("Invalid structure height for point " + (i + 1));
            }
        }

        return ValidationResult.success();
    }

    public static ValidationResult validatePhotoPoints(List<RelativePhotoPoint> points) {
        if (points == null || points.isEmpty()) {
            return ValidationResult.failure("No photo points provided");
        }

        if (points.size() > Constants.MAX_PHOTO_POINTS) {
            return ValidationResult.failure("Too many photo points. Maximum: " + Constants.MAX_PHOTO_POINTS);
        }

        for (int i = 0; i < points.size(); i++) {
            RelativePhotoPoint point = points.get(i);
            if (point == null) {
                return ValidationResult.failure("Photo point " + (i + 1) + " is null");
            }

            float pitch = point.getGimbalPitch();
            if (pitch < Constants.MIN_GIMBAL_PITCH || pitch > Constants.MAX_GIMBAL_PITCH) {
                return ValidationResult.failure("Invalid gimbal pitch for photo point " + (i + 1) +
                        ": " + pitch + " (must be between " + Constants.MIN_GIMBAL_PITCH +
                        " and " + Constants.MAX_GIMBAL_PITCH + ")");
            }
        }

        return ValidationResult.success();
    }

    public static class ValidationResult {
        private final boolean success;
        private final String errorMessage;

        private ValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}