package com.xlbiz.incident.agent.dto;

import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for IncidentRequest DTO.
 * Tests validation and conversion logic.
 */
public class IncidentRequestTest {

    @Test
    public void testIncidentRequestCreation() {
        // Given
        String id = "INC-2024-001";
        String type = "DATABASE_CONNECTION_ERROR";
        String description = "Primary database connection pool exhausted";
        String source = "monitoring-system";
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        IncidentRequest request = new IncidentRequest(id, type, description, source, timestamp);

        // Then
        assertEquals(id, request.getId());
        assertEquals(type, request.getType());
        assertEquals(description, request.getDescription());
        assertEquals(source, request.getSource());
        assertEquals(timestamp, request.getTimestamp());
    }

    @Test
    public void testTypeConversion() {
        // Given
        IncidentRequest request = new IncidentRequest();
        request.setType("DATABASE_CONNECTION_ERROR");

        // When
        IncidentType incidentType = request.getIncidentType();

        // Then
        assertEquals(IncidentType.DATABASE_CONNECTION_ERROR, incidentType);
    }

    @Test
    public void testSeverityConversion() {
        // Given
        IncidentRequest request = new IncidentRequest();
        request.setSeverity("HIGH");

        // When
        IncidentSeverity severity = request.getIncidentSeverity();

        // Then
        assertEquals(IncidentSeverity.HIGH, severity);
    }

    @Test
    public void testMetadataHandling() {
        // Given
        IncidentRequest request = new IncidentRequest();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("service", "user-service");
        metadata.put("environment", "production");
        metadata.put("affected_users", 1500);

        // When
        request.setMetadata(metadata);

        // Then
        assertEquals("user-service", request.getMetadataValueAsString("service"));
        assertEquals("production", request.getMetadataValueAsString("environment"));
        assertEquals(Integer.valueOf(1500), request.getMetadataValueAsInteger("affected_users"));
    }

    @Test
    public void testHighPriorityDetection() {
        // Test high affected users
        IncidentRequest request1 = new IncidentRequest();
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("affected_users", 2000);
        request1.setMetadata(metadata1);
        assertTrue(request1.isHighPriority());

        // Test production environment
        IncidentRequest request2 = new IncidentRequest();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("environment", "production");
        request2.setMetadata(metadata2);
        assertTrue(request2.isHighPriority());

        // Test critical service
        IncidentRequest request3 = new IncidentRequest();
        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("service", "payment-service");
        request3.setMetadata(metadata3);
        assertTrue(request3.isHighPriority());

        // Test non-high priority
        IncidentRequest request4 = new IncidentRequest();
        Map<String, Object> metadata4 = new HashMap<>();
        metadata4.put("affected_users", 10);
        metadata4.put("environment", "staging");
        metadata4.put("service", "test-service");
        request4.setMetadata(metadata4);
        assertFalse(request4.isHighPriority());
    }

    @Test
    public void testMetadataValueConversions() {
        // Given
        IncidentRequest request = new IncidentRequest();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("string_value", "test");
        metadata.put("integer_value", 42);
        metadata.put("string_number", "123");
        metadata.put("invalid_number", "not_a_number");
        request.setMetadata(metadata);

        // Then
        assertEquals("test", request.getMetadataValueAsString("string_value"));
        assertEquals(Integer.valueOf(42), request.getMetadataValueAsInteger("integer_value"));
        assertEquals(Integer.valueOf(123), request.getMetadataValueAsInteger("string_number"));
        assertNull(request.getMetadataValueAsInteger("invalid_number"));
        assertNull(request.getMetadataValueAsString("non_existent"));
    }

    @Test
    public void testToString() {
        // Given
        IncidentRequest request = new IncidentRequest("INC-001", "HIGH_CPU", 
            "Very long description that should be truncated in toString method because it exceeds fifty characters", 
            "monitoring", LocalDateTime.now());

        // When
        String toString = request.toString();

        // Then
        assertTrue(toString.contains("INC-001"));
        assertTrue(toString.contains("HIGH_CPU"));
        assertTrue(toString.contains("..."));  // Should be truncated
    }
}