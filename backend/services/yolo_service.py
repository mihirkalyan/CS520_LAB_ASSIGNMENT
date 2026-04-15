#yolo_service.py
import cv2
import numpy as np
from ultralytics import YOLO

import config

# COCO class IDs that map to vehicle categories
VEHICLE_CLASSES = {
    2: "car",
    3: "motorcycle",
    5: "bus",        # treated as truck
    7: "truck",
}


class YOLOService:
    def __init__(self, model_path: str = None):
        path = model_path or config.MODEL_PATH
        self.model = YOLO(path)
        print(f"[OK] YOLO model loaded from {path}")

    def detect_frame(self, frame):
        """
        frame: OpenCV image (numpy array)
        returns: list of detection dicts (all classes, not filtered)
        """
        results = self.model(frame, verbose=False)
        detections = []

        for result in results:
            names = result.names
            boxes = result.boxes

            if boxes is None:
                continue

            for box in boxes:
                cls_id = int(box.cls[0].item())
                conf   = float(box.conf[0].item())
                xyxy   = box.xyxy[0].tolist()

                detections.append({
                    "class_id":   cls_id,
                    "class_name": names[cls_id],
                    "confidence": round(conf, 4),
                    "bbox": {
                        "x1": float(xyxy[0]),
                        "y1": float(xyxy[1]),
                        "x2": float(xyxy[2]),
                        "y2": float(xyxy[3]),
                    }
                })
        return detections

    def detect_image_bytes(self, image_bytes: bytes):
        np_arr = np.frombuffer(image_bytes, np.uint8)
        frame  = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if frame is None:
            raise ValueError("Could not decode image.")

        return self.detect_frame(frame)

    def count_vehicles(self, detections: list) -> dict:
        """
        Filter detections to vehicle classes and return aggregated counts.
        Returns: { cars, trucks, motorcycles, total }
        """
        cars = trucks = motorcycles = 0

        for d in detections:
            cls_id = d["class_id"]
            if cls_id == 2:                  # car
                cars += 1
            elif cls_id in (5, 7):           # bus or truck
                trucks += 1
            elif cls_id == 3:                # motorcycle
                motorcycles += 1

        return {
            "cars":        cars,
            "trucks":      trucks,
            "motorcycles": motorcycles,
            "total":       cars + trucks + motorcycles,
        }


yolo_service = YOLOService()
