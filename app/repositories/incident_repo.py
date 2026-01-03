from typing import List, Optional, Any
from datetime import datetime
from sqlalchemy import or_, and_, func, case, desc, cast, String
from sqlalchemy.orm import Query
from app.extensions import db
from app.models.incident import Incident
from app.models.enums import IncidentType, IncidentSeverity, IncidentStatus
from app.repositories.base_repo import BaseRepository

class IncidentRepository(BaseRepository[Incident]):
    def __init__(self):
        super().__init__(Incident)

    def find_by_external_id(self, external_id: str) -> Optional[Incident]:
        return db.session.query(Incident).filter_by(external_id=external_id).first()

    def exists_by_external_id(self, external_id: str) -> bool:
        return db.session.query(db.literal(True)).filter(
            db.session.query(Incident).filter_by(external_id=external_id).exists()
        ).scalar()

    def find_by_type(self, type_val: IncidentType) -> List[Incident]:
        return db.session.query(Incident).filter_by(type=type_val).all()

    def find_active_incidents(self) -> List[Incident]:
        return db.session.query(Incident).filter(
            Incident.status.notin_([IncidentStatus.RESOLVED, IncidentStatus.CLOSED, IncidentStatus.FAILED])
        ).order_by(Incident.created_at.desc()).all()

    def find_recent_incidents(self, since: datetime) -> List[Incident]:
        return db.session.query(Incident).filter(
            Incident.created_at >= since
        ).order_by(Incident.created_at.desc()).all()

    def find_by_criteria(self, type_val: Optional[IncidentType] = None, 
                        severity: Optional[IncidentSeverity] = None, 
                        status: Optional[IncidentStatus] = None, 
                        source: Optional[str] = None) -> List[Incident]:
        query = db.session.query(Incident)
        if type_val:
            query = query.filter(Incident.type == type_val)
        if severity:
            query = query.filter(Incident.severity == severity)
        if status:
            query = query.filter(Incident.status == status)
        if source:
            query = query.filter(Incident.source == source)
        return query.order_by(Incident.created_at.desc()).all()

    def get_incident_statistics(self, since: datetime) -> dict:
        # returns total, active, high_severity, resolved, recent
        result = db.session.query(
            func.count(Incident.id).label('total'),
            func.count(case((
                Incident.status.in_([IncidentStatus.RECEIVED, IncidentStatus.CLASSIFYING, 
                                   IncidentStatus.PROCESSING, IncidentStatus.IN_PROGRESS]), 1
            ))).label('active'),
            func.count(case((Incident.severity == IncidentSeverity.HIGH, 1))).label('high_severity'),
            func.count(case((Incident.status == IncidentStatus.RESOLVED, 1))).label('resolved'),
            func.count(case((Incident.created_at >= since, 1))).label('recent')
        ).first()
        
        return {
            'total': result[0],
            'active': result[1],
            'high_severity': result[2],
            'resolved': result[3],
            'recent': result[4]
        }

    def find_incidents_requiring_attention(self, threshold: datetime) -> List[Incident]:
        return db.session.query(Incident).filter(
            or_(
                and_(
                    Incident.severity == IncidentSeverity.HIGH,
                    Incident.status.in_([IncidentStatus.RECEIVED, IncidentStatus.CLASSIFYING, 
                                       IncidentStatus.PROCESSING, IncidentStatus.IN_PROGRESS])
                ),
                and_(
                    Incident.status.in_([IncidentStatus.RECEIVED, IncidentStatus.CLASSIFYING, 
                                       IncidentStatus.PROCESSING]),
                    Incident.created_at < threshold
                )
            )
        ).order_by(Incident.severity.desc(), Incident.created_at.asc()).all()

    def find_by_metadata_field(self, key: str, value: str) -> List[Incident]:
        return db.session.query(Incident).filter(
            Incident.metadata_[key].astext == value
        ).order_by(Incident.created_at.desc()).all()

    def find_similar_incidents(self, type_val: IncidentType, keyword: str) -> List[Incident]:
        keyword_pattern = f"%{keyword}%"
        return db.session.query(Incident).filter(
            Incident.type == type_val,
            or_(
                Incident.description.ilike(keyword_pattern),
                Incident.ai_suggestion.ilike(keyword_pattern)
            )
        ).order_by(Incident.created_at.desc()).all()

    def find_unresolved(self) -> List[Incident]:
        return self.find_active_incidents()
