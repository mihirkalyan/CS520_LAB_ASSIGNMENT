#main.py
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import DEBUG, ALLOWED_ORIGINS
from database.db import create_tables, SessionLocal
from models.traffic import SystemStatus
from routes import traffic, stream, reports, video


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ------------------------------------------------------------------
    # STARTUP
    # ------------------------------------------------------------------
    create_tables()
    print("[OK] Database tables verified / created.")

    # Load YOLO model (import triggers singleton instantiation)
    try:
        from services.yolo_service import yolo_service  # noqa: F401
        model_loaded = True
    except Exception as e:
        print(f"[WARN] YOLO model failed to load: {e}")
        model_loaded = False

    # Mark ai_model_loaded in system_status
    db = SessionLocal()
    try:
        status = db.query(SystemStatus).filter_by(id=1).first()
        if not status:
            status = SystemStatus(id=1, backend_connected=True, database_connected=True)
            db.add(status)
        status.ai_model_loaded    = model_loaded
        status.backend_connected  = True
        status.database_connected = True
        db.commit()
    finally:
        db.close()

    # Start background video processor
    if model_loaded:
        try:
            from services.video_service import video_processor
            video_processor.start()
            print("[OK] Video processor started in background.")
        except Exception as e:
            print(f"[WARN] Video processor failed to start: {e}")

    yield

    # ------------------------------------------------------------------
    # SHUTDOWN
    # ------------------------------------------------------------------
    try:
        from services.video_service import video_processor
        video_processor.stop()
        print("[OK] Video processor stopped.")
    except Exception:
        pass


app = FastAPI(
    title="Anthem AI Traffic Backend",
    description="Python backend for the Anthem Traffic Dashboard (MVP).",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# ---------------------------------------------------------------------------
# CORS — allow the JavaFX HTTP client and browser dev tools
# ---------------------------------------------------------------------------
app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Routers
# ---------------------------------------------------------------------------
app.include_router(stream.router)    # /api/stream/start  /api/stream/stop
app.include_router(traffic.router)   # /api/traffic/live  /api/traffic/historical  /api/traffic/record
app.include_router(video.router)     # /api/video/frame
app.include_router(reports.router)   # /api/reports


# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------
@app.get("/health", tags=["health"])
def health():
    return {"status": "ok"}
