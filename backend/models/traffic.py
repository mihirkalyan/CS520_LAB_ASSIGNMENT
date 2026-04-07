#traffic.py

from database.db import Base
from sqlalchemy import Column, Integer, Float, String, Boolean, DateTime, func
from sqlalchemy.dialects.postgresql import UUID
import uuid


class TrafficRecord(Base):
    """
    One row per detection snapshot sent by the JavaFX frontend every ~2 seconds.
    Maps directly to the data produced in Main.java's mockDataTimer.
    """
    __tablename__ = "traffic_records"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    # Raw vehicle counts
    cars       = Column(Integer, nullable=False, default=0)
    trucks     = Column(Integer, nullable=False, default=0)
    motorcycles = Column(Integer, nullable=False, default=0)
    total      = Column(Integer, nullable=False, default=0)

    # Category percentages (0-100)
    cars_pct        = Column(Float, nullable=False, default=0.0)
    trucks_pct      = Column(Float, nullable=False, default=0.0)
    motorcycles_pct = Column(Float, nullable=False, default=0.0)

    # Derived alert level: "smooth" | "moderate" | "heavy"
    congestion_level = Column(String(10), nullable=False, default="smooth")

    # "frontend" (mock) or "ai" (future YOLOv8)
    source = Column(String(20), nullable=False, default="frontend")


class SystemStatus(Base):
    """
    Single-row table — always upsert row with id=1.
    Maps to updateSystemStatus(backendConnected, databaseConnected, aiModelLoaded).
    """
    __tablename__ = "system_status"

    id                  = Column(Integer, primary_key=True, default=1)
    backend_connected   = Column(Boolean, nullable=False, default=True)
    database_connected  = Column(Boolean, nullable=False, default=False)
    ai_model_loaded     = Column(Boolean, nullable=False, default=False)
    stream_active       = Column(Boolean, nullable=False, default=False)
    updated_at          = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())


class Report(Base):
    """Tracks generated reports so the frontend can poll for completion."""
    __tablename__ = "reports"

    id         = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    status     = Column(String(20), nullable=False, default="pending")   # pending | ready | failed
    file_url   = Column(String(500), nullable=True)
    from_ts    = Column(DateTime(timezone=True), nullable=True)
    to_ts      = Column(DateTime(timezone=True), nullable=True)
    row_count  = Column(Integer, nullable=True)
