#video_service.py
import cv2
import time
from services.yolo_service import yolo_service


class VideoProcessor:
    def __init__(self):
        self.latest_result = {
            "source": None,
            "frame_number": 0,
            "timestamp": None,
            "count": 0,
            "detections": []
        }
        self.is_running = False

    import cv2
import time
from services.yolo_service import yolo_service


class VideoProcessor:
    def __init__(self):
        self.latest_result = {
            "source": None,
            "frame_number": 0,
            "timestamp": None,
            "count": 0,
            "detections": []
        }
        self.is_running = False

    def process_video_source(self, source=0, sample_every_n_frames=10):
        cap = cv2.VideoCapture(source)

        if not cap.isOpened():
            raise RuntimeError(f"Could not open video source: {source}")

        self.is_running = True
        frame_number = 0

        try:
            while self.is_running:
                ret, frame = cap.read()
                if not ret:
                    break

                frame_number += 1

                if frame_number % sample_every_n_frames != 0:
                    continue

                detections = yolo_service.detect_frame(frame)

                self.latest_result = {
                    "source": str(source),
                    "frame_number": frame_number,
                    "timestamp": time.time(),
                    "count": len(detections),
                    "detections": detections
                }

        finally:
            cap.release()
            self.is_running = False

    def stop(self):
        self.is_running = False

    def get_latest_result(self):
        return self.latest_result


video_processor = VideoProcessor()

