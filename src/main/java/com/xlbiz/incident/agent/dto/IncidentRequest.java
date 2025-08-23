package com.xlbiz.incident.agent.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for incoming incident requests from external monitoring systems.
 * Represents the JSON payload structure expected by the incident trigger endpoint.
 */
public class IncidentRequest {

    /**
     * Unique identifier from the source monitoring system
     */
    @NotBlank(message = "Incident ID is required")
    @Size(max = 255, message = "Incident ID must not exceed 255 characters")
    @JsonProperty("id")
    private String id;

    /**
     * Type of incident (will be converted to IncidentType enum)
     */
    @NotNull(message = "Incident type is required")
    @JsonProperty("type")
    private String type;

    /**
     * Human-readable description of the incident
     */
    @NotBlank(message = "Incident description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @JsonProperty("description")
    private String description;

    /**
     * Optional severity level (if not provided, AI will classify)
     */
    @JsonProperty("severity")
    private String severity;

    /**
     * Source system that reported the incident
     */
    @NotBlank(message = "Source system is required")
    @Size(max = 100, message = "Source must not exceed 100 characters")
    @JsonProperty("source")
    private String source;

    /**
     * Timestamp when the incident occurred
     */
    @NotNull(message = "Incident timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Additional metadata from the monitoring system
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors

    public IncidentRequest() {
    }

    public IncidentRequest(String id, String type, String description, String source, LocalDateTime timestamp) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.source = source;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    // Utility methods

    /**
     * Convert string type to IncidentType enum
     */
    public IncidentType getIncidentType() {
        return IncidentType.fromString(this.type);
    }

    /**
     * Convert string severity to IncidentSeverity enum
     */
    public IncidentSeverity getIncidentSeverity() {
        return IncidentSeverity.fromString(this.severity);
    }

    /**
     * Get metadata value by key
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Get metadata value as string
     */
    public String getMetadataValueAsString(String key) {
        Object value = getMetadataValue(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get metadata value as integer
     */
    public Integer getMetadataValueAsInteger(String key) {
        Object value = getMetadataValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Check if this is a high-priority incident based on metadata
     */
    public boolean isHighPriority() {
        // Check for high affected user count
        Integer affectedUsers = getMetadataValueAsInteger("affected_users");
        if (affectedUsers != null && affectedUsers > 1000) {
            return true;
        }

        // Check for production environment
        String environment = getMetadataValueAsString("environment");
        if ("production".equalsIgnoreCase(environment) || "prod".equalsIgnoreCase(environment)) {
            return true;
        }

        // Check for critical services
        String service = getMetadataValueAsString("service");
        if (service != null) {
            String serviceLower = service.toLowerCase();
            return serviceLower.contains("payment") || 
                   serviceLower.contains("auth") || 
                   serviceLower.contains("database") ||
                   serviceLower.contains("api-gateway");
        }

        return false;
    }

    @Override
    public String toString() {
        return "IncidentRequest{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", description='" + (description != null && description.length() > 50 ? 
                    description.substring(0, 50) + "..." : description) + '\'' +
                ", severity='" + severity + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", metadata=" + metadata +
                '}';
    }
}