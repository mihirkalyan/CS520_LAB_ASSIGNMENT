#traffic.py

from database.db import Base
from sqlalchemy import Column, Integer, Float, String, Boolean, DateTime, ForeignKey, func
from sqlalchemy.dialects.postgresql import UUID
import uuid


class DroneSession(Base):
    """
    One row per drone / stream session.
    Created when the video processor starts, closed when it stops.
    """
    __tablename__ = "drone_sessions"

    id         = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    stream_url = Column(String, nullable=False)
    started_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    ended_at   = Column(DateTime(timezone=True), nullable=True)
    status     = Column(String(20), nullable=False, default="active")  # active | stopped | error


class RawDetection(Base):
    """
    One row per vehicle detected per sampled frame.
    Ground-truth output from YOLO — useful for auditing and retraining.
    """
    __tablename__ = "raw_detections"

    id           = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id   = Column(UUID(as_uuid=True), ForeignKey("drone_sessions.id", ondelete="CASCADE"), nullable=True)
    frame_number = Column(Integer, nullable=False)
    detected_at  = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)
    class_name   = Column(String(50), nullable=False)
    class_id     = Column(Integer, nullable=False)
    confidence   = Column(Float, nullable=False)
    bbox_x1      = Column(Float, nullable=False)
    bbox_y1      = Column(Float, nullable=False)
    bbox_x2      = Column(Float, nullable=False)
    bbox_y2      = Column(Float, nullable=False)


class TrafficSnapshot(Base):
    """
    Aggregated vehicle counts per ~2-second window.
    This is the canonical table the frontend reads via /api/traffic/live
    and /api/traffic/historical. Same shape as the old traffic_records table.
    """
    __tablename__ = "traffic_snapshots"

    id         = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id = Column(UUID(as_uuid=True), ForeignKey("drone_sessions.id", ondelete="SET NULL"), nullable=True)
    timestamp  = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    # Raw vehicle counts
    cars        = Column(Integer, nullable=False, default=0)
    trucks      = Column(Integer, nullable=False, default=0)
    motorcycles = Column(Integer, nullable=False, default=0)
    total       = Column(Integer, nullable=False, default=0)

    # Category percentages (0-100)
    cars_pct        = Column(Float, nullable=False, default=0.0)
    trucks_pct      = Column(Float, nullable=False, default=0.0)
    motorcycles_pct = Column(Float, nullable=False, default=0.0)

    # Derived alert level: "smooth" | "moderate" | "heavy"
    congestion_level = Column(String(10), nullable=False, default="smooth")

    # "frontend" (mock) or "ai" (YOLO pipeline)
    source = Column(String(20), nullable=False, default="ai")


class SystemStatus(Base):
    """
    Single-row table — always upsert row with id=1.
    Maps to updateSystemStatus(backendConnected, databaseConnected, aiModelLoaded).
    """
    __tablename__ = "system_status"

    id                 = Column(Integer, primary_key=True, default=1)
    backend_connected  = Column(Boolean, nullable=False, default=True)
    database_connected = Column(Boolean, nullable=False, default=False)
    ai_model_loaded    = Column(Boolean, nullable=False, default=False)
    stream_active      = Column(Boolean, nullable=False, default=False)
    updated_at         = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())


class Report(Base):
    """Tracks generated reports so the frontend can poll for completion."""
    __tablename__ = "reports"

    id           = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    created_at   = Column(DateTime(timezone=True), server_default=func.now())
    status       = Column(String(20), nullable=False, default="pending")  # pending | ready | failed
    file_url     = Column(String(500), nullable=True)
    from_ts      = Column(DateTime(timezone=True), nullable=True)
    to_ts        = Column(DateTime(timezone=True), nullable=True)
    row_count    = Column(Integer, nullable=True)
