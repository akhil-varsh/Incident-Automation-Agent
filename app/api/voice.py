from flask import Blueprint, request, Response, jsonify
from app.services.voice_service import VoiceService
from app.repositories.voice_repo import VoiceCallRepository
from config import Config
import logging

bp = Blueprint('voice', __name__, url_prefix='/api/twilio/voice')
logger = logging.getLogger(__name__)

def get_service():
    return VoiceService(VoiceCallRepository())

@bp.route('/incoming', methods=['POST'])
def incoming_call():
    service = get_service()
    twiml = service.generate_answer_twiml()
    return Response(twiml, mimetype='application/xml')

@bp.route('/recording', methods=['POST'])
def recording_callback():
    recording_url = request.form.get('RecordingUrl')
    call_sid = request.form.get('CallSid')
    from_number = request.form.get('From')
    
    if not recording_url:
        return Response("<Response></Response>", mimetype='application/xml')

    # Async processing via Celery usually, but calling Service directly for now
    # Service implementation has simplified logic
    try:
        service = get_service()
        # In a real async setup: process_voice_task.delay(recording_url, call_sid, from_number)
        service.process_recording(recording_url, call_sid, from_number)
    except Exception as e:
        logger.error(f"Error processing recording: {e}")

    # Return thank you TwiML
    return Response("""<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Say voice="alice">Thank you. Your incident has been recorded.</Say>
    <Hangup />
</Response>""", mimetype='application/xml')

@bp.route('/status', methods=['POST'])
def call_status():
    call_sid = request.form.get('CallSid')
    status = request.form.get('CallStatus')
    logger.info(f"Call {call_sid} status: {status}")
    return Response("<Response></Response>", mimetype='application/xml')

@bp.route('/outbound-twiml', methods=['GET', 'POST'])
def outbound_twiml():
    service = get_service()
    incident_id = request.args.get('incidentId')
    # Fetch incident details if needed or use passed params
    # This matches VoiceService.make_incident_notification_call logic
    # For now, return generic or specific TwiML
    
    response = service.generate_notification_twiml(
        incident_id or "Unknown", 
        "High", 
        "Check Slack for details", 
        "Check Slack"
    )
    return Response(response, mimetype='application/xml')
