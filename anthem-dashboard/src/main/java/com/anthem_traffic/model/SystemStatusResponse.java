package com.anthem_traffic.model;

/**
 * Response model for system status
 */
public class SystemStatusResponse {
    public boolean backend_connected;
    public boolean database_connected;
    public boolean ai_model_loaded;
    public boolean stream_active;

    public SystemStatusResponse() {}

    @Override
    public String toString() {
        return String.format("SystemStatusResponse{backend=%s, database=%s, ai_model=%s, stream=%s}",
                backend_connected, database_connected, ai_model_loaded, stream_active);
    }
}
