from app.extensions import celery, db
from app.services import IncidentService, AiService, RagService, VoiceService, SlackService
from app.services.jira_service import JiraService
from app.repositories.incident_repo import IncidentRepository
from app.repositories.voice_repo import VoiceCallRepository
from app.models.incident import Incident
from app.models.enums import IncidentSeverity
from config import Config

def get_incident_service():
    # Factory for service instantiation inside tasks
    return IncidentService(
        incident_repo=IncidentRepository(),
        ai_service=AiService(RagService()),
        slack_service=SlackService(),
        voice_service=VoiceService(VoiceCallRepository()),
        jira_service=JiraService()
    )

@celery.task
def process_incident_task(incident_data):
    """Async task to process a new incident"""
    service = get_incident_service()
    service.process_incident(incident_data)

@celery.task
def schedule_outbound_call(incident_id):
    """Delayed outbound call task"""
    service = get_incident_service()
    incident = service.incident_repo.find_by_id(incident_id) # Using base repo find_by_id logic or use query
    if not incident:
         incident = db.session.get(Incident, incident_id)
    
    if incident and incident.severity == IncidentSeverity.HIGH:
        phone_number = Config.TWILIO_PHONE_NUMBER 
        if phone_number:
            service.voice_service.make_incident_notification_call(
                to_number=phone_number,
                incident_id=incident.external_id,
                severity=incident.severity.value,
                description=incident.description,
                ai_suggestion=incident.ai_suggestion
            )
