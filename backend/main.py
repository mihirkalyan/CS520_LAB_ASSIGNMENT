from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import DEBUG, ALLOWED_ORIGINS
from database.db import create_tables
from routes import traffic, stream, reports

app = FastAPI(
    title="Anthem AI Traffic Backend",
    description="Python backend for the Anthem Traffic Dashboard (MVP).",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
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
# Startup: ensure tables exist (safe no-op if already created via SQL seed)
# ---------------------------------------------------------------------------
@app.on_event("startup")
def on_startup():
    create_tables()
    print("✓ Database tables verified / created.")


# ---------------------------------------------------------------------------
# Routers
# ---------------------------------------------------------------------------
app.include_router(stream.router)    # /api/stream/start  /api/stream/stop
app.include_router(traffic.router)   # /api/traffic/live  /api/traffic/historical  /api/traffic/record
app.include_router(reports.router)   # /api/reports


# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------
@app.get("/health", tags=["health"])
def health():
    return {"status": "ok"}
