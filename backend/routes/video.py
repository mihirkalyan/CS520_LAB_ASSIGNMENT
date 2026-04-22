#video.py
from fastapi import APIRouter
from fastapi.responses import Response
from services.video_service import video_processor

router = APIRouter(prefix="/api/video", tags=["video"])


@router.get("/frame")
def get_latest_frame():
    frame_bytes = video_processor.latest_frame_bytes
    if frame_bytes is None:
        return Response(status_code=204)
    return Response(content=frame_bytes, media_type="image/jpeg")
