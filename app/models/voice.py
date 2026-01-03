from datetime import datetime
from typing import Optional
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Integer, Text, DateTime, ForeignKey, Numeric, func
from app.extensions import db
from app.models.enums import VoiceProcessingStatus

class VoiceCall(db.Model):
    __tablename__ = 'voice_calls'

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    incident_id: Mapped[Optional[int]] = mapped_column(ForeignKey('incidents.id'), index=True)
    
    conversation_uuid: Mapped[str] = mapped_column(String(100), unique=True, nullable=False, index=True)
    caller_number: Mapped[Optional[str]] = mapped_column(String(20), index=True)
    recording_url: Mapped[Optional[str]] = mapped_column(String(500))
    recording_sid: Mapped[Optional[str]] = mapped_column(String(100))
    transcription: Mapped[Optional[str]] = mapped_column(Text)
    call_duration: Mapped[Optional[int]] = mapped_column(Integer)
    
    processing_status: Mapped[VoiceProcessingStatus] = mapped_column(
        db.Enum(VoiceProcessingStatus), 
        default=VoiceProcessingStatus.RECEIVED, 
        nullable=False, 
        index=True
    )
    
    speech_to_text_service: Mapped[Optional[str]] = mapped_column(String(20))
    transcription_confidence: Mapped[Optional[float]] = mapped_column(Numeric(3, 2))
    error_message: Mapped[Optional[str]] = mapped_column(Text)

    # Outbound call fields
    call_sid: Mapped[Optional[str]] = mapped_column(String(100))
    phone_number: Mapped[Optional[str]] = mapped_column(String(20))
    direction: Mapped[Optional[str]] = mapped_column(String(10))
    call_type: Mapped[Optional[str]] = mapped_column(String(50))
    twilio_status: Mapped[Optional[str]] = mapped_column(String(20))
    duration_seconds: Mapped[Optional[int]] = mapped_column(Integer)
    ended_at: Mapped[Optional[datetime]] = mapped_column(DateTime)

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False, index=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now(), nullable=False)
    processed_at: Mapped[Optional[datetime]] = mapped_column(DateTime)

    # Relationships
    incident = relationship("Incident", backref="voice_calls")

    def __init__(self, conversation_uuid, caller_number=None, recording_url=None):
        self.conversation_uuid = conversation_uuid
        self.caller_number = caller_number
        self.recording_url = recording_url

    def set_processing_status(self, status: VoiceProcessingStatus):
        self.processing_status = status
        if status == VoiceProcessingStatus.PROCESSED and self.processed_at is None:
            self.processed_at = datetime.utcnow()
