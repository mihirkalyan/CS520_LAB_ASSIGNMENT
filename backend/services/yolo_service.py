#yolo_service.py
from ultralytics import YOLO
import cv2
import numpy as np




class YOLOService:
    def __init__(self, model_path: str = "models/yolov8n.pt"):
        self.model = YOLO(model_path)
    
    def detect_frame(self, frame):
        """
        frame: OpenCV image (numpy array)
        returns: list of detections
        """
        results = self.model(frame)

        detections = []

        for result in results:
            names = result.names
            boxes = result.boxes


            if boxes is None:
                continue

            for box in boxes:
                cls_id = int(box.cls[0].item())
                conf = float(box.conf[0].item())
                xyxy = box.xyxy[0].tolist()

                detections.append({
                    "class_id": cls_id,
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
        frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if frame is None:
            raise ValueError("Could not decode image.")

        return self.detect_frame(frame)


yolo_service = YOLOService()


