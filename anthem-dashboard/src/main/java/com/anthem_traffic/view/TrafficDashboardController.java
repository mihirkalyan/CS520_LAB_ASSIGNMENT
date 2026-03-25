package com.anthem_traffic.view;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
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
    
    // ========== FXML Components - Video Feed ==========
    @FXML
    private ImageView videoFeedImageView;
    
    @FXML
    private Label streamStatusLabel;
    
    @FXML
    private Label fpsLabel;
    
    // ========== FXML Components - Vehicle Counts ==========
    @FXML
    private Label carsCountLabel;
    
    @FXML
    private Label trucksCountLabel;
    
    @FXML
    private Label motorcyclesCountLabel;
    
    @FXML
    private Label totalVehiclesLabel;
    
    // ========== FXML Components - Charts ==========
    @FXML
    private PieChart categoryPieChart;
    
    @FXML
    private LineChart<String, Number> trafficTrendChart;
    
    private XYChart.Series<String, Number> carsSeries;
    private XYChart.Series<String, Number> trucksSeries;
    private XYChart.Series<String, Number> motorcyclesSeries;
    
    // ========== FXML Components - Alert System ==========
    @FXML
    private HBox alertContainer;
    
    @FXML
    private Label alertLabel;
    
    @FXML
    private Label alertDescriptionLabel;
    
    // ========== FXML Components - Control Panel ==========
    @FXML
    private Button startStreamButton;
    
    @FXML
    private Button stopStreamButton;
    
    @FXML
    private Button fetchHistoricalButton;
    
    @FXML
    private Button generateReportButton;
    
    // ========== FXML Components - System Status ==========
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

        // TURN OFF THE SHRINKING ANIMATION!
        categoryPieChart.setAnimated(true); 
        trafficTrendChart.setAnimated(true);
    }
    
    /**
     * Setter for Presenter injection
     * Called by main application to establish MVP connection
     */
    public void setPresenter(ITrafficDashboardPresenter presenter) {
        this.presenter = presenter;
    }
    
    // ========================================================================
    // INTERFACE IMPLEMENTATION - Methods called BY Presenter TO update View
    // ========================================================================
    
    @Override
    public void updateVehicleCounts(int cars, int trucks, int motorcycles) {
        // CRITICAL: Use Platform.runLater() to update UI from non-JavaFX threads
        Platform.runLater(() -> {
            carsCountLabel.setText(String.valueOf(cars));
            trucksCountLabel.setText(String.valueOf(trucks));
            motorcyclesCountLabel.setText(String.valueOf(motorcycles));
        });
    }
    
    @Override
    public void updateVideoFrame(Image frame) {
        Platform.runLater(() -> {
            videoFeedImageView.setImage(frame);
        });
    }
    
    @Override
    public void showCongestionAlert(String severity) {
        Platform.runLater(() -> {
            switch (severity.toLowerCase()) {
                case "smooth":
                    alertContainer.setStyle("-fx-background-color: #10b981;");
                    alertLabel.setText("Traffic Smooth");
                    alertDescriptionLabel.setText("Traffic flow is optimal");
                    break;
                    
                case "moderate":
                    alertContainer.setStyle("-fx-background-color: #f59e0b;");
                    alertLabel.setText("Moderate Traffic");
                    alertDescriptionLabel.setText("Traffic density is increasing");
                    break;
                    
                case "heavy":
                    alertContainer.setStyle("-fx-background-color: #ef4444;");
                    alertLabel.setText("Heavy Congestion");
                    alertDescriptionLabel.setText("Alert: High traffic density detected");
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
                // First time only: create the slices
                categoryPieChart.getData().addAll(
                    new PieChart.Data("Cars " + String.format("%.1f", carPercent) + "%", carPercent),
                    new PieChart.Data("Trucks " + String.format("%.1f", truckPercent) + "%", truckPercent),
                    new PieChart.Data("Motorcycles " + String.format("%.1f", motorcyclePercent) + "%", motorcyclePercent)
                );
            } else {
                // Every other time: just silently update the sizes!
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
            } else {
                streamStatusLabel.setText("OFFLINE");
                streamStatusLabel.setTextFill(Color.RED);
                startStreamButton.setDisable(false);
                stopStreamButton.setDisable(true);
            }
        });
    }
    
    @Override
    public void showNotification(String message, String type) {
        Platform.runLater(() -> {
            // Implementation: Could use JavaFX Alert dialog or custom toast notification
            System.out.println("[" + type.toUpperCase() + "] " + message);
            // TODO: Implement visual notification (e.g., ControlsFX Notifications)
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
            // Clear existing data
            carsSeries.getData().clear();
            trucksSeries.getData().clear();
            motorcyclesSeries.getData().clear();

            // Parse and populate charts with historical data
            for (Object dataItem : historicalData) {
                if (dataItem instanceof com.anthem_traffic.model.HistoricalDataPoint) {
                    com.anthem_traffic.model.HistoricalDataPoint point =
                            (com.anthem_traffic.model.HistoricalDataPoint) dataItem;

                    // Extract time from timestamp for display (e.g., "14:30")
                    String timeLabel = extractTimeLabel(point.timestamp);

                    // Add data to each series
                    carsSeries.getData().add(new XYChart.Data<>(timeLabel, point.cars));
                    trucksSeries.getData().add(new XYChart.Data<>(timeLabel, point.trucks));
                    motorcyclesSeries.getData().add(new XYChart.Data<>(timeLabel, point.motorcycles));
                }
            }
        });
    }

    /**
     * Extract time label from timestamp string
     * Handles both "YYYY-MM-DD HH:MM:SS" and "HH:MM:SS" formats
     */
    private String extractTimeLabel(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "00:00";
        }

        if (timestamp.contains(" ")) {
            // "YYYY-MM-DD HH:MM:SS" format
            try {
                return timestamp.substring(11, 16); // Get "HH:MM"
            } catch (StringIndexOutOfBoundsException e) {
                return "00:00";
            }
        } else if (timestamp.contains(":")) {
            // "HH:MM:SS" format
            try {
                return timestamp.substring(0, 5); // Get "HH:MM"
            } catch (StringIndexOutOfBoundsException e) {
                return "00:00";
            }
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
    
    // ========================================================================
    // USER INTERACTION HANDLERS - Forward events TO Presenter
    // ========================================================================
    
    @FXML
    private void handleStartStream() {
        // NO LOGIC HERE - just forward to Presenter
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
            presenter.onGenerateReportClicked();
        }
    }
}
