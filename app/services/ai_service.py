import logging
import json
from typing import Optional, Dict, Any
from groq import Groq
from app.models.incident import Incident
from app.models.enums import IncidentSeverity, IncidentType
from config import Config
from app.services.rag_service import RagService

logger = logging.getLogger(__name__)

class AiService:
    def __init__(self, rag_service: RagService):
        self.client = Groq(api_key=Config.GROQ_API_KEY)
        self.rag_service = rag_service
        self.model = Config.AI_MODEL

    def classify_incident(self, incident: Incident) -> Dict[str, Any]:
        logger.info(f"Classifying incident: {incident.external_id}")

        # 1. RAG Search & Optimization
        similar_incidents = []
        try:
            query = f"{incident.description} {incident.type.value}"
            similar_incidents = self.rag_service.search_similar_incidents(query)
            
            # Optimization: Skip LLM if high confidence match found
            if similar_incidents and similar_incidents[0].score > 0.85:
                best = similar_incidents[0]
                logger.info(f"RAG Optimization: High confidence match ({best.score:.2f}) found. Skipping LLM.")
                return {
                    "severity": IncidentSeverity.UNKNOWN, # Keep unknown or map from knowledge if available
                    "confidence": best.score,
                    "reasoning": f"Exact match found in Knowledge Base: {best.title}",
                    "suggestion": best.solution
                }
                
        except Exception as e:
            logger.error(f"RAG search failed: {e}")

        # 2. Construct Prompt for Groq
        context_str = "\n".join([f"- {i.title}: {i.solution}" for i in similar_incidents])
        
        system_prompt = """You are an expert incident management AI. Analyze the incident and provide classification.
        RESPONSE FORMAT: JSON with keys: severity, confidence, reasoning, suggestion.
        Severity options: LOW, MEDIUM, HIGH, CRITICAL.
        Confidence: 0.0 to 1.0.
        """
        
        user_prompt = f"""
        INCIDENT DETAILS:
        Type: {incident.type.value}
        Description: {incident.description}
        Source: {incident.source}
        Metadata: {incident.metadata_}

        SIMILAR KNOWLEDGE BASE ENTRIES:
        {context_str}

        Analyze and classify.
        """

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                response_format={"type": "json_object"},
                temperature=0.3
            )
            
            content = response.choices[0].message.content
            result = json.loads(content)
            
            return {
                "severity": IncidentSeverity(result.get("severity", "UNKNOWN")),
                "confidence": float(result.get("confidence", 0.0)),
                "reasoning": result.get("reasoning", ""),
                "suggestion": result.get("suggestion", "")
            }

        except Exception as e:
            logger.error(f"AI Classification failed: {e}")
            return {
                "severity": IncidentSeverity.UNKNOWN,
                "confidence": 0.0,
                "reasoning": f"AI Error: {str(e)}",
                "suggestion": "Manual review required"
            }
