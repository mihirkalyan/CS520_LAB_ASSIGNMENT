#video_service.py
import time
import threading
import cv2

import config
from database.db import SessionLocal
from models.traffic import DroneSession, RawDetection, TrafficSnapshot, SystemStatus
from services.yolo_service import yolo_service
from config import CONGESTION_HEAVY_THRESHOLD, CONGESTION_MODERATE_THRESHOLD


def _derive_congestion(total: int) -> str:
    if total > CONGESTION_HEAVY_THRESHOLD:
        return "heavy"
    elif total > CONGESTION_MODERATE_THRESHOLD:
        return "moderate"
    return "smooth"


def _calc_pcts(cars, trucks, motorcycles, total):
    if total == 0:
        return 0.0, 0.0, 0.0
    return (
        round((cars * 100.0) / total, 2),
        round((trucks * 100.0) / total, 2),
        round((motorcycles * 100.0) / total, 2),
    )


class VideoProcessor:
    def __init__(self):
        self.is_running   = False
        self.session_id   = None
        self._thread      = None
        self.latest_result = {
            "source":       None,
            "frame_number": 0,
            "timestamp":    None,
            "count":        0,
            "detections":   [],
        }

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def start(self, source=None):
        """
        Start video processing in a background daemon thread.
        source: int (webcam index) or str (file path / RTSP URL).
        Defaults to config.STREAM_URL.
        """
        if self.is_running:
            self.stop()

        raw_source = source if source is not None else config.STREAM_URL
        # Convert "0" string from .env to integer for webcam
        try:
            resolved_source = int(raw_source)
        except (ValueError, TypeError):
            resolved_source = raw_source

        self._thread = threading.Thread(
            target=self._run,
            args=(resolved_source,),
            daemon=True,
        )
        self._thread.start()

    def stop(self):
        self.is_running = False
        self._close_session()

    def get_latest_result(self):
        return self.latest_result

    # ------------------------------------------------------------------
    # Internal
    # ------------------------------------------------------------------

    def _run(self, source):
        db = SessionLocal()
        try:
            # Open a new drone session row
            session = DroneSession(stream_url=str(source), status="active")
            db.add(session)
            db.commit()
            db.refresh(session)
            self.session_id = session.id

            # Mark stream active in system_status
            status_row = db.query(SystemStatus).filter_by(id=1).first()
            if status_row:
                status_row.stream_active = True
                status_row.stream_url    = str(source)
                db.commit()

            cap = cv2.VideoCapture(source)
            if not cap.isOpened():
                print(f"[ERROR] Could not open video source: {source}")
                session.status = "error"
                db.commit()
                return

            self.is_running = True
            frame_number    = 0
            print(f"[OK] Video processor started — source: {source}")

            while self.is_running:
                ret, frame = cap.read()
                if not ret:
                    print("[INFO] Video source ended or no frame received.")
                    break

                frame_number += 1
                if frame_number % config.SAMPLE_EVERY_N_FRAMES != 0:
                    continue

                detections = yolo_service.detect_frame(frame)
                counts     = yolo_service.count_vehicles(detections)

                # Persist raw per-vehicle detections
                for d in detections:
                    if d["class_id"] not in (2, 3, 5, 7):
                        continue  # skip non-vehicle classes
                    db.add(RawDetection(
                        session_id   = self.session_id,
                        frame_number = frame_number,
                        class_name   = d["class_name"],
                        class_id     = d["class_id"],
                        confidence   = d["confidence"],
                        bbox_x1      = d["bbox"]["x1"],
                        bbox_y1      = d["bbox"]["y1"],
                        bbox_x2      = d["bbox"]["x2"],
                        bbox_y2      = d["bbox"]["y2"],
                    ))

                # Persist aggregated snapshot
                cars_pct, trucks_pct, moto_pct = _calc_pcts(
                    counts["cars"], counts["trucks"], counts["motorcycles"], counts["total"]
                )
                db.add(TrafficSnapshot(
                    session_id       = self.session_id,
                    cars             = counts["cars"],
                    trucks           = counts["trucks"],
                    motorcycles      = counts["motorcycles"],
                    total            = counts["total"],
                    cars_pct         = cars_pct,
                    trucks_pct       = trucks_pct,
                    motorcycles_pct  = moto_pct,
                    congestion_level = _derive_congestion(counts["total"]),
                    source           = "ai",
                ))
                db.commit()

                self.latest_result = {
                    "source":       str(source),
                    "frame_number": frame_number,
                    "timestamp":    time.time(),
                    "count":        counts["total"],
                    "detections":   detections,
                }

        except Exception as e:
            print(f"[ERROR] Video processor exception: {e}")
        finally:
            cap.release() if 'cap' in dir() else None
            self.is_running = False
            self._close_session(db)
            db.close()

    def _close_session(self, db=None):
        if self.session_id is None:
            return
        close_db = db is None
        if close_db:
            db = SessionLocal()
        try:
            session = db.query(DroneSession).filter_by(id=self.session_id).first()
            if session:
                session.status   = "stopped"
                session.ended_at = __import__("datetime").datetime.now(__import__("datetime").timezone.utc)
                db.commit()

            status_row = db.query(SystemStatus).filter_by(id=1).first()
            if status_row:
                status_row.stream_active = False
                db.commit()
        except Exception:
            pass
        finally:
            if close_db:
                db.close()
        self.session_id = None


video_processor = VideoProcessor()
