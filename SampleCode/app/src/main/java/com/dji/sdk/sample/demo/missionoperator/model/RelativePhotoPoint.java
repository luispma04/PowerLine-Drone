// ==========================================
// RelativePhotoPoint.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.model;

import com.dji.sdk.sample.demo.missionoperator.utils.Constants;

public class RelativePhotoPoint {
    private final float offsetX;      // meters east (+) / west (-)
    private final float offsetY;      // meters north (+) / south (-)
    private final float offsetZ;      // meters up (+) / down (-)
    private final float gimbalPitch;  // degrees (0 = level, -90 = straight down)

    public RelativePhotoPoint(float offsetX, float offsetY, float offsetZ, float gimbalPitch) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.gimbalPitch = gimbalPitch;
    }

    public static class Builder {
        private float offsetX = 0f;
        private float offsetY = 0f;
        private float offsetZ = 0f;
        private float gimbalPitch = 0f;

        public Builder setOffset(float x, float y, float z) {
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            return this;
        }

        public Builder setGimbalPitch(float pitch) {
            if (pitch < -90 || pitch > 30) {
                throw new IllegalArgumentException("Gimbal pitch must be between -90 and +30 degrees");
            }
            this.gimbalPitch = pitch;
            return this;
        }

        public RelativePhotoPoint build() {
            return new RelativePhotoPoint(offsetX, offsetY, offsetZ, gimbalPitch);
        }
    }

    // Getters
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public float getGimbalPitch() { return gimbalPitch; }

    public GPSCoordinate calculateAbsolutePosition(InspectionPoint reference, double meterOffset) {
        double newLat = reference.getLatitude() + (offsetY * meterOffset);
        double newLon = reference.getLongitude() + (offsetX * meterOffset);
        float newAlt = reference.getGroundAltitude() + reference.getStructureHeight() + offsetZ;

        return new GPSCoordinate(newLat, newLon, newAlt);
    }

    @Override
    public String toString() {
        return String.format("RelativePhotoPoint{x=%.1f, y=%.1f, z=%.1f, pitch=%.1f°}",
                offsetX, offsetY, offsetZ, gimbalPitch);
    }
}