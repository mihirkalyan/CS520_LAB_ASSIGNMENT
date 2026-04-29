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

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * TrafficDashboardPresenter - Implements MVP Presenter pattern
 *
 * Replaced historical-data replay with real-time polling of
 *         GET /api/traffic/live every 2 seconds.
 * view.updateVideoFrame() hook retained — ready to be called
 *         if a /api/video/frame endpoint is added later.
 * onStopStreamClicked() now resets isStreamActive and the UI
 *         unconditionally, regardless of whether the backend responds.
 */
public class TrafficDashboardPresenter implements ITrafficDashboardPresenter {

    private ITrafficDashboardView view;

    // renamed from dataReplayTimer to liveDataTimer to reflect its real purpose
    private Timeline liveDataTimer;
    private Timeline statusCheckTimer;

    private boolean isStreamActive = false;

    public TrafficDashboardPresenter(ITrafficDashboardView view) {
        this.view = view;
        startStatusCheckTimer();
    }

    // Status check timer — unchanged, runs every 5 s
    private void startStatusCheckTimer() {
        statusCheckTimer = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
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

    private void updateSystemStatus() {
        SystemStatusResponse status = ApiClient.get(ApiConfig.SYSTEM_STATUS, SystemStatusResponse.class);
        if (status != null) {
            Platform.runLater(() ->
                view.updateSystemStatus(status.backend_connected, status.database_connected, status.ai_model_loaded)
            );
        }
    }

    // Start stream: signal backend, then poll /api/traffic/live
    @Override
    public void onStartStreamClicked() {
        if (isStreamActive) {
            System.out.println("[PRESENTER] Stream already active");
            return;
        }

        Task<Boolean> startTask = new Task<>() {
            @Override
            protected Boolean call() {
                // Tell the backend the stream is starting
                SimpleResponse startResponse = ApiClient.post(ApiConfig.STREAM_START, SimpleResponse.class);
                if (startResponse == null) {
                    Platform.runLater(() ->
                        view.showNotification("Failed to connect to backend — is FastAPI running?", "error")
                    );
                    return false;
                }
                return true;
            }
        };

        startTask.setOnSucceeded(event -> {
            if (Boolean.TRUE.equals(startTask.getValue())) {
                isStreamActive = true;
                view.setStreamStatus(true);
                view.showNotification("Stream started — live data polling active", "success");
                //start the LIVE polling timer (not a historical replay)
                startLiveDataTimer();
            }
        });

        startTask.setOnFailed(event -> {
            view.showNotification("Unexpected error starting stream", "error");
            System.err.println("[PRESENTER] startTask failed: " + startTask.getException().getMessage());
        });

        new Thread(startTask).start();
    }

    // Live data timer: polls GET /api/traffic/live every 2 seconds
    private void startLiveDataTimer() {
        if (liveDataTimer != null) {
            liveDataTimer.stop();
        }

        liveDataTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            if (!isStreamActive) return;

            // Run the network call off the FX thread
            Task<HistoricalDataPoint> pollTask = new Task<>() {
                @Override
                protected HistoricalDataPoint call() {
                    // Use HistoricalDataPoint class since it has all the exact fields we need
                    return ApiClient.get(ApiConfig.TRAFFIC_LIVE, HistoricalDataPoint.class);
                }
            };

            pollTask.setOnSucceeded(e -> {
                HistoricalDataPoint live = pollTask.getValue();
                if (live == null) {
                    // Backend unreachable — show warning once, keep trying
                    System.err.println("[PRESENTER] /api/traffic/live returned null — retrying next tick");
                    return;
                }

                // Push every data field to the view
                view.updateVehicleCounts(live.cars, live.trucks, live.motorcycles);
                view.updateTotalVehicleCount(live.total);

                String displayTime = extractTimeFromTimestamp(live.timestamp);
                view.updateTrafficTrend(displayTime, live.cars, live.trucks, live.motorcycles);
                view.updateCategoryDistribution(live.cars_pct, live.trucks_pct, live.motorcycles_pct);
                view.showCongestionAlert(live.congestion_level);
            });

            pollTask.setOnFailed(e ->
                System.err.println("[PRESENTER] Live poll failed: " + pollTask.getException().getMessage())
            );

            new Thread(pollTask).start();
        }));

        liveDataTimer.setCycleCount(Timeline.INDEFINITE);
        liveDataTimer.play();
    }

    private static final DateTimeFormatter TIME_ONLY_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Parses ISO-8601 backend timestamps (e.g. "2024-01-15T14:30:45.123456+00:00")
    // and returns a local HH:mm label for live chart ticks.
    private String extractTimeFromTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return "00:00";
        try {
            OffsetDateTime dt = OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return dt.atZoneSameInstant(ZoneId.systemDefault()).format(TIME_ONLY_FMT);
        } catch (DateTimeParseException e) {
            // Fallback for legacy space-separated or bare time strings
            if (timestamp.length() >= 16 && (timestamp.charAt(10) == 'T' || timestamp.charAt(10) == ' ')) {
                return timestamp.substring(11, 16);
            }
            if (timestamp.contains(":")) {
                return timestamp.substring(0, Math.min(5, timestamp.length()));
            }
            return "00:00";
        }
    }

    private List<HistoricalDataPoint> downsample(List<HistoricalDataPoint> list, int maxPoints) {
        if (list.size() <= maxPoints) return list;
        List<HistoricalDataPoint> result = new ArrayList<>(maxPoints);
        double step = (double)(list.size() - 1) / (maxPoints - 1);
        for (int i = 0; i < maxPoints; i++) {
            result.add(list.get((int) Math.round(i * step)));
        }
        return result;
    }

    // Stop stream: reset UI/state unconditionally, fire-and-forget
    //           backend call so a dead backend cannot leave the UI stuck.

    @Override
    public void onStopStreamClicked() {
        if (!isStreamActive) {
            System.out.println("[PRESENTER] Stream is not active");
            return;
        }

        // Stop the timer and update UI state IMMEDIATELY — do not wait
        //         for the backend to confirm. A dead/slow backend must not
        //         leave isStreamActive=true and the Stop button frozen.
        if (liveDataTimer != null) {
            liveDataTimer.stop();
        }
        isStreamActive = false;
        view.setStreamStatus(false);
        view.showNotification("Stream stopped", "info");

        // Fire-and-forget: tell the backend to mark stream_active=false.
        // If it fails, the UI is already correct — we just log it.
        Task<Void> stopTask = new Task<>() {
            @Override
            protected Void call() {
                SimpleResponse stopResponse = ApiClient.post(ApiConfig.STREAM_STOP, SimpleResponse.class);
                if (stopResponse == null) {
                    System.err.println("[PRESENTER] Backend did not acknowledge stream stop — UI already updated.");
                }
                return null;
            }
        };
        new Thread(stopTask).start();
    }

    // Fetch historical data — unchanged
    @Override
    public void onFetchHistoricalDataClicked() {
        if (isStreamActive) {
            onStopStreamClicked();
            Platform.runLater(() -> view.showNotification("Live stream paused to view historical data.", "info"));
        }
        
        Task<List<HistoricalDataPoint>> fetchTask = new Task<>() {
            @Override
            protected List<HistoricalDataPoint> call() {
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
                    Platform.runLater(() -> view.showNotification("Failed to fetch historical data — keeping current chart", "error"));
                    return null;
                }

                List<HistoricalDataPoint> dataList = new ArrayList<>();
                for (HistoricalDataPoint point : dataArray) {
                    dataList.add(point);
                }

                System.out.println("[PRESENTER] Historical fetch: " + dataList.size() + " records" +
                    (dataList.isEmpty() ? "" :
                     ", first=" + dataList.get(0).timestamp +
                     ", last=" + dataList.get(dataList.size() - 1).timestamp));

                return dataList;
            }
        };

        fetchTask.setOnSucceeded(event -> {
            List<HistoricalDataPoint> data = fetchTask.getValue();
            if (data == null) return; // error already shown in call()
            if (data.isEmpty()) {
                view.showNotification("No historical records found for the selected period", "info");
                return; // leave the chart as-is
            }
            List<HistoricalDataPoint> displayData = downsample(data, 50);
            view.displayHistoricalData(displayData.toArray());
            view.showNotification(
                "Loaded " + data.size() + " records (" + displayData.size() + " shown)", "info");
        });

        fetchTask.setOnFailed(event ->
            view.showNotification("Error fetching historical data", "error")
        );

        new Thread(fetchTask).start();
    }

    // Generate report — unchanged
    @Override
    public void onGenerateReportClicked() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Platform.runLater(() -> view.showNotification("Generating report...", "info"));

                try {
                    ReportResponse reportResponse = ApiClient.post(
                            ApiConfig.REPORTS_GENERATE,
                            new Object(),
                            ReportResponse.class
                    );

                    if (reportResponse == null || reportResponse.report_id == null) {
                        Platform.runLater(() -> view.showNotification("Failed to generate report", "error"));
                        return null;
                    }

                    System.out.println("[PRESENTER] Report generated: " + reportResponse.report_id);

                    if (reportResponse.file_url != null) {
                        String downloadUrl = ApiConfig.getDownloadUrl(reportResponse.report_id);
                        byte[] csvData = ApiClient.downloadFile(downloadUrl);

                        if (csvData != null) {
                            String downloadsPath = System.getProperty("user.home") + "/Downloads";
                            String fileName = "traffic_report_" + System.currentTimeMillis() + ".csv";
                            String filePath = downloadsPath + "/" + fileName;

                            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                                fos.write(csvData);
                            }

                            Platform.runLater(() ->
                                view.showNotification("Report downloaded: " + filePath, "success")
                            );
                            System.out.println("[PRESENTER] Report saved to: " + filePath);
                        } else {
                            Platform.runLater(() -> view.showNotification("Failed to download report", "error"));
                        }
                    }

                } catch (IOException e) {
                    System.err.println("[PRESENTER] Error generating report: " + e.getMessage());
                    Platform.runLater(() -> view.showNotification("Error: " + e.getMessage(), "error"));
                }

                return null;
            }
        };

        new Thread(task).start();
    }

    // Shutdown
    public void shutdown() {
        if (liveDataTimer != null) liveDataTimer.stop();
        if (statusCheckTimer != null) statusCheckTimer.stop();
        if (isStreamActive) onStopStreamClicked();
    }
}
