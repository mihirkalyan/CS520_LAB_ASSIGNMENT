package com.anthem_traffic.model;

/**
 * Response model for report generation
 */
public class ReportResponse {
    public String report_id;
    public String status;
    public String file_url;
    public int row_count;

    public ReportResponse() {}

    @Override
    public String toString() {
        return String.format("ReportResponse{report_id=%s, status=%s, rows=%d}",
                report_id, status, row_count);
    }
}
