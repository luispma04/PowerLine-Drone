// ==========================================
// InspectionPoint.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.model;

public class InspectionPoint {
    private final double latitude;
    private final double longitude;
    private final float groundAltitude;    // Elevation difference from base level (meters)
    private final float structureHeight;   // Height of the structure (meters)

    public InspectionPoint(double latitude, double longitude, float groundAltitude, float structureHeight) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.groundAltitude = groundAltitude;
        this.structureHeight = structureHeight;
    }

    public static class Builder {
        private double latitude;
        private double longitude;
        private float groundAltitude;
        private float structureHeight;

        public Builder setLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder setAltitude(float groundAltitude) {
            this.groundAltitude = groundAltitude;
            return this;
        }

        public Builder setHeight(float structureHeight) {
            this.structureHeight = structureHeight;
            return this;
        }

        public InspectionPoint build() {
            validateData();
            return new InspectionPoint(latitude, longitude, groundAltitude, structureHeight);
        }

        private void validateData() {
            if (Math.abs(latitude) > 90) {
                throw new IllegalArgumentException("Invalid latitude: " + latitude);
            }
            if (Math.abs(longitude) > 180) {
                throw new IllegalArgumentException("Invalid longitude: " + longitude);
            }
            if (structureHeight < 0) {
                throw new IllegalArgumentException("Structure height must be positive: " + structureHeight);
            }
        }
    }

    // Getters
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getGroundAltitude() { return groundAltitude; }
    public float getStructureHeight() { return structureHeight; }

    public float getTotalInspectionAltitude(float safetyDistance) {
        return groundAltitude + structureHeight + safetyDistance;
    }

    @Override
    public String toString() {
        return String.format("InspectionPoint{lat=%.6f, lon=%.6f, groundAlt=%.1f, height=%.1f}",
                latitude, longitude, groundAltitude, structureHeight);
    }
}