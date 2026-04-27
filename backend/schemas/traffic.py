#traffic.py
from pydantic import BaseModel, Field, UUID4
from datetime import datetime
from typing import Literal, Optional


# ---------------------------------------------------------------------------
# Inbound (frontend → backend)
# ---------------------------------------------------------------------------

class TrafficRecordCreate(BaseModel):
    """
    Payload the JavaFX frontend POSTs each tick (~every 2 seconds).
    Field names intentionally match Main.java variable names.
    """
    cars:        int   = Field(..., ge=0)
    trucks:      int   = Field(..., ge=0)
    motorcycles: int   = Field(..., ge=0)
    total:       int   = Field(..., ge=0)
    source:      str   = Field("frontend")


# ---------------------------------------------------------------------------
# Outbound (backend → frontend)
# ---------------------------------------------------------------------------

class VehicleCountResponse(BaseModel):
    cars:        int
    trucks:      int
    motorcycles: int
    total:       int


class CategoryDistributionResponse(BaseModel):
    cars_pct:        float
    trucks_pct:      float
    motorcycles_pct: float


class TrafficTrendPoint(BaseModel):
    timestamp:   str    # "HH:mm:ss" to match existing view method signature
    cars:        int
    trucks:      int
    motorcycles: int


class CongestionAlertResponse(BaseModel):
    severity: Literal["smooth", "moderate", "heavy"]


class SystemStatusResponse(BaseModel):
    backend_connected:  bool
    database_connected: bool
    ai_model_loaded:    bool
    stream_active:      bool


class LiveStreamResponse(BaseModel):
    """
    Combined payload for GET /api/traffic/live.
    The JavaFX presenter calls one view method per field group.
    """
    vehicle_counts:       VehicleCountResponse
    category_distribution: CategoryDistributionResponse
    trend:                TrafficTrendPoint
    congestion:           CongestionAlertResponse
    system_status:        SystemStatusResponse


class HistoricalDataPoint(BaseModel):
    timestamp:        datetime
    cars:             int
    trucks:           int
    motorcycles:      int
    total:            int
    congestion_level: str
    cars_pct:         float = 0.0
    trucks_pct:       float = 0.0
    motorcycles_pct:  float = 0.0
    source:           str = "ai"


class ReportResponse(BaseModel):
    report_id: UUID4
    status:    str
    file_url:  Optional[str] = None
    row_count: Optional[int] = None
    message:   str
