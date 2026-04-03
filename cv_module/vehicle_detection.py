from ultralytics import YOLO
import cv2
import os
from collections import defaultdict

# Load YOLO model
model = YOLO("yolov8n.pt")

#Open camera
cap = cv2.VideoCapture(0)

unique_ids = set() #counts only unique objects

try:
    while True:
        ret, frame = cap.read()


        if not ret:
            break
        
        #Run YOLO detection
        results = model.track(frame, persist=True)[0]

        #Dictionary to count objects
        object_counts = defaultdict(int)

        #Loop through detected objects
        for box in results.boxes:
            cls = int(box.cls[0])
            label = model.names[cls]

            object_counts[label] += 1

            #unique tracking
            if box.id is not None:
                 obj_id = int(box.id[0])
                 unique_ids.add(obj_id)

            # Bounding box
            x1, y1, x2, y2 = map(int, box.xyxy[0])

            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(frame, label, (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

        #Display counts on screen
        y_offset = 30
        for obj, count in object_counts.items():
            text = f"{obj}: {count}"
            cv2.putText(frame, text, (10, y_offset),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
            y_offset += 30
        
        #total unique count
        total_unique = len(unique_ids)

        #traffic logic
        if total_unique < 5:
             traffic = "Low"
        elif total_unique < 15:
             traffic = "MEDIUM"
        else:
             traffic = "HIGH"

    
        #Display information
        cv2.putText(frame, f"Total Unique Objects: {total_unique}", 
                    (10, y_offset + 20), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 0, 0), 2)
        
        cv2.putText(frame, f"Traffic: {traffic}", (10, y_offset + 50), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 0, 0), 2)

        cv2.imshow("Real-Time Object Detection", frame)

        key = cv2.waitKey(10)
        if key == 27 or key == ord('q'): 
            print("Graceful exit...")
            break
        
except KeyboardInterrupt:
            print("Interrupted using Ctrl + C")

finally:
     cap.release()
     cv2.destroyAllWindows()
     print("Camera released and windows closed")