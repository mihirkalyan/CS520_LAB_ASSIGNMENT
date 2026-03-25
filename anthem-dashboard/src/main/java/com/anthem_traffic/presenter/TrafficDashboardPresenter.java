package com.anthem_traffic.presenter;

import com.anthem_traffic.view.ITrafficDashboardView;
import com.anthem_traffic.network.ApiClient;
import com.anthem_traffic.model.HistoricalDataPoint;
import com.anthem_traffic.model.SystemStatusResponse;
import com.anthem_traffic.model.ReportResponse;
import com.anthem_traffic.model.SimpleResponse;
import com.anthem_traffic.config.ApiConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * TrafficDashboardPresenter - Implements MVP Presenter pattern
 *
 * Handles all interaction between the View and the Backend API.
 * Fetches seed data from the database and replays it at 2-second intervals.
 * Later, this will be replaced with live AI detection data via POST /api/traffic/record.
 */
public class TrafficDashboardPresenter implements ITrafficDashboardPresenter {

    private ITrafficDashboardView view;
    private Timeline dataReplayTimer;
    private Timeline statusCheckTimer;
    private List<HistoricalDataPoint> currentDataSet;
    private int currentDataIndex = 0;
    private boolean isStreamActive = false;

    public TrafficDashboardPresenter(ITrafficDashboardView view) {
        this.view = view;
        this.currentDataSet = new ArrayList<>();
        startStatusCheckTimer();
    }

