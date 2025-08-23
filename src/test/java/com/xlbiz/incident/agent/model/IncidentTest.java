package com.xlbiz.incident.agent.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the Incident entity model.
 * Tests basic functionality and business logic.
 */
public class IncidentTest {

    @Test
    public void testIncidentCreation() {
        // Given
        String externalId = "INC-2024-001";
        IncidentType type = IncidentType.DATABASE_CONNECTION_ERROR;
        String description = "Database connection pool exhausted";
        String source = "monitoring-system";

        // When
        Incident incident = new Incident(externalId, type, description, source);

        // Then
        assertEquals(externalId, incident.getExternalId());
        assertEquals(type, incident.getType());
        assertEquals(description, incident.getDescription());
        assertEquals(source, incident.getSource());
        assertEquals(IncidentSeverity.UNKNOWN, incident.getSeverity());
        assertEquals(IncidentStatus.RECEIVED, incident.getStatus());
        assertNotNull(incident.getIncidentTimestamp());
    }

    @Test
    public void testIncidentStatusTransition() {
        // Given
        Incident incident = new Incident("INC-001", IncidentType.HIGH_CPU, "High CPU usage", "monitoring");

        // When
        incident.setStatus(IncidentStatus.RESOLVED);

        // Then
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertNotNull(incident.getResolvedAt());
    }

    @Test
    public void testMetadataHandling() {
        // Given
        Incident incident = new Incident("INC-001", IncidentType.DISK_FULL, "Disk full", "monitoring");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("service", "user-service");
        metadata.put("environment", "production");
        metadata.put("affected_users", 1500);

        // When
        incident.setMetadata(metadata);
        incident.setMetadataValue("severity_score", 8.5);

        // Then
        assertEquals("user-service", incident.getMetadataValue("service"));
        assertEquals("production", incident.getMetadataValue("environment"));
        assertEquals(1500, incident.getMetadataValue("affected_users"));
        assertEquals(8.5, incident.getMetadataValue("severity_score"));
    }

    @Test
    public void testAiProcessingStatus() {
        // Given
        Incident incident = new Incident("INC-001", IncidentType.API_FAILURE, "API timeout", "monitoring");

        // When - Initially no AI processing
        assertFalse(incident.isAiProcessed());

        // When - Add AI suggestion
        incident.setAiSuggestion("Check API gateway configuration and restart service");
        incident.setAiReasoning("High severity due to production environment and user impact");
        incident.setAiConfidence(0.85);

        // Then
        assertTrue(incident.isAiProcessed());
        assertEquals("Check API gateway configuration and restart service", incident.getAiSuggestion());
        assertEquals(0.85, incident.getAiConfidence());
    }

    @Test
    public void testIntegrationStatus() {
        // Given
        Incident incident = new Incident("INC-001", IncidentType.SERVICE_DOWN, "Service unavailable", "monitoring");

        // When - Initially no integrations
        assertFalse(incident.hasSlackIntegration());
        assertFalse(incident.hasJiraIntegration());

        // When - Add integrations
        incident.setSlackChannelId("C1234567890");
        incident.setSlackMessageTs("1234567890.123456");
        incident.setJiraTicketKey("PREP-123");

        // Then
        assertTrue(incident.hasSlackIntegration());
        assertTrue(incident.hasJiraIntegration());
        assertEquals("C1234567890", incident.getSlackChannelId());
        assertEquals("PREP-123", incident.getJiraTicketKey());
    }

    @Test
    public void testIncidentEquality() {
        // Given
        Incident incident1 = new Incident("INC-001", IncidentType.NETWORK_ISSUE, "Network timeout", "monitoring");
        Incident incident2 = new Incident("INC-001", IncidentType.HIGH_CPU, "Different description", "different-source");
        Incident incident3 = new Incident("INC-002", IncidentType.NETWORK_ISSUE, "Network timeout", "monitoring");

        // Then
        assertEquals(incident1, incident2); // Same external ID
        assertNotEquals(incident1, incident3); // Different external ID
        assertEquals(incident1.hashCode(), incident2.hashCode());
    }
}