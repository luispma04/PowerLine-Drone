// ==========================================
//GPSCoordinate.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.model;

public class GPSCoordinate {
    private final double latitude;
    private final double longitude;
    private final float altitude;

    public GPSCoordinate(double latitude, double longitude, float altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getAltitude() { return altitude; }

    @Override
    public String toString() {
        return String.format("GPS{%.6f, %.6f, %.1fm}", latitude, longitude, altitude);
    }
}