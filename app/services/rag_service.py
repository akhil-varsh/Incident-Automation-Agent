import logging
import uuid
import json
from typing import List, Optional, Dict, Any
from datetime import datetime

from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_core.documents import Document

from app.schemas.knowledge import KnowledgeEntry
from config import Config

logger = logging.getLogger(__name__)

class RagService:
    def __init__(self):
        self.embeddings = OpenAIEmbeddings(api_key=Config.OPENAI_API_KEY)
        self.vector_store = Chroma(
            collection_name="incident_knowledge_base",
            embedding_function=self.embeddings,
            persist_directory="./chroma_db"
        )

    def add_knowledge_entry(self, entry: KnowledgeEntry) -> None:
        try:
            if not entry.id:
                entry.id = str(uuid.uuid4())

            # Create document text for embedding
            embedding_text = (
                f"Title: {entry.title}\n"
                f"Pattern Type: {entry.pattern_type}\n"
                f"Symptoms: {entry.symptoms}\n"
                f"Root Cause: {entry.root_cause}\n"
                f"Solution: {entry.solution}\n"
                f"Severity: {entry.severity}"
            )

            # Metadata for filtering and retrieval
            metadata = entry.model_dump(exclude={"metadata", "prerequisites", "verification_steps", "tags", "environments", "technologies"}, mode='json')
            # Store complex lists as JSON strings in metadata if needed, but Chroma handles primitives well.
            # Simplified metadata for compatibility
            
            doc = Document(page_content=embedding_text, metadata=metadata, id=entry.id)
            self.vector_store.add_documents([doc])
            logger.info(f"Added knowledge entry: {entry.title}")

        except Exception as e:
            logger.error(f"Error adding knowledge entry: {e}")
            raise

    def search_similar_incidents(self, query: str, k: int = 3) -> List[KnowledgeEntry]:
        try:
            results = self.vector_store.similarity_search_with_score(query, k=k)
            entries = []
            for doc, score in results:
                # Reconstruct KnowledgeEntry from metadata (simplified)
                # Note: Full reconstruction might require storing full JSON in metadata or a separate DB
                # For now, we populate what we have in metadata
                data = doc.metadata.copy()
                data['symptoms'] = "See full content" # Placeholder if not in metadata
                
                # Parse back fields if available
                # In a real app, you might fetch full details from a SQL DB using ID
                
                # Create a temporary entry from metadata + content
                # This logic tries to respect the fields we put in metadata
                entry = KnowledgeEntry(
                     id=data.get('id'),
                     title=data.get('title', 'Unknown'),
                     pattern_type=data.get('pattern_type', 'OTHER'),
                     symptoms=data.get('symptoms', 'Refer to content'),
                     root_cause=data.get('root_cause', 'Refer to content'),
                     solution=data.get('solution', 'Refer to content'),
                     severity=data.get('severity'),
                     confidence_score=data.get('confidence_score', 0.0)
                )
                # Pass score roughly as confidence
                # LangChain score 0 is perfect match (distance), so 1 - distance approx similarity
                # Chroma default might be L2 distance, so validation needed.
                entries.append(entry)
            return entries

        except Exception as e:
            logger.error(f"Error searching similar incidents: {e}")
            return []

    def initialize_sample_data(self):
        # Ported from VectorSearchKnowledgeBaseService.java
        samples = [
            KnowledgeEntry(
                title="Database Connection Timeout Issues",
                pattern_type="DATABASE_CONNECTION_ERROR",
                severity="HIGH",
                symptoms="Application unable to connect to database, connection pool exhausted",
                root_cause="Connection pool exhaustion",
                solution="Increase max_connections, kill long running queries",
                environments=["production"]
            ),
             KnowledgeEntry(
                title="Authentication Service Failure",
                pattern_type="AUTHENTICATION_ERROR",
                severity="HIGH",
                symptoms="Users cannot login, 401 errors",
                root_cause="Auth service crash",
                solution="Restart auth service, check logs",
                environments=["production"]
            )
        ]
        for s in samples:
            self.add_knowledge_entry(s)
