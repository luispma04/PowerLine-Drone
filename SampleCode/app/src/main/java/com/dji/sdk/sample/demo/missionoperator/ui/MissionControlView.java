package com.dji.sdk.sample.demo.missionoperator.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.missionmanager.MissionBaseView;
import com.dji.sdk.sample.demo.missionoperator.adapter.PhotoGalleryAdapter;
import com.dji.sdk.sample.demo.missionoperator.adapter.StructureFolderAdapter;
import com.dji.sdk.sample.demo.missionoperator.controller.MissionController;
import com.dji.sdk.sample.demo.missionoperator.service.FlightService;
import com.dji.sdk.sample.demo.missionoperator.service.PhotoService;
import com.dji.sdk.sample.demo.missionoperator.service.FileService;
import com.dji.sdk.sample.demo.missionoperator.service.LiveStreamService;
import com.dji.sdk.sample.demo.missionoperator.storage.PhotoStorageManager;
import com.dji.sdk.sample.demo.missionoperator.utils.Constants;
import com.dji.sdk.sample.demo.missionoperator.utils.ServiceFactory;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.util.List;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;


public class MissionControlView extends MissionBaseView implements
        MissionController.MissionControllerCallback,
        PhotoGalleryAdapter.OnPhotoClickListener,
        View.OnClickListener {

    private static final String TAG = "MissionControlView";

    // === UI COMPONENTS ===
    // Mission controls
    private Button btnLoadStructures;
    private Button btnLoadPhotoPositions;
    private Button btnStartMission;
    private ToggleButton btnPause;
    private Button btnStopMission;
    private Button btnReviewPhoto;
    private TextView csvInfoText;
    private TextView statusText;
    private ProgressBar progressMission;
    private TextView currentStructureText;
    private TextView currentPhotoText;
    private TextView advancedMissionInfoText;

    // Connection status
    private TextView connectionStatusText;
    private TextView modelTextView;
    private TextView batteryText;
    private TextView droneLocationText;

    // Gallery components
    private ViewFlipper viewFlipper;
    private Button btnToggleGallery;
    private Button btnBackToMission;
    private RecyclerView recyclerPhotos;
    private TextView noPhotosText;
    private Button btnLiveStream;
    private FrameLayout liveStreamContainer;

    // Gallery structure components
    private ViewFlipper galleryViewFlipper;
    private RecyclerView recyclerStructures;
    private TextView noStructuresText;
    private Button btnBackToStructures;
    private TextView structureTitleText;
    private TextView galleryTitleText;

    // === SERVICES AND CONTROLLERS ===
    private MissionController missionController;
    private PhotoService photoService;

    // === UI STATE ===
    private PhotoGalleryAdapter photoGalleryAdapter;
    private StructureFolderAdapter structureFolderAdapter;
    private int currentStructureId = -1;
    private boolean isSimulatorMode = false;

    // === CURRENT PHOTO DIALOG ===
    private AlertDialog currentPhotoDialog;

    public MissionControlView(Context context) {
        this(context, false);
    }

    public MissionControlView(Context context, boolean simulatorMode) {
        super(context);
        this.isSimulatorMode = simulatorMode;

        Log.d(TAG, "Initializing MissionControlView - Simulator mode: " + simulatorMode);

        initializeServices();
        initializeUI();
        updateConnectionStatus();
    }

    /**
     * Initialize all services
     */
    private void initializeServices() {
        Log.d(TAG, "Initializing services");

        // Create mission controller with all services
        missionController = ServiceFactory.createMissionController(getContext(), isSimulatorMode);
        missionController.setUiCallback(this);

        // Get photo service for gallery functionality
        photoService = ServiceFactory.createPhotoService(getContext(), isSimulatorMode);

        Log.d(TAG, "Services initialized successfully");
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Determine layout based on orientation
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            inflate(getContext(), R.layout.view_structure_inspection_mission_land, this);
        } else {
            inflate(getContext(), R.layout.view_structure_inspection_mission, this);
        }

        findViews();
        setupClickListeners();
        initializeGallery();
        updateButtonStates();
    }

    /**
     * Find all UI views
     */
    private void findViews() {
        // Mission controls
        btnLoadStructures = findViewById(R.id.btn_load_structures);
        btnLoadPhotoPositions = findViewById(R.id.btn_load_photo_positions);
        btnStartMission = findViewById(R.id.btn_start_mission);
        btnPause = findViewById(R.id.btn_pause);
        btnStopMission = findViewById(R.id.btn_stop_mission);
        btnReviewPhoto = findViewById(R.id.btn_review_photo);
        csvInfoText = findViewById(R.id.text_csv_info);
        statusText = findViewById(R.id.text_status);
        progressMission = findViewById(R.id.progress_mission);
        currentStructureText = findViewById(R.id.text_current_structure);
        currentPhotoText = findViewById(R.id.text_current_photo);
        advancedMissionInfoText = findViewById(R.id.text_advanced_mission_info);

        // Connection status
        connectionStatusText = findViewById(R.id.text_connection_status);
        modelTextView = findViewById(R.id.text_product_model);
        batteryText = findViewById(R.id.text_battery_info);
        droneLocationText = findViewById(R.id.text_drone_location);

        // Gallery components
        viewFlipper = findViewById(R.id.view_flipper);
        btnToggleGallery = findViewById(R.id.btn_toggle_gallery);
        btnBackToMission = findViewById(R.id.btn_back_to_mission);
        recyclerPhotos = findViewById(R.id.recycler_photos);
        noPhotosText = findViewById(R.id.text_no_photos);
        btnLiveStream = findViewById(R.id.btn_live_stream);
        liveStreamContainer = findViewById(R.id.live_stream_container);

        // Gallery structure components
        galleryViewFlipper = findViewById(R.id.gallery_view_flipper);
        recyclerStructures = findViewById(R.id.recycler_structures);
        noStructuresText = findViewById(R.id.text_no_structures);
        btnBackToStructures = findViewById(R.id.btn_back_to_structures);
        structureTitleText = findViewById(R.id.text_structure_title);
        galleryTitleText = findViewById(R.id.text_gallery_title);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Mission controls
        if (btnLoadStructures != null) {
            btnLoadStructures.setOnClickListener(this);
        }

        if (btnLoadPhotoPositions != null) {
            btnLoadPhotoPositions.setOnClickListener(this);
        }

        if (btnStartMission != null) {
            btnStartMission.setOnClickListener(this);
        }

        if (btnStopMission != null) {
            btnStopMission.setOnClickListener(this);
        }

        if (btnReviewPhoto != null) {
            btnReviewPhoto.setOnClickListener(this);
        }

        // Pause button (toggle)
        if (btnPause != null) {
            btnPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        missionController.resumeMission();
                    } else {
                        missionController.pauseMission();
                    }
                }
            });
        }

        // Gallery controls
        if (btnToggleGallery != null) {
            btnToggleGallery.setOnClickListener(this);
        }

        if (btnBackToMission != null) {
            btnBackToMission.setOnClickListener(this);
        }

        if (btnBackToStructures != null) {
            btnBackToStructures.setOnClickListener(this);
        }

        if (btnLiveStream != null) {
            btnLiveStream.setOnClickListener(this);
        }
    }

    /**
     * Handle click events for all buttons
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_load_structures) {
            openFilePicker(Constants.REQUEST_STRUCTURES_CSV);
        } else if (id == R.id.btn_load_photo_positions) {
            openFilePicker(Constants.REQUEST_PHOTO_POSITIONS_CSV);
        } else if (id == R.id.btn_start_mission) {
            missionController.startMission();
        } else if (id == R.id.btn_stop_mission) {
            missionController.stopMission();
        } else if (id == R.id.btn_review_photo) {
            missionController.forcePhotoReview();
        } else if (id == R.id.btn_toggle_gallery) {
            showGalleryView();
        } else if (id == R.id.btn_back_to_mission) {
            showMissionView();
        } else if (id == R.id.btn_back_to_structures) {
            showStructuresList();
        } else if (id == R.id.btn_live_stream) {
            showLiveStreamView();
        }
    }

    /**
     * Initialize photo gallery
     */
    private void initializeGallery() {
        // Setup recycler views
        if (recyclerStructures != null) {
            int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ?
                    Constants.GRID_SPAN_LANDSCAPE : Constants.GRID_SPAN_PORTRAIT;
            recyclerStructures.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        }

        if (recyclerPhotos != null) {
            int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ?
                    Constants.GRID_SPAN_LANDSCAPE : Constants.GRID_SPAN_PORTRAIT;
            recyclerPhotos.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        }

        refreshGallery();
    }

    /**
     * Refresh photo gallery
     */
    private void refreshGallery() {
        if (photoService == null) return;

        List<Integer> structureIds = photoService.getStructureIdsWithPhotos();

        // Update structures list
        if (recyclerStructures != null) {
            if (structureFolderAdapter == null) {
                structureFolderAdapter = new StructureFolderAdapter(
                        getContext(),
                        structureIds,
                        new PhotoStorageManager(getContext()),
                        structureId -> showPhotosForStructure(structureId)
                );
                recyclerStructures.setAdapter(structureFolderAdapter);
            } else {
                structureFolderAdapter.updateStructureList(structureIds);
            }
        }

        // Update no structures text
        if (noStructuresText != null) {
            noStructuresText.setVisibility(structureIds.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Show gallery view
     */
    private void showGalleryView() {
        if (viewFlipper != null) {
            refreshGallery();
            viewFlipper.setDisplayedChild(1);

            if (galleryViewFlipper != null) {
                galleryViewFlipper.setDisplayedChild(0);
            }

            if (galleryTitleText != null) {
                galleryTitleText.setText("Photo Gallery by Structure");
            }
        }
    }

    /**
     * Show mission view
     */
    private void showMissionView() {
        if (viewFlipper != null) {
            viewFlipper.setDisplayedChild(0);
        }
    }

    /**
     * Show structures list
     */
    private void showStructuresList() {
        if (galleryViewFlipper != null) {
            galleryViewFlipper.setDisplayedChild(0);
            currentStructureId = -1;

            if (galleryTitleText != null) {
                galleryTitleText.setText("Photo Gallery by Structure");
            }
        }
    }

    /**
     * Show photos for specific structure
     */
    private void showPhotosForStructure(int structureId) {
        if (galleryViewFlipper != null && photoService != null) {
            currentStructureId = structureId;

            if (structureTitleText != null) {
                structureTitleText.setText("Structure " + structureId);
            }

            List<PhotoStorageManager.PhotoInfo> photos = photoService.getPhotosForStructure(structureId);

            if (noPhotosText != null) {
                noPhotosText.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
            }

            if (recyclerPhotos != null) {
                if (photoGalleryAdapter == null) {
                    photoGalleryAdapter = new PhotoGalleryAdapter(getContext(), photos, this);
                    recyclerPhotos.setAdapter(photoGalleryAdapter);
                } else {
                    photoGalleryAdapter.updatePhotoList(photos);
                }
            }

            galleryViewFlipper.setDisplayedChild(1);

            if (galleryTitleText != null) {
                galleryTitleText.setText("Photos - Structure " + structureId);
            }
        }
    }

    /**
     * Show live stream view
     */
    private void showLiveStreamView() {
        if (liveStreamContainer != null) {
            // Create and show live stream view
            StructureLiveStreamView liveStreamView = new StructureLiveStreamView(getContext());
            liveStreamView.setOnCloseListener(() -> {
                liveStreamContainer.removeAllViews();
                liveStreamContainer.setVisibility(View.GONE);
            });

            liveStreamContainer.removeAllViews();
            liveStreamContainer.addView(liveStreamView);
            liveStreamContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Update button states based on mission state
     */
    private void updateButtonStates() {
        if (missionController == null) return;

        boolean missionInProgress = missionController.isMissionInProgress();
        int totalStructures = missionController.getTotalStructures();
        int totalPhotoPoints = missionController.getTotalPhotoPoints();
        boolean hasData = totalStructures > 0 && totalPhotoPoints > 0;

        // Update button states
        if (btnStartMission != null) {
            btnStartMission.setEnabled(hasData && !missionInProgress);
        }

        if (btnPause != null) {
            btnPause.setEnabled(missionInProgress);
        }

        if (btnStopMission != null) {
            btnStopMission.setEnabled(missionInProgress);
        }

        // Update info text
        if (csvInfoText != null && hasData) {
            csvInfoText.setText(totalStructures + " structures, " + totalPhotoPoints + " photo positions");
        }

        // Log for debugging
        Log.d(TAG, "Button states updated - hasData: " + hasData +
                ", structures: " + totalStructures +
                ", photos: " + totalPhotoPoints +
                ", missionInProgress: " + missionInProgress);
    }

    /**
     * Update connection status
     */
    private void updateConnectionStatus() {
        BaseProduct product = DJISampleApplication.getProductInstance();

        if (connectionStatusText != null) {
            if (product != null && product.isConnected()) {
                connectionStatusText.setText("Connected");
                connectionStatusText.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                connectionStatusText.setText("Disconnected");
                connectionStatusText.setTextColor(Color.parseColor("#F44336"));
            }
        }

        if (modelTextView != null) {
            if (product != null) {
                modelTextView.setText("Model: " + (product.getModel() != null ? product.getModel().getDisplayName() : "Unknown"));
            } else {
                modelTextView.setText("Model: N/A");
            }
        }

        // Update battery info
        if (batteryText != null && product instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) product;
            if (aircraft.getBattery() != null) {
                aircraft.getBattery().setStateCallback(batteryState -> {
                    post(() -> {
                        if (batteryText != null && batteryState != null) {
                            batteryText.setText("Battery: " + batteryState.getChargeRemainingInPercent() + "%");
                        }
                    });
                });
            } else {
                batteryText.setText("Battery: N/A");
            }
        }
    }

    /**
     * Open file picker
     */
    private void openFilePicker(int requestCode) {
        try {
            if (getContext() instanceof FilePickerCallback) {
                ((FilePickerCallback) getContext()).openFilePicker(requestCode);
            } else {
                updateStatus("Error: Activity doesn't implement FilePickerCallback");
            }
        } catch (Exception e) {
            updateStatus("Error opening file picker: " + e.getMessage());
            Log.e(TAG, "Error opening file picker", e);
        }
    }

    // === FILE PICKER INTERFACE ===
    public interface FilePickerCallback {
        void openFilePicker(int requestCode);
    }

    /**
     * Handle file selection result
     */
    public void onFileSelected(int requestCode, Uri fileUri) {
        if (fileUri == null) {
            updateStatus("No file selected");
            return;
        }

        if (requestCode == Constants.REQUEST_STRUCTURES_CSV) {
            missionController.loadInspectionPoints(fileUri);
        } else if (requestCode == Constants.REQUEST_PHOTO_POSITIONS_CSV) {
            missionController.loadPhotoPositions(fileUri);
        }
    }

    // === MISSION CONTROLLER CALLBACKS ===

    @Override
    public void onStatusUpdate(String status) {
        updateStatus(status);
        // Update button states and UI when status changes
        updateButtonStates();
        updateAdvancedMissionInfo();
    }

    @Override
    public void onMissionProgress(int currentStructure, int totalStructures, int currentPhoto, int totalPhotos) {
        if (currentStructureText != null) {
            currentStructureText.setText("Structure: " + currentStructure + "/" + totalStructures);
        }

        if (currentPhotoText != null) {
            currentPhotoText.setText("Photo: " + currentPhoto + "/" + totalPhotos);
        }

        if (progressMission != null) {
            int totalPhotosCount = totalStructures * totalPhotos;
            int completedPhotos = (currentStructure - 1) * totalPhotos + currentPhoto;
            int progressPercent = totalPhotosCount > 0 ? (completedPhotos * 100 / totalPhotosCount) : 0;
            progressMission.setProgress(progressPercent);
        }

        updateAdvancedMissionInfo();
    }

    @Override
    public void onMissionCompleted(boolean success, String message) {
        updateStatus(success ? "Mission completed successfully" : "Mission failed: " + message);
        updateButtonStates();
    }

    @Override
    public void onPhotoReviewRequired(Bitmap photo) {
        showPhotoConfirmationDialog(photo);
    }

    @Override
    public void onError(String error) {
        updateStatus("Error: " + error);
        Log.e(TAG, "Mission error: " + error);
    }

    // === PHOTO GALLERY CALLBACKS ===

    @Override
    public void onPhotoClick(PhotoStorageManager.PhotoInfo photoInfo) {
        // Show fullscreen photo dialog here if needed
        // Implementation similar to original but simplified
    }

    @Override
    public void onDownloadClick(PhotoStorageManager.PhotoInfo photoInfo) {
        // Share photo implementation
    }

    @Override
    public void onDeleteClick(PhotoStorageManager.PhotoInfo photoInfo) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Delete")
                .setMessage("Delete this photo?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (photoService.deletePhoto(photoInfo)) {
                        updateStatus("Photo deleted");
                        refreshGallery();
                    } else {
                        updateStatus("Failed to delete photo");
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    // === PRIVATE UI METHODS ===

    /**
     * Show photo confirmation dialog
     */
    private void showPhotoConfirmationDialog(Bitmap photo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        int orientation = getResources().getConfiguration().orientation;
        View dialogView;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_photo_confirmation_land, null);
        } else {
            dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_photo_confirmation, null);
        }

        builder.setView(dialogView);

        ImageView photoImageView = dialogView.findViewById(R.id.popup_image_preview);
        TextView photoDetailsText = dialogView.findViewById(R.id.text_photo_details);
        Button retakeButton = dialogView.findViewById(R.id.btn_popup_retake);
        Button acceptButton = dialogView.findViewById(R.id.btn_popup_accept);

        photoImageView.setImageBitmap(photo);
        photoDetailsText.setText("Structure: S" + missionController.getCurrentStructureIndex() +
                " | Position: P" + missionController.getCurrentPhotoIndex());

        currentPhotoDialog = builder.create();
        currentPhotoDialog.setCancelable(false);

        retakeButton.setOnClickListener(v -> {
            currentPhotoDialog.dismiss();
            missionController.retakePhoto();
        });

        acceptButton.setOnClickListener(v -> {
            currentPhotoDialog.dismiss();
            missionController.acceptPhoto();
        });

        currentPhotoDialog.show();
    }

    /**
     * Update status text
     */
    private void updateStatus(String status) {
        if (statusText != null) {
            statusText.setText("Status: " + status);
        }
        Log.d(TAG, "Status: " + status);
    }

    /**
     * Update advanced mission info
     */
    private void updateAdvancedMissionInfo() {
        if (advancedMissionInfoText == null || missionController == null) return;

        StringBuilder info = new StringBuilder();

        info.append("MISSION PARAMETERS:\n");
        info.append("Speed: ").append(Constants.DEFAULT_SPEED).append(" m/s\n");
        info.append("Base altitude: ").append(Constants.DEFAULT_ALTITUDE).append(" m\n");
        info.append("Safety altitude: ").append(Constants.SAFETY_ALTITUDE).append(" m\n");
        info.append("Safe distance: ").append(Constants.SAFE_DISTANCE).append(" m\n\n");

        info.append("STATISTICS:\n");
        info.append("Structures: ").append(missionController.getTotalStructures()).append("\n");
        info.append("Photo positions per structure: ").append(missionController.getTotalPhotoPoints()).append("\n");
        info.append("Mission in progress: ").append(missionController.isMissionInProgress() ? "Yes" : "No").append("\n\n");

        info.append("OBSTACLE AVOIDANCE:\n");
        info.append("Enabled: ").append(missionController.isObstacleAvoidanceEnabled() ? "Yes" : "No").append("\n");
        if (missionController.isObstacleAvoidanceEnabled()) {
            float distance = missionController.getClosestObstacleDistance();
            if (distance < Float.MAX_VALUE) {
                info.append("Closest obstacle: ").append(String.format("%.2f", distance)).append("m\n");
            }
        }

        advancedMissionInfoText.setText(info.toString());
    }

    // === LIFECYCLE METHODS ===

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateConnectionStatus();
    }

    @Override
    protected void onDetachedFromWindow() {
        // Cleanup
        if (currentPhotoDialog != null && currentPhotoDialog.isShowing()) {
            currentPhotoDialog.dismiss();
        }

        if (missionController != null) {
            missionController.cleanup();
        }

        super.onDetachedFromWindow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Recreate UI for new orientation
        removeAllViews();
        initializeUI();
        updateConnectionStatus();
        updateButtonStates();
    }

    // === PRODUCT CONNECTION CALLBACK ===

    public void onProductConnected() {
        post(() -> {
            updateConnectionStatus();
            // Reinitialize services with new product connection
            initializeServices();
        });
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_structure_inspection_mission;
    }
}