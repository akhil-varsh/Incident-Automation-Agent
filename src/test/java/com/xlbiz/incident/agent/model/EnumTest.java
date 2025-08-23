package com.xlbiz.incident.agent.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for incident-related enums.
 * Tests enum conversion and business logic methods.
 */
public class EnumTest {

    @Test
    public void testIncidentTypeFromString() {
        // Test valid conversions
        assertEquals(IncidentType.DATABASE_CONNECTION_ERROR, IncidentType.fromString("DATABASE_CONNECTION_ERROR"));
        assertEquals(IncidentType.HIGH_CPU, IncidentType.fromString("high_cpu"));
        assertEquals(IncidentType.DISK_FULL, IncidentType.fromString("Disk_Full"));
        
        // Test invalid/null conversions
        assertEquals(IncidentType.OTHER, IncidentType.fromString("INVALID_TYPE"));
        assertEquals(IncidentType.OTHER, IncidentType.fromString(null));
        assertEquals(IncidentType.OTHER, IncidentType.fromString(""));
    }

    @Test
    public void testIncidentSeverityFromString() {
        // Test valid conversions
        assertEquals(IncidentSeverity.LOW, IncidentSeverity.fromString("LOW"));
        assertEquals(IncidentSeverity.MEDIUM, IncidentSeverity.fromString("medium"));
        assertEquals(IncidentSeverity.HIGH, IncidentSeverity.fromString("High"));
        
        // Test invalid/null conversions
        assertEquals(IncidentSeverity.UNKNOWN, IncidentSeverity.fromString("INVALID"));
        assertEquals(IncidentSeverity.UNKNOWN, IncidentSeverity.fromString(null));
    }

    @Test
    public void testIncidentSeverityBusinessLogic() {
        // Test urgency
        assertTrue(IncidentSeverity.HIGH.isUrgent());
        assertFalse(IncidentSeverity.MEDIUM.isUrgent());
        assertFalse(IncidentSeverity.LOW.isUrgent());
        assertFalse(IncidentSeverity.UNKNOWN.isUrgent());

        // Test Slack channel requirement
        assertTrue(IncidentSeverity.HIGH.requiresSlackChannel());
        assertTrue(IncidentSeverity.MEDIUM.requiresSlackChannel());
        assertFalse(IncidentSeverity.LOW.requiresSlackChannel());
        assertFalse(IncidentSeverity.UNKNOWN.requiresSlackChannel());

        // Test priority values
        assertEquals(3, IncidentSeverity.HIGH.getPriority());
        assertEquals(2, IncidentSeverity.MEDIUM.getPriority());
        assertEquals(1, IncidentSeverity.LOW.getPriority());
        assertEquals(0, IncidentSeverity.UNKNOWN.getPriority());
    }

    @Test
    public void testIncidentStatusFromString() {
        // Test valid conversions
        assertEquals(IncidentStatus.RECEIVED, IncidentStatus.fromString("RECEIVED"));
        assertEquals(IncidentStatus.PROCESSING, IncidentStatus.fromString("processing"));
        assertEquals(IncidentStatus.RESOLVED, IncidentStatus.fromString("Resolved"));
        
        // Test invalid/null conversions
        assertEquals(IncidentStatus.RECEIVED, IncidentStatus.fromString("INVALID"));
        assertEquals(IncidentStatus.RECEIVED, IncidentStatus.fromString(null));
    }

    @Test
    public void testIncidentStatusBusinessLogic() {
        // Test active status
        assertTrue(IncidentStatus.RECEIVED.isActive());
        assertTrue(IncidentStatus.PROCESSING.isActive());
        assertTrue(IncidentStatus.IN_PROGRESS.isActive());
        assertFalse(IncidentStatus.RESOLVED.isActive());
        assertFalse(IncidentStatus.CLOSED.isActive());
        assertFalse(IncidentStatus.FAILED.isActive());

        // Test complete status
        assertTrue(IncidentStatus.PROCESSED.isComplete());
        assertTrue(IncidentStatus.RESOLVED.isComplete());
        assertTrue(IncidentStatus.CLOSED.isComplete());
        assertFalse(IncidentStatus.RECEIVED.isComplete());
        assertFalse(IncidentStatus.PROCESSING.isComplete());

        // Test error status
        assertTrue(IncidentStatus.FAILED.isError());
        assertFalse(IncidentStatus.RESOLVED.isError());
        assertFalse(IncidentStatus.PROCESSING.isError());
    }

    @Test
    public void testDisplayNames() {
        assertEquals("Database Connection Error", IncidentType.DATABASE_CONNECTION_ERROR.getDisplayName());
        assertEquals("High", IncidentSeverity.HIGH.getDisplayName());
        assertEquals("In Progress", IncidentStatus.IN_PROGRESS.getDisplayName());
    }
}