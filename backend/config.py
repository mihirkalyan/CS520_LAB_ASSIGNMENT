#config.py
import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL: str = os.getenv("DATABASE_URL", "").strip()
if not DATABASE_URL:
    raise RuntimeError(
        "DATABASE_URL is missing. Create backend/.env from backend/.env.example and set a valid PostgreSQL URI."
    )

APP_HOST: str = os.getenv("APP_HOST", "0.0.0.0")
APP_PORT: int = int(os.getenv("APP_PORT", "8000"))
DEBUG: bool = os.getenv("DEBUG", "true").lower() == "true"

# Congestion thresholds (must match JavaFX Main.java)
CONGESTION_HEAVY_THRESHOLD: int = 75
CONGESTION_MODERATE_THRESHOLD: int = 50

# CORS - allow JavaFX HTTP client + browser dev tools
ALLOWED_ORIGINS: list[str] = os.getenv(
    "ALLOWED_ORIGINS", "http://localhost,http://localhost:8000"
).split(",")

# ML / Video pipeline
# STREAM_URL: "0" = local webcam; set to rtsp://... for drone feed
STREAM_URL: str = os.getenv("STREAM_URL", "0").strip() or "0"
MODEL_PATH: str = os.getenv("MODEL_PATH", "models_weights/yolov8n.pt")
SAMPLE_EVERY_N_FRAMES: int = int(os.getenv("SAMPLE_EVERY_N_FRAMES", "10"))
