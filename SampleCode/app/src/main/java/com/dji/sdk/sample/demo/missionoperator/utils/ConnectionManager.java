package com.dji.sdk.sample.demo.missionoperator.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;

public class ConnectionManager {
    private static final String TAG = "ConnectionManager";

    private static int lastKnownBatteryPercentage = -1;

    public interface ConnectionCallback {
        void onConnectionStatusChanged(boolean connected, String status);
        void onProductModelChanged(String model);
        void onBatteryStatusChanged(int percentage);
    }

    public static boolean isDroneConnected() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        return product != null && product.isConnected();
    }

    public static String getDroneModel() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product != null && product.getModel() != null) {
            return product.getModel().getDisplayName();
        }
        return "Unknown";
    }

    public static int getBatteryPercentage() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            if (aircraft.getBattery() != null) {
                // Set up battery state callback to get current percentage
                aircraft.getBattery().setStateCallback(batteryState -> {
                    if (batteryState != null) {
                        lastKnownBatteryPercentage = batteryState.getChargeRemainingInPercent();
                    }
                });

                // Return last known value if available
                return lastKnownBatteryPercentage;
            }
        }
        return -1;
    }

    /**
     * Get battery percentage with callback for real-time updates
     */
    public static void getBatteryPercentage(BatteryCallback callback) {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            if (aircraft.getBattery() != null) {
                aircraft.getBattery().setStateCallback(batteryState -> {
                    if (batteryState != null && callback != null) {
                        int percentage = batteryState.getChargeRemainingInPercent();
                        lastKnownBatteryPercentage = percentage;
                        callback.onBatteryPercentageReceived(percentage);
                    }
                });
            } else if (callback != null) {
                callback.onBatteryPercentageReceived(-1);
            }
        } else if (callback != null) {
            callback.onBatteryPercentageReceived(-1);
        }
    }

    public interface BatteryCallback {
        void onBatteryPercentageReceived(int percentage);
    }

    public static int getConnectionStatusColor() {
        return isDroneConnected() ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
    }

    public static String getConnectionStatusText() {
        return isDroneConnected() ? "Connected" : "Disconnected";
    }

    public static void setupConnectionMonitoring(ConnectionCallback callback) {
        Log.d(TAG, "Connection monitoring setup requested");

        if (callback != null) {
            // Basic connection info
            callback.onConnectionStatusChanged(isDroneConnected(), getConnectionStatusText());
            callback.onProductModelChanged(getDroneModel());

            // Battery info with callback
            getBatteryPercentage(percentage -> {
                callback.onBatteryStatusChanged(percentage);
            });
        }
    }

    public static boolean isCameraAvailable() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            return aircraft.getCamera() != null;
        }
        return false;
    }

    public static boolean isFlightControllerAvailable() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            return aircraft.getFlightController() != null;
        }
        return false;
    }

    public static boolean isGimbalAvailable() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            return aircraft.getGimbal() != null ||
                    (aircraft.getGimbals() != null && !aircraft.getGimbals().isEmpty());
        }
        return false;
    }

    public static String getSystemReadinessStatus() {
        if (!isDroneConnected()) {
            return "Drone not connected";
        }

        int battery = lastKnownBatteryPercentage;
        if (battery >= 0 && battery < 20) {
            return "Low battery: " + battery + "%";
        }

        if (!isCameraAvailable()) {
            return "Camera not available";
        }

        if (!isFlightControllerAvailable()) {
            return "Flight controller not available";
        }

        return "System ready";
    }

    public static boolean isSystemReady() {
        return isDroneConnected() &&
                isCameraAvailable() &&
                isFlightControllerAvailable() &&
                lastKnownBatteryPercentage >= 20;
    }

    /**
     * Initialize battery monitoring - call this once when the app starts
     */
    public static void initializeBatteryMonitoring() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            if (aircraft.getBattery() != null) {
                aircraft.getBattery().setStateCallback(batteryState -> {
                    if (batteryState != null) {
                        lastKnownBatteryPercentage = batteryState.getChargeRemainingInPercent();
                        Log.d(TAG, "Battery percentage updated: " + lastKnownBatteryPercentage + "%");
                    }
                });
            }
        }
    }

    /**
     * Clean up battery monitoring - call when shutting down
     */
    public static void cleanup() {
        BaseProduct product = DJISampleApplication.getProductInstance();
        if (product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            if (aircraft.getBattery() != null) {
                aircraft.getBattery().setStateCallback(null);
            }
        }
        lastKnownBatteryPercentage = -1;
    }
}