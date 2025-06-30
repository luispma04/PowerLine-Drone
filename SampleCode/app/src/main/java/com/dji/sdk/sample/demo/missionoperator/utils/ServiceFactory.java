package com.dji.sdk.sample.demo.missionoperator.utils;

import android.content.Context;
import android.util.Log;

import com.dji.sdk.sample.demo.missionoperator.controller.MissionController;
import com.dji.sdk.sample.demo.missionoperator.service.FlightService;
import com.dji.sdk.sample.demo.missionoperator.service.PhotoService;
import com.dji.sdk.sample.demo.missionoperator.service.FileService;
import com.dji.sdk.sample.demo.missionoperator.service.LiveStreamService;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import dji.sdk.base.BaseProduct;
import dji.sdk.mission.MissionControl;
import dji.sdk.products.Aircraft;

public class ServiceFactory {
    private static final String TAG = "ServiceFactory";

    public static MissionController createMissionController(Context context, boolean simulatorMode) {
        Log.d(TAG, "Creating MissionController - simulator mode: " + simulatorMode);

        try {
            // Create services
            FlightService flightService = createFlightService(simulatorMode);
            PhotoService photoService = createPhotoService(context, simulatorMode);

            // Create mission controller
            MissionController controller = new MissionController(context, flightService, photoService);

            Log.d(TAG, "MissionController created successfully");
            return controller;

        } catch (Exception e) {
            Log.e(TAG, "Error creating MissionController", e);
            // Return a basic controller that can still handle file loading
            return new MissionController(context, null, null);
        }
    }

    public static FlightService createFlightService(boolean simulatorMode) {
        Log.d(TAG, "Creating FlightService - simulator mode: " + simulatorMode);

        try {
            BaseProduct product = DJISampleApplication.getProductInstance();
            Aircraft aircraft = null;

            if (product instanceof Aircraft) {
                aircraft = (Aircraft) product;
            }

            if (aircraft != null && !simulatorMode) {
                return new FlightService(
                        aircraft.getFlightController(),
                        aircraft.getFlightController() != null ? aircraft.getFlightController().getFlightAssistant() : null,
                        aircraft.getGimbal() != null ? aircraft.getGimbal() :
                                (aircraft.getGimbals() != null && !aircraft.getGimbals().isEmpty() ? aircraft.getGimbals().get(0) : null)
                );
            } else {
                Log.w(TAG, "Aircraft not available or simulator mode - creating minimal FlightService");
                return new FlightService(null, null, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating FlightService", e);
            return new FlightService(null, null, null);
        }
    }

    public static PhotoService createPhotoService(Context context, boolean simulatorMode) {
        Log.d(TAG, "Creating PhotoService - simulator mode: " + simulatorMode);

        try {
            BaseProduct product = DJISampleApplication.getProductInstance();
            Aircraft aircraft = null;

            if (product instanceof Aircraft) {
                aircraft = (Aircraft) product;
            }

            if (aircraft != null && !simulatorMode) {
                return new PhotoService(context, aircraft.getCamera(), simulatorMode);
            } else {
                Log.w(TAG, "Aircraft not available or simulator mode - creating simulator PhotoService");
                return new PhotoService(context, null, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating PhotoService", e);
            return new PhotoService(context, null, true);
        }
    }

    public static FileService createFileService(Context context) {
        Log.d(TAG, "Creating FileService");
        return new FileService(context);
    }

    public static LiveStreamService createLiveStreamService(Context context, boolean simulatorMode) {
        Log.d(TAG, "Creating LiveStreamService - simulator mode: " + simulatorMode);
        return new LiveStreamService(context, simulatorMode);
    }

    public static String getSystemStatus() {
        StringBuilder status = new StringBuilder();

        try {
            BaseProduct product = DJISampleApplication.getProductInstance();

            status.append("=== SYSTEM STATUS ===\n");

            if (product != null && product.isConnected()) {
                status.append("Product: Connected (").append(product.getModel().getDisplayName()).append(")\n");

                if (product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) product;

                    status.append("Flight Controller: ").append(aircraft.getFlightController() != null ? "Available" : "Not Available").append("\n");
                    status.append("Camera: ").append(aircraft.getCamera() != null ? "Available" : "Not Available").append("\n");
                    status.append("Gimbal: ").append(aircraft.getGimbal() != null ? "Available" : "Not Available").append("\n");

                    if (aircraft.getBattery() != null) {
                        // Note: Battery percentage requires callback, so we show availability only
                        status.append("Battery: Available\n");
                    } else {
                        status.append("Battery: Not Available\n");
                    }
                }
            } else {
                status.append("Product: Not Connected\n");
            }

            status.append("Mission Control: ").append(MissionControl.getInstance() != null ? "Available" : "Not Available").append("\n");
            status.append("Services: Initialized Successfully\n");

        } catch (Exception e) {
            status.append("Error getting system status: ").append(e.getMessage()).append("\n");
        }

        return status.toString();
    }
}