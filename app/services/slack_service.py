import logging
from typing import List, Optional, Dict, Any
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError
from app.models.incident import Incident
from app.models.enums import IncidentSeverity, IncidentType
from config import Config

logger = logging.getLogger(__name__)

class SlackService:
    def __init__(self):
        self.client = WebClient(token=Config.SLACK_BOT_TOKEN)
        self.channel_cache = {}
        
        # Stakeholder mappings (Simplification of Java map)
        self.type_stakeholders = {
            IncidentType.DATABASE_CONNECTION_ERROR: ["database-team", "backend-team"],
            IncidentType.SECURITY_BREACH: ["security-team"],
            IncidentType.NETWORK_ISSUE: ["network-team"]
        }
        self.severity_stakeholders = {
            IncidentSeverity.HIGH: ["incident-commander"],
            IncidentSeverity.MEDIUM: ["team-leads"]
        }

    def create_incident_channel(self, incident: Incident) -> Optional[str]:
        channel_name = f"incident-{incident.id}"[:21] # Slack 21 char limit for some, but typically 80. Java code did substring(0,8)
        # Java logic: incident-{short_id}
        short_id = str(incident.id)[:8]
        channel_name = f"incident-{short_id}"
        
        if incident.id in self.channel_cache:
            return self.channel_cache[incident.id]

        try:
            # Check if channel exists (optional, create throws if exists)
            response = self.client.conversations_create(name=channel_name, is_private=False)
            channel_id = response['channel']['id']
            
            # Set topic
            topic = f"Incident: {incident.external_id or incident.id} | Type: {incident.type.value} | Severity: {incident.severity.value}"
            self.client.conversations_setTopic(channel=channel_id, topic=topic)
            
            self.channel_cache[incident.id] = channel_id
            return channel_id
            
        except SlackApiError as e:
            if e.response['error'] == 'name_taken':
                logger.info(f"Channel {channel_name} already exists. Finding ID...")
                # In real app, search channels to find ID. For now return None or implement search
                pass
            logger.error(f"Error creating channel: {e}")
            return None

    def post_incident_notification(self, channel_id: str, incident: Incident, ai_suggestion: str = None):
        try:
            severity_emoji = "🔥" if incident.severity == IncidentSeverity.HIGH else "⚠️"
            blocks = [
                {
                    "type": "header",
                    "text": {
                        "type": "plain_text",
                        "text": f"{severity_emoji} NEW INCIDENT ALERT",
                        "emoji": True
                    }
                },
                {
                    "type": "section",
                    "fields": [
                        {"type": "mrkdwn", "text": f"*Type:*\n{incident.type.value}"},
                        {"type": "mrkdwn", "text": f"*Severity:*\n{incident.severity.value}"},
                        {"type": "mrkdwn", "text": f"*Source:*\n{incident.source}"},
                        {"type": "mrkdwn", "text": f"*Created:*\n{incident.created_at}"}
                    ]
                },
                {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": f"*Description:*\n{incident.description}"
                    }
                }
            ]
            
            if ai_suggestion:
                blocks.append({
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": f"*🤖 AI Analysis & Suggestions:*\n{ai_suggestion}"
                    }
                })
                
            self.client.chat_postMessage(channel=channel_id, blocks=blocks, text=f"New Incident: {incident.description}")
            return True
            
        except SlackApiError as e:
            logger.error(f"Error posting notification: {e}")
            return False

    def notify_stakeholders(self, channel_id: str, incident: Incident):
        stakeholders = set()
        stakeholders.update(self.type_stakeholders.get(incident.type, []))
        stakeholders.update(self.severity_stakeholders.get(incident.severity, []))
        
        if not stakeholders:
            return

        mentions = " ".join([f"@{s}" for s in stakeholders])
        try:
            self.client.chat_postMessage(
                channel=channel_id,
                text=f"📢 *Notifying Stakeholders:* {mentions}\nPlease review the incident above."
            )
        except SlackApiError as e:
            logger.error(f"Error notifying stakeholders: {e}")

    def archive_channel(self, channel_id: str):
        try:
            self.client.conversations_archive(channel=channel_id)
            return True
        except SlackApiError as e:
            logger.error(f"Error archiving channel: {e}")
            return False
