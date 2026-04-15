#traffic.py
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import desc
from datetime import datetime, timezone
from typing import Optional

from database.db import get_db
from models.traffic import TrafficSnapshot, SystemStatus
from schemas.traffic import (
    TrafficRecordCreate,
    LiveStreamResponse,
    VehicleCountResponse,
    CategoryDistributionResponse,
    TrafficTrendPoint,
    CongestionAlertResponse,
    SystemStatusResponse,
    HistoricalDataPoint,
)
from config import CONGESTION_HEAVY_THRESHOLD, CONGESTION_MODERATE_THRESHOLD

router = APIRouter(prefix="/api/traffic", tags=["traffic"])


def _derive_congestion(total: int) -> str:
    if total > CONGESTION_HEAVY_THRESHOLD:
        return "heavy"
    elif total > CONGESTION_MODERATE_THRESHOLD:
        return "moderate"
    return "smooth"


def _calc_pcts(cars: int, trucks: int, motorcycles: int, total: int):
    if total == 0:
        return 0.0, 0.0, 0.0
    return (
        round((cars * 100.0) / total, 2),
        round((trucks * 100.0) / total, 2),
        round((motorcycles * 100.0) / total, 2),
    )


# ---------------------------------------------------------------------------
# POST /api/traffic/record
# Accepts snapshots from the JavaFX frontend (mock/frontend data path).
# The AI pipeline writes directly via video_service — this endpoint stays
# for backward compatibility and manual testing.
# ---------------------------------------------------------------------------
@router.post("/record", status_code=201)
def record_traffic(payload: TrafficRecordCreate, db: Session = Depends(get_db)):
    cars_pct, trucks_pct, moto_pct = _calc_pcts(
        payload.cars, payload.trucks, payload.motorcycles, payload.total
    )
    record = TrafficSnapshot(
        cars=payload.cars,
        trucks=payload.trucks,
        motorcycles=payload.motorcycles,
        total=payload.total,
        cars_pct=cars_pct,
        trucks_pct=trucks_pct,
        motorcycles_pct=moto_pct,
        congestion_level=_derive_congestion(payload.total),
        source=payload.source,
    )
    db.add(record)
    db.commit()
    db.refresh(record)
    return {"id": str(record.id), "congestion_level": record.congestion_level}


# ---------------------------------------------------------------------------
# GET /api/traffic/live
# Frontend polls this every 2 seconds when stream is active.
# ---------------------------------------------------------------------------
@router.get("/live", response_model=LiveStreamResponse)
def get_live(db: Session = Depends(get_db)):
    latest: Optional[TrafficSnapshot] = (
        db.query(TrafficSnapshot)
        .order_by(desc(TrafficSnapshot.timestamp))
        .first()
    )
    status: Optional[SystemStatus] = db.query(SystemStatus).filter_by(id=1).first()

    if not latest:
        return LiveStreamResponse(
            vehicle_counts=VehicleCountResponse(cars=0, trucks=0, motorcycles=0, total=0),
            category_distribution=CategoryDistributionResponse(cars_pct=0, trucks_pct=0, motorcycles_pct=0),
            trend=TrafficTrendPoint(timestamp="--:--:--", cars=0, trucks=0, motorcycles=0),
            congestion=CongestionAlertResponse(severity="smooth"),
            system_status=SystemStatusResponse(
                backend_connected=True,
                database_connected=status is not None,
                ai_model_loaded=status.ai_model_loaded if status else False,
                stream_active=status.stream_active if status else False,
            ),
        )

    ts_str = latest.timestamp.astimezone(timezone.utc).strftime("%H:%M:%S")

    return LiveStreamResponse(
        vehicle_counts=VehicleCountResponse(
            cars=latest.cars, trucks=latest.trucks,
            motorcycles=latest.motorcycles, total=latest.total,
        ),
        category_distribution=CategoryDistributionResponse(
            cars_pct=latest.cars_pct,
            trucks_pct=latest.trucks_pct,
            motorcycles_pct=latest.motorcycles_pct,
        ),
        trend=TrafficTrendPoint(
            timestamp=ts_str,
            cars=latest.cars, trucks=latest.trucks, motorcycles=latest.motorcycles,
        ),
        congestion=CongestionAlertResponse(severity=latest.congestion_level),
        system_status=SystemStatusResponse(
            backend_connected=True,
            database_connected=True,
            ai_model_loaded=status.ai_model_loaded if status else False,
            stream_active=status.stream_active if status else False,
        ),
    )


# ---------------------------------------------------------------------------
# GET /api/traffic/historical
# ---------------------------------------------------------------------------
@router.get("/historical", response_model=list[HistoricalDataPoint])
def get_historical(
    from_ts: datetime = Query(..., alias="from"),
    to_ts:   datetime = Query(..., alias="to"),
    limit:   int      = Query(500, le=2000),
    db: Session = Depends(get_db),
):
    records = (
        db.query(TrafficSnapshot)
        .filter(TrafficSnapshot.timestamp >= from_ts, TrafficSnapshot.timestamp <= to_ts)
        .order_by(TrafficSnapshot.timestamp)
        .limit(limit)
        .all()
    )
    return [
        HistoricalDataPoint(
            timestamp=r.timestamp,
            cars=r.cars,
            trucks=r.trucks,
            motorcycles=r.motorcycles,
            total=r.total,
            congestion_level=r.congestion_level,
        )
        for r in records
    ]


# ---------------------------------------------------------------------------
# GET /api/traffic/system/status
# ---------------------------------------------------------------------------
@router.get("/system/status", response_model=SystemStatusResponse)
def get_system_status(db: Session = Depends(get_db)):
    status = db.query(SystemStatus).filter_by(id=1).first()
    if not status:
        return SystemStatusResponse(
            backend_connected=True, database_connected=True,
            ai_model_loaded=False, stream_active=False,
        )
    return SystemStatusResponse(
        backend_connected=status.backend_connected,
        database_connected=status.database_connected,
        ai_model_loaded=status.ai_model_loaded,
        stream_active=status.stream_active,
    )
