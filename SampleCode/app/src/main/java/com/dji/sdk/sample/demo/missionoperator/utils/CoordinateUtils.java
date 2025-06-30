// ==========================================
// CoordinateUtils.java
// ==========================================
package com.dji.sdk.sample.demo.missionoperator.utils;

public final class CoordinateUtils {
    private CoordinateUtils() {}

    public static float calculateHeadingToStructure(float offsetX, float offsetY) {
        double angleRadians = Math.atan2(-offsetX, -offsetY);
        float angleDegrees = (float) Math.toDegrees(angleRadians);
        float headingDegrees = 90 - angleDegrees;

        while (headingDegrees < 0) headingDegrees += 360;
        while (headingDegrees >= 360) headingDegrees -= 360;

        return headingDegrees;
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_M = 6371000;

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_M * c;
    }

    public static double coordinateOffsetToMeters(double coordOffset) {
        return coordOffset / Constants.ONE_METER_OFFSET;
    }

    public static double meterOffsetToCoordinate(double meterOffset) {
        return meterOffset * Constants.ONE_METER_OFFSET;
    }

    public static boolean areCoordinatesValid(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    public static float calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double x = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double y = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);

        double bearingRad = Math.atan2(x, y);
        double bearingDeg = Math.toDegrees(bearingRad);

        return (float) ((bearingDeg + 360) % 360);
    }
}