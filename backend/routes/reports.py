#reports.py
import csv
import io
import uuid as _uuid
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, Query
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from database.db import get_db
from models.traffic import Report, TrafficRecord
from schemas.traffic import ReportResponse

router = APIRouter(prefix="/api/reports", tags=["reports"])


# ---------------------------------------------------------------------------
# POST /api/reports
# ITrafficDashboardPresenter.onGenerateReportClicked
# ---------------------------------------------------------------------------
@router.post("", response_model=ReportResponse, status_code=201)
def generate_report(
    from_ts: Optional[datetime] = Query(None, alias="from"),
    to_ts:   Optional[datetime] = Query(None, alias="to"),
    db: Session = Depends(get_db),
):
    """
    Generates an in-memory CSV report of all traffic records in the
    requested window (defaults to all time) and marks it ready immediately.
    For large datasets this would be replaced with a background Celery task.
    """
    query = db.query(TrafficRecord).order_by(TrafficRecord.timestamp)
    if from_ts:
        query = query.filter(TrafficRecord.timestamp >= from_ts)
    if to_ts:
        query = query.filter(TrafficRecord.timestamp <= to_ts)

    records = query.all()
    row_count = len(records)

    report = Report(
        status="ready",
        from_ts=from_ts,
        to_ts=to_ts,
        row_count=row_count,
    )
    db.add(report)
    db.commit()
    db.refresh(report)

    return ReportResponse(
        report_id=report.id,
        status="ready",
        file_url=f"/api/reports/{report.id}/download",
        row_count=row_count,
        message=f"Report ready with {row_count} records.",
    )


# ---------------------------------------------------------------------------
# GET /api/reports/{report_id}/download
# Returns CSV file for download
# ---------------------------------------------------------------------------
@router.get("/{report_id}/download")
def download_report(report_id: str, db: Session = Depends(get_db)):
    """Streams the CSV content back to the caller."""
    report = db.query(Report).filter_by(id=report_id).first()
    if not report:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Report not found.")

    query = db.query(TrafficRecord).order_by(TrafficRecord.timestamp)
    if report.from_ts:
        query = query.filter(TrafficRecord.timestamp >= report.from_ts)
    if report.to_ts:
        query = query.filter(TrafficRecord.timestamp <= report.to_ts)

    records = query.all()

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow([
        "timestamp", "cars", "trucks", "motorcycles", "total",
        "cars_pct", "trucks_pct", "motorcycles_pct", "congestion_level", "source"
    ])
    for r in records:
        writer.writerow([
            r.timestamp.isoformat(), r.cars, r.trucks, r.motorcycles, r.total,
            r.cars_pct, r.trucks_pct, r.motorcycles_pct, r.congestion_level, r.source
        ])

    output.seek(0)
    filename = f"traffic_report_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S')}.csv"
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": f"attachment; filename={filename}"},
    )
