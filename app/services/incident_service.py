import logging
from typing import Optional, List, Dict, Any
from datetime import datetime, timedelta

from app.extensions import db
from app.models.incident import Incident
from app.models.enums import IncidentStatus, IncidentType, IncidentSeverity
from app.repositories.incident_repo import IncidentRepository
from app.services.ai_service import AiService
from app.services.slack_service import SlackService
from app.services.voice_service import VoiceService
from app.services.jira_service import JiraService

logger = logging.getLogger(__name__)

class IncidentService:
    def __init__(self, incident_repo: IncidentRepository, ai_service: AiService, 
                 slack_service: SlackService, voice_service: VoiceService,
                 jira_service: JiraService):
        self.incident_repo = incident_repo
        self.ai_service = ai_service
        self.slack_service = slack_service
        self.voice_service = voice_service
        self.jira_service = jira_service

    def process_incident(self, incident_data: Dict[str, Any]) -> Incident:
        external_id = incident_data.get('id')
        logger.info(f"Processing incident: {external_id}")

        # Check duplicate
        if self.incident_repo.exists_by_external_id(external_id):
            logger.warning(f"Incident {external_id} already exists")
            return self.incident_repo.find_by_external_id(external_id)

        # Create Incident
        incident = Incident(
            external_id=external_id,
            description=incident_data.get('description'),
            source=incident_data.get('source'),
            incident_timestamp=incident_data.get('timestamp') or datetime.utcnow(),
            metadata_=incident_data.get('metadata', {}),
            status=IncidentStatus.RECEIVED,
            type=IncidentType.OTHER,
            severity=IncidentSeverity.UNKNOWN
        )
        self.incident_repo.save(incident)

        # AI Classification (runs largely synchronously here, typically called by async task wrapper)
        classification = self.ai_service.classify_incident(incident)
        
        incident.severity = classification.get('severity', IncidentSeverity.UNKNOWN)
        incident.ai_confidence = classification.get('confidence', 0.0)
        incident.ai_reasoning = classification.get('reasoning', '')
        incident.ai_suggestion = classification.get('suggestion', '')
        
        # Infer type
        req_type = incident_data.get('type')
        if req_type:
             try:
                 incident.type = IncidentType(req_type)
             except:
                 pass

        incident.status = IncidentStatus.PROCESSING
        self.incident_repo.save(incident)
        
        # Integrations
        
        # 1. Jira Ticket Creation
        if self.jira_service:
            ticket_key = self.jira_service.create_incident_ticket(
                summary=f"Incident: {incident.external_id}",
                description=f"{incident.description}\n\nAI Analysis:\n{incident.ai_reasoning}\n\nSuggestion:\n{incident.ai_suggestion}",
                incident_type=incident.type.value,
                severity=incident.severity.value
            )
            if ticket_key:
                incident.jira_ticket_key = ticket_key
                logger.info(f"Created Jira ticket: {ticket_key}")
        
        # 2. Slack Notification
        channel_id = self.slack_service.create_incident_channel(incident)
        if channel_id:
            incident.metadata_['slack_channel_id'] = channel_id
            self.slack_service.post_incident_notification(channel_id, incident, incident.ai_suggestion)
            self.slack_service.notify_stakeholders(channel_id, incident)
            
            # Post Jira link to Slack if exists
            if incident.jira_ticket_key:
                self.slack_service.client.chat_postMessage(
                    channel=channel_id, 
                    text=f"🎫 *Jira Ticket Created:* {incident.jira_ticket_key}"
                )
                
            self.incident_repo.save(incident)

        # 3. Voice Call (Delayed)
        from app.tasks import schedule_outbound_call
        if incident.severity == IncidentSeverity.HIGH and incident.ai_suggestion:
             schedule_outbound_call.apply_async(args=[incident.id], countdown=60)

        return incident

    def get_incident_status(self, external_id: str) -> Optional[Dict]:
        incident = self.incident_repo.find_by_external_id(external_id)
        if not incident:
            return None
        return {
            "id": incident.external_id,
            "status": incident.status.value,
            "severity": incident.severity.value,
            "suggestion": incident.ai_suggestion,
            "jiraKey": incident.jira_ticket_key
        }

    def list_incidents(self, page: int = 1, per_page: int = 20, **filters):
        return self.incident_repo.find_by_criteria(**filters)

    def update_status(self, external_id: str, status: IncidentStatus) -> Optional[Incident]:
        incident = self.incident_repo.find_by_external_id(external_id)
        if not incident:
            return None
            
        old_status = incident.status
        incident.status = status
        if status in [IncidentStatus.RESOLVED, IncidentStatus.CLOSED]:
            incident.resolved_at = datetime.utcnow()
            
            # Archive Slack Channel
            channel_id = incident.metadata_.get('slack_channel_id')
            if channel_id:
                self.slack_service.client.chat_postMessage(channel=channel_id, text=f"Incident Resolved. Archiving channel.")
                self.slack_service.archive_channel(channel_id)

        self.incident_repo.save(incident)
        return incident
