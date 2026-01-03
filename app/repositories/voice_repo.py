from typing import List, Optional, Any
from datetime import datetime
from sqlalchemy import func, case, desc
from app.extensions import db
from app.models.voice import VoiceCall
from app.models.enums import VoiceProcessingStatus
from app.repositories.base_repo import BaseRepository

class VoiceCallRepository(BaseRepository[VoiceCall]):
    def __init__(self):
        super().__init__(VoiceCall)

    def find_by_conversation_uuid(self, conversation_uuid: str) -> Optional[VoiceCall]:
        return db.session.query(VoiceCall).filter_by(conversation_uuid=conversation_uuid).first()

    def find_by_call_sid(self, call_sid: str) -> Optional[VoiceCall]:
        return db.session.query(VoiceCall).filter_by(call_sid=call_sid).first()

    def exists_by_conversation_uuid(self, conversation_uuid: str) -> bool:
        return db.session.query(db.literal(True)).filter(
            db.session.query(VoiceCall).filter_by(conversation_uuid=conversation_uuid).exists()
        ).scalar()

    def find_recent_calls(self, since: datetime) -> List[VoiceCall]:
        return db.session.query(VoiceCall).filter(
            VoiceCall.created_at >= since
        ).order_by(VoiceCall.created_at.desc()).all()

    def get_call_statistics(self, since: datetime) -> dict:
        result = db.session.query(
            func.count(VoiceCall.id).label('totalCalls'),
            func.count(case((VoiceCall.processing_status == VoiceProcessingStatus.PROCESSED, 1))).label('processedCalls'),
            func.count(case((VoiceCall.processing_status == VoiceProcessingStatus.ERROR, 1))).label('errorCalls'),
            func.avg(VoiceCall.call_duration).label('avgDuration')
        ).filter(VoiceCall.created_at >= since).first()

        return {
            'totalCalls': result[0],
            'processedCalls': result[1],
            'errorCalls': result[2],
            'avgDuration': float(result[3]) if result[3] else 0.0
        }

    def find_failed_calls(self) -> List[VoiceCall]:
        return db.session.query(VoiceCall).filter_by(
            processing_status=VoiceProcessingStatus.ERROR
        ).order_by(VoiceCall.created_at.desc()).all()
