#stream.py
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from database.db import get_db
from models.traffic import SystemStatus

router = APIRouter(prefix="/api/stream", tags=["stream"])


def _get_or_create_status(db: Session) -> SystemStatus:
    status = db.query(SystemStatus).filter_by(id=1).first()
    if not status:
        status = SystemStatus(
            id=1,
            backend_connected=True,
            database_connected=True,
            ai_model_loaded=False,
            stream_active=False,
        )
        db.add(status)
        db.commit()
        db.refresh(status)
    return status


# ---------------------------------------------------------------------------
# POST /api/stream/start
# ITrafficDashboardPresenter.onStartStreamClicked
# ---------------------------------------------------------------------------
@router.post("/start")
def start_stream(db: Session = Depends(get_db)):
    """
    Marks the stream as active in the database.
    The JavaFX presenter calls this when the user clicks Start Stream.
    """
    status = _get_or_create_status(db)
    status.stream_active = True
    status.backend_connected = True
    status.database_connected = True
    db.commit()
    return {
        "stream_active": True,
        "message": "Stream started. Backend is ready to receive traffic records.",
    }


# ---------------------------------------------------------------------------
# POST /api/stream/stop
# ITrafficDashboardPresenter.onStopStreamClicked
# ---------------------------------------------------------------------------
@router.post("/stop")
def stop_stream(db: Session = Depends(get_db)):
    """
    Marks the stream as inactive.
    The JavaFX presenter calls this when the user clicks Stop Stream.
    """
    status = _get_or_create_status(db)
    status.stream_active = False
    db.commit()
    return {
        "stream_active": False,
        "message": "Stream stopped.",
    }
