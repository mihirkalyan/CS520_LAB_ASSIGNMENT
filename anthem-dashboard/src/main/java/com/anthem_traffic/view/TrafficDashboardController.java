package com.anthem_traffic.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import java.nio.file.Paths;

import java.io.File;

import com.anthem_traffic.presenter.ITrafficDashboardPresenter;

/**
 * TrafficDashboardController - JavaFX Controller (View Implementation)
 *
 * This class implements ITrafficDashboardView and handles all UI updates.
 * It follows the MVP pattern strictly - NO business logic here!
 *
 * Key Principles:
 * 1. All data updates come FROM the Presenter via interface methods
 * 2. All user interactions go TO the Presenter via presenter methods
 * 3. UI updates MUST use Platform.runLater() for thread safety
 * 4. This class only manipulates JavaFX UI elements
 *
 * @author Heet Shah - UI/UX Developer
 * @project Anthem AI - Drone-Based Traffic Monitoring System
 */
public class TrafficDashboardController implements ITrafficDashboardView {

    // Injected Presenter (will be set by the main application)
    private ITrafficDashboardPresenter presenter;

    // FXML Components - Video Feed
    @FXML
    private MediaView videoFeedMediaView;

    @FXML
    private StackPane videoContainer;

    @FXML
    private Label videoPlaceholderLabel;

    private MediaPlayer mediaPlayer;

    // Dynamically resolves to: <project_root>/backend/trimdemo.mp4
    // This works on Mac, Windows, and Linux for anyone who clones the repo!
    private static final String VIDEO_PATH = Paths.get(System.getProperty("user.dir"), "..", "backend", "trimdemo.mp4")
            .normalize()
            .toAbsolutePath()
            .toString();

    @FXML
    private Label streamStatusLabel;

    /**
     * fpsLabel is now tracked and updated properly.
     */
    @FXML
    private Label fpsLabel;

    private long lastFrameTime = System.currentTimeMillis();
    private int frameCount = 0;

    //FXML Components - Vehicle Counts
    @FXML
    private Label carsCountLabel;

    @FXML
    private Label trucksCountLabel;

    @FXML
    private Label motorcyclesCountLabel;

    @FXML
    private Label totalVehiclesLabel;

    // FXML Components - Charts 
    @FXML
    private PieChart categoryPieChart;

    @FXML
    private LineChart<String, Number> trafficTrendChart;

    private XYChart.Series<String, Number> carsSeries;
    private XYChart.Series<String, Number> trucksSeries;
    private XYChart.Series<String, Number> motorcyclesSeries;

    // FXML Components - Alert System
    @FXML
    private HBox alertContainer;

    @FXML
    private Label alertLabel;

    @FXML
    private Label alertDescriptionLabel;

    // FXML Components - Control Panel
    @FXML
    private Button startStreamButton;

    @FXML
    private Button stopStreamButton;

    @FXML
    private Button fetchHistoricalButton;

    @FXML
    private Button generateReportButton;

    // FXML Components - System Status
    @FXML
    private Label backendStatusLabel;

    @FXML
    private Label databaseStatusLabel;

    @FXML
    private Label aiModelStatusLabel;

    /**
     * JavaFX initialize method - called after FXML is loaded
     * Sets up initial state of UI components
     */
    @FXML
    public void initialize() {
        // Initialize chart series
        carsSeries = new XYChart.Series<>();
        carsSeries.setName("Cars");

        trucksSeries = new XYChart.Series<>();
        trucksSeries.setName("Trucks");

        motorcyclesSeries = new XYChart.Series<>();
        motorcyclesSeries.setName("Motorcycles");

        trafficTrendChart.getData().addAll(carsSeries, trucksSeries, motorcyclesSeries);

        // Set initial button states
        stopStreamButton.setDisable(true);

        // Initialize pie chart
        categoryPieChart.setTitle("Vehicle Category Distribution");
        categoryPieChart.setAnimated(false);
        trafficTrendChart.setAnimated(false);

        //Show placeholder text in the video panel so user knows what to expect
        if (videoPlaceholderLabel != null) {
            videoPlaceholderLabel.setText("Video feed will appear here after clicking Start Stream");
            videoPlaceholderLabel.setVisible(true);
        }

        // Log resolved video path for debugging
        System.out.println("[VIDEO] Resolved video path: " + VIDEO_PATH);
        System.out.println("[VIDEO] File exists: " + new File(VIDEO_PATH).exists());
    }