    /**
     * Start periodic health check to update system status
     */
    private void startStatusCheckTimer() {
        statusCheckTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(5), event -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    updateSystemStatus();
                    return null;
                }
            };
            new Thread(task).start();
        }));
        statusCheckTimer.setCycleCount(Timeline.INDEFINITE);
        statusCheckTimer.play();
    }

    /**
     * Fetch system status from backend and update UI
     */
    private void updateSystemStatus() {
        SystemStatusResponse status = ApiClient.get(ApiConfig.SYSTEM_STATUS, SystemStatusResponse.class);
        if (status != null) {
            Platform.runLater(() -> {
                view.updateSystemStatus(status.backend_connected, status.database_connected, status.ai_model_loaded);
            });
        }
    }

    /**
     * Called when "Start Stream" button is clicked
     * Fetches seed data from backend and starts replay timer
     */
    @Override
    public void onStartStreamClicked() {
        if (isStreamActive) {
            System.out.println("Stream already active");
            return;
        }

        // Run fetch operation in background thread
        Task<List<HistoricalDataPoint>> fetchTask = new Task<>() {
            @Override
            protected List<HistoricalDataPoint> call() {
                // Call backend API to start stream (marks stream_active=true in DB)
                SimpleResponse startResponse = ApiClient.post(ApiConfig.STREAM_START, SimpleResponse.class);
                if (startResponse == null) {
                    Platform.runLater(() -> view.showNotification("Failed to connect to backend", "error"));
                    return null;
                }

                // Fetch seed data from backend
                // Query for last 30 days to capture all seed data (generated for last 7 days)
                // Format: YYYY-MM-DDTHH:MM:SS
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime thirtyDaysAgo = now.minusDays(30);
                String fromDate = thirtyDaysAgo.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String toDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String queryParams = "from=" + fromDate + "&to=" + toDate + "&limit=2000";

                HistoricalDataPoint[] dataArray = ApiClient.get(
                        ApiConfig.TRAFFIC_HISTORICAL,
                        queryParams,
                        HistoricalDataPoint[].class
                );

                if (dataArray == null || dataArray.length == 0) {
                    Platform.runLater(() -> view.showNotification("No historical data available", "warning"));
                    return null;
                }

                List<HistoricalDataPoint> dataList = new ArrayList<>();
                for (HistoricalDataPoint point : dataArray) {
                    dataList.add(point);
                }

                System.out.println("Fetched " + dataList.size() + " data points from backend");
                return dataList;
            }
        };

        fetchTask.setOnSucceeded(event -> {
            List<HistoricalDataPoint> data = fetchTask.getValue();
            if (data != null && !data.isEmpty()) {
                currentDataSet = data;
                currentDataIndex = 0;
                isStreamActive = true;
                view.setStreamStatus(true);
                view.showNotification("Stream started - replaying historical data", "success");
                startDataReplayTimer();
            }
        });

        fetchTask.setOnFailed(event -> {
            view.showNotification("Failed to fetch data from backend", "error");
            System.err.println("Error fetching data: " + fetchTask.getException().getMessage());
        });

        new Thread(fetchTask).start();
    }

    /**
     * Start the replay timer to update UI with seed data at 2-second intervals
     */
    private void startDataReplayTimer() {
        if (dataReplayTimer != null) {
            dataReplayTimer.stop();
        }

        dataReplayTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            if (currentDataSet.isEmpty() || !isStreamActive) {
                return;
            }

            // Get next data point from seed data
            HistoricalDataPoint dataPoint = currentDataSet.get(currentDataIndex % currentDataSet.size());
            currentDataIndex++;

            // Extract timestamp (format: "HH:MM:SS" for display)
            String displayTime = extractTimeFromTimestamp(dataPoint.timestamp);

            // Update all UI components
            view.updateVehicleCounts(dataPoint.cars, dataPoint.trucks, dataPoint.motorcycles);
            view.updateTotalVehicleCount(dataPoint.total);
            view.updateTrafficTrend(displayTime, dataPoint.cars, dataPoint.trucks, dataPoint.motorcycles);
            view.updateCategoryDistribution(dataPoint.cars_pct, dataPoint.trucks_pct, dataPoint.motorcycles_pct);
            view.showCongestionAlert(dataPoint.congestion_level);
        }));

        dataReplayTimer.setCycleCount(Timeline.INDEFINITE);
        dataReplayTimer.play();
    }

    /**
     * Extract time portion from timestamp (e.g., "10:45:30" from full datetime)
     */
    private String extractTimeFromTimestamp(String timestamp) {
        if (timestamp == null || timestamp.length() < 8) {
            return "00:00";
        }
        // Handle both "YYYY-MM-DD HH:MM:SS" and "HH:MM:SS" formats
        if (timestamp.contains(" ")) {
            return timestamp.substring(11, 16); // Get "HH:MM"
        } else if (timestamp.contains(":")) {
            return timestamp.substring(0, 5); // Get "HH:MM"
        }
        return "00:00";
    }

    /**
     * Called when "Stop Stream" button is clicked
     */
    @Override
    public void onStopStreamClicked() {
        if (!isStreamActive) {
            System.out.println("Stream is not active");
            return;
        }

        // Stop the replay timer
        if (dataReplayTimer != null) {
            dataReplayTimer.stop();
        }

        // Call backend API to stop stream
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                SimpleResponse stopResponse = ApiClient.post(ApiConfig.STREAM_STOP, SimpleResponse.class);
                if (stopResponse != null) {
                    Platform.runLater(() -> {
                        isStreamActive = false;
                        view.setStreamStatus(false);
                        view.showNotification("Stream stopped", "info");
                    });
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    /**
     * Called when "Fetch Historical Data" button is clicked
     * Currently displays the current dataset or fetches with custom date range
     */
    @Override
    public void onFetchHistoricalDataClicked() {
        Task<List<HistoricalDataPoint>> fetchTask = new Task<>() {
            @Override
            protected List<HistoricalDataPoint> call() {
                // For now, fetch last 30 days of data
                // Later, this can be enhanced with date pickers in UI
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime thirtyDaysAgo = now.minusDays(30);
                String fromDate = thirtyDaysAgo.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String toDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String queryParams = "from=" + fromDate + "&to=" + toDate + "&limit=1000";

                HistoricalDataPoint[] dataArray = ApiClient.get(
                        ApiConfig.TRAFFIC_HISTORICAL,
                        queryParams,
                        HistoricalDataPoint[].class
                );

                if (dataArray == null) {
                    Platform.runLater(() -> view.showNotification("Failed to fetch historical data", "error"));
                    return null;
                }

                List<HistoricalDataPoint> dataList = new ArrayList<>();
                for (HistoricalDataPoint point : dataArray) {
                    dataList.add(point);
                }

                return dataList;
            }
        };

        fetchTask.setOnSucceeded(event -> {
            List<HistoricalDataPoint> data = fetchTask.getValue();
            if (data != null) {
                // Convert to Object array for display
                Object[] displayData = data.toArray();
                view.displayHistoricalData(displayData);
                view.showNotification("Loaded " + data.size() + " historical records", "info");
            }
        });

        fetchTask.setOnFailed(event -> {
            view.showNotification("Error fetching historical data", "error");
        });

        new Thread(fetchTask).start();
    }

    /**
     * Called when "Generate Report" button is clicked
     * Generates a CSV report and downloads it
     */
    @Override
    public void onGenerateReportClicked() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Platform.runLater(() -> view.showNotification("Generating report...", "info"));

                try {
                    // Call backend to generate report
                    ReportResponse reportResponse = ApiClient.post(
                            ApiConfig.REPORTS_GENERATE,
                            new Object(), // Empty body
                            ReportResponse.class
                    );

                    if (reportResponse == null || reportResponse.report_id == null) {
                        Platform.runLater(() -> view.showNotification("Failed to generate report", "error"));
                        return;
                    }

                    System.out.println("Report generated: " + reportResponse.report_id);

                    // Poll for report to be ready
                    boolean reportReady = false;
                    int pollAttempts = 0;
                    while (!reportReady && pollAttempts < 30) { // Max 30 seconds
                        Thread.sleep(1000); // Wait 1 second
                        pollAttempts++;

                        // Optionally check report status
                        // For now, assume report is ready after generation
                        reportReady = true;
                    }

                    if (reportReady && reportResponse.file_url != null) {
                        // Download the report
                        String downloadUrl = ApiConfig.getDownloadUrl(reportResponse.report_id);
                        byte[] csvData = ApiClient.downloadFile(downloadUrl);

                        if (csvData != null) {
                            // Save to Downloads folder
                            String downloadsPath = System.getProperty("user.home") + "/Downloads";
                            String fileName = "traffic_report_" + System.currentTimeMillis() + ".csv";
                            String filePath = downloadsPath + "/" + fileName;

                            FileOutputStream fos = new FileOutputStream(filePath);
                            fos.write(csvData);
                            fos.close();

                            Platform.runLater(() ->
                                view.showNotification("Report downloaded: " + filePath, "success")
                            );
                            System.out.println("Report saved to: " + filePath);
                        } else {
                            Platform.runLater(() -> view.showNotification("Failed to download report", "error"));
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error generating report: " + e.getMessage());
                    Platform.runLater(() -> view.showNotification("Error: " + e.getMessage(), "error"));
                }

                return null;
            }
        };

        new Thread(task).start();
    }

    /**
     * Cleanup method - call when application terminates
     */
    public void shutdown() {
        if (dataReplayTimer != null) {
            dataReplayTimer.stop();
        }
        if (statusCheckTimer != null) {
            statusCheckTimer.stop();
        }
        if (isStreamActive) {
            onStopStreamClicked();
        }
    }
}
