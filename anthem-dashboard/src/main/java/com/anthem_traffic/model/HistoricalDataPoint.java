package com.anthem_traffic.model;

import java.util.List;

/**
 * Response model for historical traffic data
 */
public class HistoricalDataPoint {
    public String id;
    public String timestamp;
    public int cars;
    public int trucks;
    public int motorcycles;
    public int total;
    public double cars_pct;
    public double trucks_pct;
    public double motorcycles_pct;
    public String congestion_level;
    public String source;

    public HistoricalDataPoint() {}

    @Override
    public String toString() {
        return String.format("HistoricalDataPoint{timestamp=%s, cars=%d, trucks=%d, motorcycles=%d, congestion=%s}",
                timestamp, cars, trucks, motorcycles, congestion_level);
    }
}
