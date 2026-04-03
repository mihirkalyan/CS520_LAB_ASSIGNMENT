import cv2
import os

# Create folder to store frames
os.makedirs("frames", exist_ok=True)

cap = cv2.VideoCapture(0)

frame_count = 0

while True:
    ret, frame = cap.read()

    if not ret:
        break

    cv2.imshow("Drone Stream (Simulator)", frame)

    # Save frame every 30 frames
    if frame_count % 30 == 0:
        filename = f"frames/frame_{frame_count}.jpg"
        cv2.imwrite(filename, frame)
        print(f"Saved {filename}")

    frame_count += 1

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()