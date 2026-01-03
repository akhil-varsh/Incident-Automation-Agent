from datetime import datetime
from typing import Optional
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy import String, Integer, Float, DateTime, Text, func
from app.extensions importdb
from app.models.enums import IncidentType, IncidentSeverity, IncidentStatus

class Incident(db.Model):
    __tablename__ = 'incidents'

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    external_id: Mapped[str] = mapped_column(String(255), unique=True, nullable=False, index=True)
    type: Mapped[IncidentType] = mapped_column(db.Enum(IncidentType), nullable=False, index=True)
    description: Mapped[Optional[str]] = mapped_column(Text)
    severity: Mapped[IncidentSeverity] = mapped_column(db.Enum(IncidentSeverity), default=IncidentSeverity.UNKNOWN, index=True)
    status: Mapped[IncidentStatus] = mapped_column(db.Enum(IncidentStatus), default=IncidentStatus.RECEIVED, nullable=False, index=True)
    source: Mapped[Optional[str]] = mapped_column(String(100), index=True)
    incident_timestamp: Mapped[Optional[datetime]] = mapped_column(DateTime)
    
    # Metadata
    metadata_: Mapped[dict] = mapped_column("metadata", JSONB, default=dict)

    # Integration tracking
    slack_channel_id: Mapped[Optional[str]] = mapped_column(String(50))
    slack_message_ts: Mapped[Optional[str]] = mapped_column(String(50))
    jira_ticket_key: Mapped[Optional[str]] = mapped_column(String(20))

    # AI analysis
    ai_suggestion: Mapped[Optional[str]] = mapped_column(Text)
    ai_reasoning: Mapped[Optional[str]] = mapped_column(Text)
    ai_confidence: Mapped[Optional[float]] = mapped_column(Float)

    # Voice call fields
    transcription: Mapped[Optional[str]] = mapped_column(Text)
    recording_url: Mapped[Optional[str]] = mapped_column(String(500))
    call_duration: Mapped[Optional[int]] = mapped_column(Integer)
    conversation_uuid: Mapped[Optional[str]] = mapped_column(String(100))

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False, index=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now(), nullable=False)
    resolved_at: Mapped[Optional[datetime]] = mapped_column(DateTime)

    def __init__(self, external_id, type_val, description, source, incident_timestamp=None):
        self.external_id = external_id
        self.type = type_val
        self.description = description
        self.source = source
        self.incident_timestamp = incident_timestamp or datetime.utcnow()

    def set_status(self, new_status: IncidentStatus):
        self.status = new_status
        if new_status == IncidentStatus.RESOLVED and self.resolved_at is None:
            self.resolved_at = datetime.utcnow()

    def is_ai_processed(self):
        return bool(self.ai_suggestion and self.ai_suggestion.strip())

    def to_dict(self):
        return {
            'id': self.id,
            'external_id': self.external_id,
            'type': self.type.value,
            'description': self.description,
            'severity': self.severity.value,
            'status': self.status.value,
            'source': self.source,
            'incident_timestamp': self.incident_timestamp.isoformat() if self.incident_timestamp else None,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }
