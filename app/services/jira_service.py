import logging
import os
from typing import Optional, Dict
from jira import JIRA

class JiraService:
    def __init__(self):
        self.jira = None
        self.project_key = os.getenv('JIRA_PROJECT_KEY', 'INC')
        
        try:
            url = os.getenv('JIRA_URL')
            user = os.getenv('JIRA_USER')
            token = os.getenv('JIRA_API_TOKEN')
            
            if url and user and token:
                self.jira = JIRA(server=url, basic_auth=(user, token))
            else:
                logging.getLogger(__name__).warning("JIRA credentials incomplete")
        except Exception as e:
            logging.getLogger(__name__).error(f"Failed to init JIRA client: {e}")

    def create_incident_ticket(self, summary: str, description: str, incident_type: str, severity: str) -> Optional[str]:
        if not self.jira:
            return None
            
        try:
            issue_dict = {
                'project': {'key': self.project_key},
                'summary': f"[{severity}] {summary[:50]}...",
                'description': f"Incident Type: {incident_type}\nSeverity: {severity}\n\n{description}",
                'issuetype': {'name': 'Bug'}, # Or 'Incident' if custom type exists
                'priority': {'name': self._map_severity(severity)}
            }
            new_issue = self.jira.create_issue(fields=issue_dict)
            return new_issue.key
        except Exception as e:
            logging.getLogger(__name__).error(f"Error creating Jira ticket: {e}")
            return None

    def _map_severity(self, severity: str) -> str:
        mapping = {
            'HIGH': 'High',
            'MEDIUM': 'Medium', 
            'LOW': 'Low',
            'CRITICAL': 'Highest'
        }
        return mapping.get(severity, 'Medium')
