package com.anthem_traffic.model;

import java.util.List;

/**
 * Generic response wrapper for simple API responses
 */
public class SimpleResponse {
    public String message;
    public boolean stream_active;

    public SimpleResponse() {}

    @Override
    public String toString() {
        return String.format("SimpleResponse{message=%s, stream_active=%s}", message, stream_active);
    }
}

/**
 * Response wrapper for list of historical data points
 */
class HistoricalDataResponse {
    public List<HistoricalDataPoint> data;

    public HistoricalDataResponse() {}
}
