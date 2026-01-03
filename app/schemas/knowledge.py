from pydantic import BaseModel, Field, field_validator
from typing import List, Optional, Dict, Any
from datetime import datetime

class KnowledgeEntry(BaseModel):
    id: Optional[str] = None
    title: str = Field(..., min_length=1)
    pattern_type: str = Field(..., alias="patternType")
    symptoms: str
    root_cause: str = Field(..., alias="rootCause")
    solution: str
    severity: Optional[str] = None
    environments: Optional[List[str]] = None
    technologies: Optional[List[str]] = None
    confidence_score: float = Field(default=0.8, ge=0.0, le=1.0, alias="confidenceScore")
    success_rate: Optional[float] = Field(default=None, alias="successRate")
    resolution_time_minutes: Optional[int] = Field(default=None, alias="resolutionTimeMinutes")
    prerequisites: Optional[List[str]] = None
    verification_steps: Optional[List[str]] = Field(default=None, alias="verificationSteps")
    tags: Optional[List[str]] = None
    metadata: Optional[Dict[str, Any]] = None
    created_at: datetime = Field(default_factory=datetime.utcnow, alias="createdAt")
    updated_at: datetime = Field(default_factory=datetime.utcnow, alias="updatedAt")
    usage_count: int = Field(default=0, alias="usageCount")

    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "title": "Database Connection Timeout",
                "patternType": "DATABASE_CONNECTION_ERROR",
                "symptoms": "Application logs show 'Connection timed out' errors",
                "rootCause": "Connection pool exhaustion",
                "solution": "Increase max_connections in postgresql.conf",
                "severity": "HIGH"
            }
        }