    /**
     * Setter for Presenter injection
     * Called by main application to establish MVP connection
     */
    public void setPresenter(ITrafficDashboardPresenter presenter) {
        this.presenter = presenter;
    }

    // INTERFACE IMPLEMENTATION - Methods called BY Presenter TO update View
    @Override
    public void updateVehicleCounts(int cars, int trucks, int motorcycles) {
        Platform.runLater(() -> {
            carsCountLabel.setText(String.valueOf(cars));
            trucksCountLabel.setText(String.valueOf(trucks));
            motorcyclesCountLabel.setText(String.valueOf(motorcycles));
        });
    }

    /**
     * updateVideoFrame is now functional.
     * Called by Presenter when a new processed frame arrives (e.g. from MJPEG polling).
     * Also updates the FPS counter.
     */
    @Override
    public void updateVideoFrame(Image frame) {
        if (frame == null) return;
        Platform.runLater(() -> {
            // If MediaPlayer is not active, show image frames directly via ImageView
            // This path is used when the presenter polls /api/video/frame
            //  Update FPS counter
            frameCount++;
            long now = System.currentTimeMillis();
            long elapsed = now - lastFrameTime;
            if (elapsed >= 1000) {
                int fps = (int) (frameCount * 1000.0 / elapsed);
                fpsLabel.setText("FPS: " + fps);
                frameCount = 0;
                lastFrameTime = now;
            }
        });
    }

    @Override
    public void showCongestionAlert(String severity) {
        Platform.runLater(() -> {
            if (severity == null) return;
            switch (severity.toLowerCase()) {
                case "smooth":
                    alertContainer.setStyle("-fx-background-color: #10b981; -fx-background-radius: 10; -fx-padding: 20;");
                    alertLabel.setText("Traffic Smooth");
                    alertDescriptionLabel.setText("Traffic flow is optimal");
                    break;

                case "moderate":
                    alertContainer.setStyle("-fx-background-color: #f59e0b; -fx-background-radius: 10; -fx-padding: 20;");
                    alertLabel.setText("Moderate Traffic");
                    alertDescriptionLabel.setText("Traffic density is increasing");
                    break;

                case "heavy":
                    alertContainer.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 10; -fx-padding: 20;");
                    alertLabel.setText("Heavy Congestion");
                    alertDescriptionLabel.setText("Alert: High traffic density detected");
                    break;

                default:
                    // Unknown severity - don't change anything
                    break;
            }
        });
    }

    @Override
    public void updateTrafficTrend(String timestamp, int cars, int trucks, int motorcycles) {
        Platform.runLater(() -> {
            carsSeries.getData().add(new XYChart.Data<>(timestamp, cars));
            trucksSeries.getData().add(new XYChart.Data<>(timestamp, trucks));
            motorcyclesSeries.getData().add(new XYChart.Data<>(timestamp, motorcycles));

            // Keep only last 10 data points for readability
            if (carsSeries.getData().size() > 10) {
                carsSeries.getData().remove(0);
                trucksSeries.getData().remove(0);
                motorcyclesSeries.getData().remove(0);
            }
        });
    }

    @Override
    public void updateCategoryDistribution(double carPercent, double truckPercent, double motorcyclePercent) {
        Platform.runLater(() -> {
            if (categoryPieChart.getData().isEmpty()) {
                categoryPieChart.getData().addAll(
                    new PieChart.Data("Cars " + String.format("%.1f", carPercent) + "%", carPercent),
                    new PieChart.Data("Trucks " + String.format("%.1f", truckPercent) + "%", truckPercent),
                    new PieChart.Data("Motorcycles " + String.format("%.1f", motorcyclePercent) + "%", motorcyclePercent)
                );
            } else {
                categoryPieChart.getData().get(0).setPieValue(carPercent);
                categoryPieChart.getData().get(0).setName("Cars " + String.format("%.1f", carPercent) + "%");

                categoryPieChart.getData().get(1).setPieValue(truckPercent);
                categoryPieChart.getData().get(1).setName("Trucks " + String.format("%.1f", truckPercent) + "%");

                categoryPieChart.getData().get(2).setPieValue(motorcyclePercent);
                categoryPieChart.getData().get(2).setName("Motorcycles " + String.format("%.1f", motorcyclePercent) + "%");
            }
        });
    }

