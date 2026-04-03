from ultralytics import YOLO
import cv2
import os

# Load YOLO model
model = YOLO("yolov8n.pt")

frames_path = r"D:\MSCS\4. Forth Sem\CS520 Software Engineering\Project\anthem-ai\frames"

for file in os.listdir(frames_path):
    if file.endswith(".jpg"):
        img_path = os.path.join(frames_path, file)

        # Run detection
        results = model(img_path)

        # Display result
        annotated_frame = results[0].plot()
        cv2.imshow("Vehicle Detection", annotated_frame)

        if cv2.waitKey(0) & 0xFF == ord('q'):
            break

cv2.destroyAllWindows()