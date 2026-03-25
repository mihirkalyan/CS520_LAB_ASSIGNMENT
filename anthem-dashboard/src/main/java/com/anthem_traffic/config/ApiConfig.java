package com.anthem_traffic.config;

/**
 * Centralized API configuration for backend communication.
 * Provides base URL and endpoint constants for all API calls.
 */
public class ApiConfig {
    // Base URL for backend API
    public static final String BASE_URL = "http://localhost:8000";

    // API Endpoints
    public static final String HEALTH_CHECK = "/health";

    // Stream endpoints
    public static final String STREAM_START = "/api/stream/start";
    public static final String STREAM_STOP = "/api/stream/stop";

    // Traffic data endpoints
    public static final String TRAFFIC_RECORD = "/api/traffic/record";
    public static final String TRAFFIC_LIVE = "/api/traffic/live";
    public static final String TRAFFIC_HISTORICAL = "/api/traffic/historical";
    public static final String SYSTEM_STATUS = "/api/traffic/system/status";

    // Report endpoints
    public static final String REPORTS_GENERATE = "/api/reports";
    public static final String REPORTS_DOWNLOAD = "/api/reports/{report_id}/download";

    // HTTP Configuration
    public static final int CONNECT_TIMEOUT_SECONDS = 5;
    public static final int REQUEST_TIMEOUT_SECONDS = 5;

    // Retry Configuration
    public static final int MAX_RETRIES = 3;
    public static final long RETRY_DELAY_MS = 1000; // Start with 1 second

    /**
     * Get full URL for an endpoint
     */
    public static String getFullUrl(String endpoint) {
        return BASE_URL + endpoint;
    }

    /**
     * Get full URL for a download endpoint with dynamic report ID
     */
    public static String getDownloadUrl(String reportId) {
        return BASE_URL + REPORTS_DOWNLOAD.replace("{report_id}", reportId);
    }
}