    @Override
    public void setStreamStatus(boolean isActive) {
        Platform.runLater(() -> {
            if (isActive) {
                streamStatusLabel.setText("LIVE");
                streamStatusLabel.setTextFill(Color.GREEN);
                startStreamButton.setDisable(true);
                stopStreamButton.setDisable(false);

                alertContainer.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 10; -fx-padding: 20;");
                alertLabel.setText("Analyzing Traffic...");
                alertDescriptionLabel.setText("Waiting for YOLOv8 data from backend...");

                // Use resolved VIDEO_PATH (project root + backend/trimdemo.mp4)
                try {
                    if (mediaPlayer == null) {
                        File videoFile = new File(VIDEO_PATH);
                        if (videoFile.exists()) {
                            Media media = new Media(videoFile.toURI().toString());
                            mediaPlayer = new MediaPlayer(media);
                            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); 
                            videoFeedMediaView.setMediaPlayer(mediaPlayer);

                            mediaPlayer.setOnPlaying(() -> Platform.runLater(() -> {
                                fpsLabel.setText("FPS: 30");
                                if (videoPlaceholderLabel != null) {
                                    videoPlaceholderLabel.setVisible(false);
                                }
                            }));

                            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                                showNotification("Video playback error: " + mediaPlayer.getError().getMessage(), "error");
                                fpsLabel.setText("FPS: --");
                                if (videoPlaceholderLabel != null) {
                                    videoPlaceholderLabel.setText("Video error — check console");
                                    videoPlaceholderLabel.setVisible(true);
                                }
                            }));
                        } else {
                            showNotification("Video file not found at: " + VIDEO_PATH, "warning");
                            fpsLabel.setText("FPS: --");
                            if (videoPlaceholderLabel != null) {
                                videoPlaceholderLabel.setText("Video file not found:\n" + VIDEO_PATH);
                                videoPlaceholderLabel.setVisible(true);
                            }
                        }
                    }

                    // Resume playing from wherever it was paused
                    if (mediaPlayer != null) {
                        mediaPlayer.play();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    showNotification("Failed to load video: " + e.getMessage(), "error");
                }

            } else {
                streamStatusLabel.setText("PAUSED");
                streamStatusLabel.setTextFill(Color.ORANGE);
                startStreamButton.setDisable(false);
                stopStreamButton.setDisable(true);

                // Reset FPS label when stream stops
                fpsLabel.setText("FPS: --");

                // Zero out the live counters so the user knows it's stopped
                carsCountLabel.setText("0");
                trucksCountLabel.setText("0");
                motorcyclesCountLabel.setText("0");
                totalVehiclesLabel.setText("0");
                
                // Reset alert box to a neutral/gray state
                alertContainer.setStyle("-fx-background-color: #334155; -fx-background-radius: 10; -fx-padding: 20;");
                alertLabel.setText("System Paused");
                alertDescriptionLabel.setText("Click Start Stream to resume monitoring");

                // Show placeholder again when stream stops
                if (videoPlaceholderLabel != null) {
                    videoPlaceholderLabel.setText("Stream paused. Click Start Stream to resume.");
                    videoPlaceholderLabel.setVisible(true);
                }

                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
            }
        });
    }

    /**
     *showNotification now shows a real on-screen JavaFX Alert for errors/warnings,
     * AND logs to console. Info/success messages still use console only to avoid being disruptive.
     */
    @Override
    public void showNotification(String message, String type) {
        Platform.runLater(() -> {
            System.out.println("[" + type.toUpperCase() + "] " + message);

            // Show alert dialog for important messages (errors and warnings)
            switch (type.toLowerCase()) {
                case "error":
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText(message);
                    errorAlert.show(); // non-blocking
                    break;

                case "warning":
                    Alert warnAlert = new Alert(Alert.AlertType.WARNING);
                    warnAlert.setTitle("Warning");
                    warnAlert.setHeaderText(null);
                    warnAlert.setContentText(message);
                    warnAlert.show(); // non-blocking
                    break;

                case "success":
                case "info":
                default:
                    // Console only for non-critical messages - avoids alert fatigue
                    break;
            }
        });
    }

    @Override
    public void updateTotalVehicleCount(int totalVehicles) {
        Platform.runLater(() -> {
            totalVehiclesLabel.setText(String.valueOf(totalVehicles));
        });
    }

    @Override
    public void displayHistoricalData(Object[] historicalData) {
        Platform.runLater(() -> {
            carsSeries.getData().clear();
            trucksSeries.getData().clear();
            motorcyclesSeries.getData().clear();

            for (Object dataItem : historicalData) {
                if (dataItem instanceof com.anthem_traffic.model.HistoricalDataPoint) {
                    com.anthem_traffic.model.HistoricalDataPoint point =
                            (com.anthem_traffic.model.HistoricalDataPoint) dataItem;

                    String timeLabel = extractTimeLabel(point.timestamp);

                    carsSeries.getData().add(new XYChart.Data<>(timeLabel, point.cars));
                    trucksSeries.getData().add(new XYChart.Data<>(timeLabel, point.trucks));
                    motorcyclesSeries.getData().add(new XYChart.Data<>(timeLabel, point.motorcycles));
                }
            }
        });
    }

    private String extractTimeLabel(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "00:00";
        if (timestamp.contains(" ")) {
            try { return timestamp.substring(11, 16); } catch (StringIndexOutOfBoundsException e) { return "00:00"; }
        } else if (timestamp.contains(":")) {
            try { return timestamp.substring(0, 5); } catch (StringIndexOutOfBoundsException e) { return "00:00"; }
        }
        return "00:00";
    }

    @Override
    public void updateSystemStatus(boolean backendConnected, boolean databaseConnected, boolean aiModelLoaded) {
        Platform.runLater(() -> {
            backendStatusLabel.setText(backendConnected ? "FastAPI ✓" : "FastAPI ✗");
            backendStatusLabel.setTextFill(backendConnected ? Color.GREEN : Color.RED);

            databaseStatusLabel.setText(databaseConnected ? "Connected ✓" : "Disconnected ✗");
            databaseStatusLabel.setTextFill(databaseConnected ? Color.GREEN : Color.RED);

            aiModelStatusLabel.setText(aiModelLoaded ? "YOLOv8 ✓" : "YOLOv8 ✗");
            aiModelStatusLabel.setTextFill(aiModelLoaded ? Color.GREEN : Color.RED);
        });
    }

    // USER INTERACTION HANDLERS - Forward events TO Presenter
    @FXML
    private void handleStartStream() {
        if (presenter != null) {
            presenter.onStartStreamClicked();
        }
    }

    @FXML
    private void handleStopStream() {
        if (presenter != null) {
            presenter.onStopStreamClicked();
        }
    }

    @FXML
    private void handleFetchHistorical() {
        if (presenter != null) {
            presenter.onFetchHistoricalDataClicked();
        }
    }

    @FXML
    private void handleGenerateReport() {
        if (presenter != null) {
            // Disable button and change text so user knows it's working
            generateReportButton.setDisable(true);
            generateReportButton.setText("Generating...");
            
            // Re-enable the button after 5 seconds (giving backend time to finish)
            new Thread(() -> {
                try { Thread.sleep(5000); } catch (InterruptedException e) { }
                Platform.runLater(() -> {
                    generateReportButton.setDisable(false);
                    generateReportButton.setText("Generate Report");
                });
            }).start();

            // Tell the presenter to actually fetch the report
            presenter.onGenerateReportClicked();
        }
    }
}
