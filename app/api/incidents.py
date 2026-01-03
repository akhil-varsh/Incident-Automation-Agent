from flask import Blueprint, request, jsonify
from app.services.incident_service import IncidentService
from app.repositories.incident_repo import IncidentRepository
from app.services.ai_service import AiService
from app.services.rag_service import RagService
from app.services.voice_service import VoiceService
from app.services.slack_service import SlackService
from app.services.jira_service import JiraService
from app.repositories.voice_repo import VoiceCallRepository
from app.models.enums import IncidentStatus, IncidentType, IncidentSeverity
from app.tasks import process_incident_task

bp = Blueprint('incidents', __name__, url_prefix='/api/v1/incidents')

def get_service():
    return IncidentService(
        incident_repo=IncidentRepository(),
        ai_service=AiService(RagService()),
        slack_service=SlackService(),
        voice_service=VoiceService(VoiceCallRepository()),
        jira_service=JiraService()
    )

@bp.route('', methods=['POST'])
def create_incident():
    data = request.json
    try:
        # ASYNC PATTERN: "Sub-200ms"
        # Validate critical fields first?
        if not data.get('id'):
            return jsonify({"error": "Missing ID"}), 400

        # Dispatch task to Celery
        process_incident_task.delay(data)
        
        # Return 202 Accepted immediately
        return jsonify({
            "status": "ACCEPTED",
            "message": "Incident received and processing started in background",
            "externalId": data.get('id')
        }), 202

    except Exception as e:
        return jsonify({
            "status": "FAILED", 
            "message": str(e), 
            "externalId": data.get('id')
        }), 400

@bp.route('/<external_id>/status', methods=['GET'])
def get_incident_status(external_id):
    service = get_service()
    status = service.get_incident_status(external_id)
    if status:
        return jsonify(status)
    return jsonify({"error": "Incident not found"}), 404

@bp.route('', methods=['GET'])
def list_incidents():
    service = get_service()
    filters = {}
    if request.args.get('type'):
        try:
             filters['type'] = IncidentType(request.args.get('type').upper())
        except ValueError: pass
    if request.args.get('severity'):
        try:
            filters['severity'] = IncidentSeverity(request.args.get('severity').upper())
        except ValueError: pass
    if request.args.get('status'):
        try:
            filters['status'] = IncidentStatus(request.args.get('status').upper())
        except ValueError: pass
    if request.args.get('source'):
        filters['source'] = request.args.get('source')

    incidents = service.list_incidents(**filters)
    
    results = []
    for inc in incidents:
        results.append({
            "id": inc.external_id,
            "description": inc.description,
            "status": inc.status.value,
            "severity": inc.severity.value,
            "type": inc.type.value,
            "createdAt": inc.created_at.isoformat()
        })
    
    return jsonify({
        "content": results,
        "totalElements": len(results),
        "totalPages": 1
    })

@bp.route('/stats', methods=['GET'])
def get_stats():
    repo = IncidentRepository()
    return jsonify(repo.get_statistics_dict())
