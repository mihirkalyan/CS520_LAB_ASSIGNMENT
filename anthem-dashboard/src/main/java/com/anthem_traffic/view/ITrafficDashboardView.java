package com.anthem_traffic.view;

import javafx.scene.image.Image;

/**
 * ITrafficDashboardView - View Interface for MVP Architecture
 * 
 * This interface defines the contract between the Presenter and the View.
 * The View (TrafficDashboardController) implements these methods to update UI elements.
 * The Presenter calls these methods to push data updates to the View.
 * 
 * IMPORTANT: The View must remain PASSIVE - it only displays data, never processes it.
 * 
 * @author Heet Shah - UI/UX Developer
 * @project Anthem AI - Drone-Based Traffic Monitoring System
 */
public interface ITrafficDashboardView {
    
    /**
     * Updates the live vehicle count display
     * Called by Presenter when new detection data arrives from YOLOv8
     * 
     * @param cars Number of cars detected
     * @param trucks Number of trucks detected
     * @param motorcycles Number of motorcycles detected
     */
    void updateVehicleCounts(int cars, int trucks, int motorcycles);
    
    /**
     * Updates the video feed with a new frame containing bounding boxes
     * Called by Presenter at ~20-30 FPS from RTSP stream
     * 
     * @param frame JavaFX Image object containing the processed video frame
     */
    void updateVideoFrame(Image frame);
    
    /**
     * Displays a traffic congestion alert
     * Called by Presenter when traffic density crosses thresholds
     * 
     * @param severity Alert level: "smooth", "moderate", or "heavy"
     */
    void showCongestionAlert(String severity);
    
    /**
     * Updates the traffic trend line chart with new data point
     * Called by Presenter every 15 seconds with aggregated data
     * 
     * @param timestamp Time label (e.g., "10:45")
     * @param cars Number of cars
     * @param trucks Number of trucks
     * @param motorcycles Number of motorcycles
     */
    void updateTrafficTrend(String timestamp, int cars, int trucks, int motorcycles);
    
    /**
     * Updates the category distribution pie chart
     * Called by Presenter when category percentages change significantly
     * 
     * @param carPercent Percentage of cars (0-100)
     * @param truckPercent Percentage of trucks (0-100)
     * @param motorcyclePercent Percentage of motorcycles (0-100)
     */
    void updateCategoryDistribution(double carPercent, double truckPercent, double motorcyclePercent);
    
    /**
     * Displays stream status (active/inactive)
     * Called by Presenter when stream connection state changes
     * 
     * @param isActive true if stream is running, false otherwise
     */
    void setStreamStatus(boolean isActive);
    
    /**
     * Shows a notification/toast message to the user
     * Called by Presenter for system notifications (e.g., "Report generated successfully")
     * 
     * @param message The notification message
     * @param type Message type: "info", "success", "warning", "error"
     */
    void showNotification(String message, String type);
    
    /**
     * Updates the total vehicle count display
     * Called by Presenter with aggregated total
     * 
     * @param totalVehicles Total number of vehicles detected
     */
    void updateTotalVehicleCount(int totalVehicles);
    
    /**
     * Displays historical data in the trend chart
     * Called by Presenter after querying database for historical records
     * 
     * @param historicalData Array of data points containing timestamp and counts
     */
    void displayHistoricalData(Object[] historicalData);
    
    /**
     * Updates the connection status indicators
     * Called by Presenter to show backend, database, and AI model status
     * 
     * @param backendConnected FastAPI backend connection status
     * @param databaseConnected Database connection status
     * @param aiModelLoaded YOLOv8 model loaded status
     */
    void updateSystemStatus(boolean backendConnected, boolean databaseConnected, boolean aiModelLoaded);
}
