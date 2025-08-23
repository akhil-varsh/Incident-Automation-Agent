package com.xlbiz.incident.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JPA entity representing an incident in the automation system.
 * Stores all incident data including AI analysis results and integration status.
 */
@Entity
@Table(name = "incidents", indexes = {
    @Index(name = "idx_incident_external_id", columnList = "external_id"),
    @Index(name = "idx_incident_type", columnList = "type"),
    @Index(name = "idx_incident_severity", columnList = "severity"),
    @Index(name = "idx_incident_status", columnList = "status"),
    @Index(name = "idx_incident_created_at", columnList = "created_at"),
    @Index(name = "idx_incident_source", columnList = "source")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External incident ID from the source system
     */
    @Column(name = "external_id", unique = true, nullable = false, length = 255)
    private String externalId;

    /**
     * Type of incident (e.g., DATABASE_CONNECTION_ERROR, HIGH_CPU)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private IncidentType type;

    /**
     * Incident description from the source system
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * AI-classified or manually assigned severity
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private IncidentSeverity severity = IncidentSeverity.UNKNOWN;

    /**
     * Current processing status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.RECEIVED;

    /**
     * Source system that reported the incident
     */
    @Column(name = "source", length = 100)
    private String source;

    /**
     * Timestamp when the incident occurred (from source system)
     */
    @Column(name = "incident_timestamp")
    private LocalDateTime incidentTimestamp;

    /**
     * Metadata from the source system (stored as JSONB)
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    private Map<String, Object> metadata = new HashMap<>();

    // Integration tracking fields
    
    /**
     * Slack channel ID created for this incident
     */
    @Column(name = "slack_channel_id", length = 50)
    private String slackChannelId;

    /**
     * Slack message timestamp for the incident summary
     */
    @Column(name = "slack_message_ts", length = 50)
    private String slackMessageTs;

    /**
     * Jira ticket key created for this incident
     */
    @Column(name = "jira_ticket_key", length = 20)
    private String jiraTicketKey;

    // AI analysis results
    
    /**
     * AI-generated remediation suggestion
     */
    @Column(name = "ai_suggestion", columnDefinition = "TEXT")
    private String aiSuggestion;

    /**
     * AI reasoning for severity classification
     */
    @Column(name = "ai_reasoning", columnDefinition = "TEXT")
    private String aiReasoning;

    /**
     * Confidence score for AI classification (0.0 to 1.0)
     */
    @Column(name = "ai_confidence")
    private Double aiConfidence;

    // Voice call fields
    
    /**
     * Voice call transcription text
     */
    @Column(name = "transcription", columnDefinition = "TEXT")
    private String transcription;

    /**
     * URL to the voice recording file
     */
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    /**
     * Call duration in seconds
     */
    @Column(name = "call_duration")
    private Integer callDuration;

    /**
     * Unique identifier for the voice conversation
     */
    @Column(name = "conversation_uuid", length = 100)
    private String conversationUuid;

    // Audit fields
    
    /**
     * Timestamp when the incident was created in our system
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the incident was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp when the incident was resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Constructors

    public Incident() {
    }

    public Incident(String externalId, IncidentType type, String description, String source) {
        this.externalId = externalId;
        this.type = type;
        this.description = description;
        this.source = source;
        this.incidentTimestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public IncidentType getType() {
        return type;
    }

    public void setType(IncidentType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
        // Automatically set resolved timestamp when status changes to RESOLVED
        if (status == IncidentStatus.RESOLVED && this.resolvedAt == null) {
            this.resolvedAt = LocalDateTime.now();
        }
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getIncidentTimestamp() {
        return incidentTimestamp;
    }

    public void setIncidentTimestamp(LocalDateTime incidentTimestamp) {
        this.incidentTimestamp = incidentTimestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public String getSlackChannelId() {
        return slackChannelId;
    }

    public void setSlackChannelId(String slackChannelId) {
        this.slackChannelId = slackChannelId;
    }

    public String getSlackMessageTs() {
        return slackMessageTs;
    }

    public void setSlackMessageTs(String slackMessageTs) {
        this.slackMessageTs = slackMessageTs;
    }

    public String getJiraTicketKey() {
        return jiraTicketKey;
    }

    public void setJiraTicketKey(String jiraTicketKey) {
        this.jiraTicketKey = jiraTicketKey;
    }

    public String getAiSuggestion() {
        return aiSuggestion;
    }

    public void setAiSuggestion(String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }

    public String getAiReasoning() {
        return aiReasoning;
    }

    public void setAiReasoning(String aiReasoning) {
        this.aiReasoning = aiReasoning;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public Integer getCallDuration() {
        return callDuration;
    }

    public void setCallDuration(Integer callDuration) {
        this.callDuration = callDuration;
    }

    public String getConversationUuid() {
        return conversationUuid;
    }

    public void setConversationUuid(String conversationUuid) {
        this.conversationUuid = conversationUuid;
    }

    // Utility methods

    /**
     * Check if this incident has been processed by AI
     */
    public boolean isAiProcessed() {
        return aiSuggestion != null && !aiSuggestion.trim().isEmpty();
    }

    /**
     * Check if Slack integration is complete
     */
    public boolean hasSlackIntegration() {
        return slackChannelId != null && !slackChannelId.trim().isEmpty();
    }

    /**
     * Check if Jira integration is complete
     */
    public boolean hasJiraIntegration() {
        return jiraTicketKey != null && !jiraTicketKey.trim().isEmpty();
    }

    /**
     * Check if this incident originated from a voice call
     */
    public boolean isVoiceIncident() {
        return conversationUuid != null && !conversationUuid.trim().isEmpty();
    }

    /**
     * Check if voice transcription is available
     */
    public boolean hasTranscription() {
        return transcription != null && !transcription.trim().isEmpty();
    }

    /**
     * Get formatted call duration
     */
    public String getFormattedCallDuration() {
        if (callDuration == null) {
            return null;
        }
        
        int minutes = callDuration / 60;
        int seconds = callDuration % 60;
        
        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Get a metadata value by key
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Set a metadata value
     */
    public void setMetadataValue(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    // equals, hashCode, toString

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Incident incident = (Incident) o;
        return Objects.equals(externalId, incident.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId);
    }

    @Override
    public String toString() {
        return "Incident{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", type=" + type +
                ", severity=" + severity +
                ", status=" + status +
                ", source='" + source + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}