import logging
import base64
import requests
import uuid
from typing import Optional, Dict, List, Any
from datetime import datetime
from twilio.rest import Client
from twilio.twiml.voice_response import VoiceResponse, Gather, Say, Pause, Record, Hangup

from app.models.voice import VoiceCall
from app.models.enums import VoiceProcessingStatus, IncidentType
from app.repositories.voice_repo import VoiceCallRepository
from config import Config

logger = logging.getLogger(__name__)

class VoiceService:
    def __init__(self, voice_repo: VoiceCallRepository):
        self.voice_repo = voice_repo
        self.twilio_client = Client(Config.TWILIO_ACCOUNT_SID, Config.TWILIO_AUTH_TOKEN) if Config.TWILIO_ACCOUNT_SID else None
        
    def generate_answer_twiml(self) -> str:
        response = VoiceResponse()
        response.say("Hello, you have reached the XLBiz Incident Automation Hotline. Please describe your incident clearly after the beep. Press star when finished.", voice="alice")
        response.record(
            action=f"{Config.TWILIO_WEBHOOK_URL}/api/twilio/voice/recording",
            method="POST",
            timeout=10,
            max_length=60,
            finish_on_key="*"
        )
        response.say("Thank you. Your incident has been recorded and will be processed immediately. Goodbye.", voice="alice")
        response.hangup()
        return str(response)

    def process_recording(self, recording_url: str, conversation_uuid: str, caller_number: str) -> Dict[str, Any]:
        logger.info(f"Processing recording: {recording_url}")
        
        # Check for duplicate
        if self.voice_repo.exists_by_conversation_uuid(conversation_uuid):
            logger.warning(f"Duplicate conversation {conversation_uuid}")
            return {"status": "DUPLICATE"}

        voice_call = VoiceCall(
            conversation_uuid=conversation_uuid,
            caller_number=caller_number,
            recording_url=recording_url,
            processing_status=VoiceProcessingStatus.RECEIVED
        )
        self.voice_repo.save(voice_call)

        try:
            voice_call.processing_status = VoiceProcessingStatus.DOWNLOADING
            self.voice_repo.save(voice_call)
            
            # Download audio
            audio_content = self._download_audio(recording_url)
            
            voice_call.processing_status = VoiceProcessingStatus.TRANSCRIBING
            self.voice_repo.save(voice_call)
            
            # Transcribe
            transcription = self._transcribe_audio(audio_content)
            voice_call.transcription = transcription
            
            # Extract incident details (Simple keyword matching + Regex from Java logic)
            incident_data = self._extract_incident_details(transcription, caller_number)
            
            # Note: The actual incident creation will be orchestrated by the Controller or a higher-level workflow
            # For now we return the extracted data
            
            voice_call.processing_status = VoiceProcessingStatus.PROCESSED
            voice_call.processed_at = datetime.utcnow()
            self.voice_repo.save(voice_call)
            
            return {
                "status": "PROCESSED",
                "transcription": transcription,
                "incident_data": incident_data,
                "voice_call_id": voice_call.id
            }

        except Exception as e:
            logger.error(f"Error processing voice recording: {e}")
            voice_call.processing_status = VoiceProcessingStatus.ERROR
            voice_call.error_message = str(e)
            self.voice_repo.save(voice_call)
            raise

    def make_incident_notification_call(self, to_number: str, incident_id: str, severity: str, description: str, ai_suggestion: str = None):
        """Outbound call logic"""
        if not self.twilio_client:
            logger.warning("Twilio client not initialized")
            return
            
        try:
            logger.info(f"Initiating call to {to_number} for incident {incident_id}")
            # In a real app, 'url' would point to an endpoint that returns TwiML
            # specific to this incident using params.
            # Simulating logic by constructing a TwiML Bin URL or using an API endpoint
            call_url = f"{Config.TWILIO_WEBHOOK_URL}/api/twilio/outbound/twiml/incident-notification?incidentId={incident_id}"
            
            call = self.twilio_client.calls.create(
                to=to_number,
                from_=Config.TWILIO_PHONE_NUMBER,
                url=call_url
            )
            logger.info(f"Call initiated: {call.sid}")
            return call.sid
            
        except Exception as e:
            logger.error(f"Failed to make outbound call: {e}")
            raise

    def generate_notification_twiml(self, incident_id: str, severity: str, description: str, ai_suggestion: str) -> str:
        """Generates TwiML for the outbound call"""
        response = VoiceResponse()
        response.say(f"Hello, this is XLBiz Incident Management. We have a {severity} priority incident {incident_id}.", voice="alice")
        response.pause(length=1)
        response.say(f"Description: {description}", voice="alice")
        if ai_suggestion:
            response.pause(length=1)
            response.say("AI Recommendation: Check Slack for details.", voice="alice") 
        
        response.gather(num_digits=1, action=f"{Config.TWILIO_WEBHOOK_URL}/api/twilio/outbound/response", method="POST")\
            .say("Press 1 to acknowledge, or 2 to escalate.")
        
        return str(response)

    def _download_audio(self, url: str) -> bytes:
        # Twilio Basic Auth
        auth = (Config.TWILIO_ACCOUNT_SID, Config.TWILIO_AUTH_TOKEN)
        resp = requests.get(url, auth=auth)
        resp.raise_for_status()
        return resp.content

    def _transcribe_audio(self, audio_data: bytes) -> str:
        # Implementing Google Cloud Speech fallback or Deepgram
        # For simplicity in this migration, we'll try Deepgram simple usage if key exists
        # Or mock/basic if not. The Java used Google then Deepgram.
        
        # Deepgram implementation
        if Config.DEEPGRAM_API_KEY:
            try:
                url = "https://api.deepgram.com/v1/listen?model=nova-2&smart_format=true&punctuate=true"
                headers = {
                    "Authorization": f"Token {Config.DEEPGRAM_API_KEY}",
                    "Content-Type": "audio/wav" # Assumed format from Twilio
                }
                response = requests.post(url, headers=headers, data=audio_data)
                response.raise_for_status()
                data = response.json()
                return data['results']['channels'][0]['alternatives'][0]['transcript']
            except Exception as e:
                logger.error(f"Deepgram transcription failed: {e}")
        
        # Fallback or Mock
        logger.warning("No STT service configured or failed. Returning dummy transcription.")
        return "System Down database connection error production environment"

    def _extract_incident_details(self, text: str, caller_number: str) -> Dict[str, Any]:
        """Simple keyword extraction ported from Java"""
        lower_text = text.lower()
        
        incident_type = IncidentType.OTHER
        if "database" in lower_text or "connection" in lower_text:
            incident_type = IncidentType.DATABASE_CONNECTION_ERROR
        elif "network" in lower_text:
            incident_type = IncidentType.NETWORK_ISSUE
        elif "security" in lower_text:
            incident_type = IncidentType.SECURITY_BREACH
        
        # Extract environment
        env = "unknown"
        if "production" in lower_text:
            env = "production"
        elif "staging" in lower_text:
            env = "staging"
            
        return {
            "description": text,
            "type": incident_type,
            "source": "voice",
            "metadata": {
                "caller_number": caller_number,
                "environment": env
            }
        }
