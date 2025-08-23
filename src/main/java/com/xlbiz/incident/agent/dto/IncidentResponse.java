package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentStatus;

import java.time.LocalDateTime;

/**
 * DTO for incident processing responses.
 * Returned by the incident trigger endpoint to provide processing status.
 */
public class IncidentResponse {

    /**
     * Internal incident ID (database primary key)
     */
    @JsonProperty("incident_id")
    private Long incidentId;

    /**
     * External incident ID from the source system
     */
    @JsonProperty("external_id")
    private String externalId;

    /**
     * Current processing status
     */
    @JsonProperty("status")
    private IncidentStatus status;

    /**
     * AI-classified severity level
     */
    @JsonProperty("ai_classified_severity")
    private IncidentSeverity aiClassifiedSeverity;

    /**
     * AI-generated remediation suggestion
     */
    @JsonProperty("ai_suggestion")
    private String aiSuggestion;

    /**
     * AI confidence score for classification
     */
    @JsonProperty("ai_confidence")
    private Double aiConfidence;

    /**
     * Integration status summary
     */
    @JsonProperty("integration_status")
    private IntegrationStatus integrationStatus;

    /**
     * Timestamp when the incident was processed
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("processed_at")
    private LocalDateTime processedAt;

    /**
     * Processing message or error details
     */
    @JsonProperty("message")
    private String message;

    // Constructors

    public IncidentResponse() {
        this.integrationStatus = new IntegrationStatus();
        this.processedAt = LocalDateTime.now();
    }

    public IncidentResponse(Long incidentId, String externalId, IncidentStatus status) {
        this();
        this.incidentId = incidentId;
        this.externalId = externalId;
        this.status = status;
    }

    // Getters and Setters

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public IncidentSeverity getAiClassifiedSeverity() {
        return aiClassifiedSeverity;
    }

    public void setAiClassifiedSeverity(IncidentSeverity aiClassifiedSeverity) {
        this.aiClassifiedSeverity = aiClassifiedSeverity;
    }

    public String getAiSuggestion() {
        return aiSuggestion;
    }

    public void setAiSuggestion(String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public IntegrationStatus getIntegrationStatus() {
        return integrationStatus;
    }

    public void setIntegrationStatus(IntegrationStatus integrationStatus) {
        this.integrationStatus = integrationStatus;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Utility methods

    /**
     * Create a successful response
     */
    public static IncidentResponse success(Long incidentId, String externalId, String message) {
        IncidentResponse response = new IncidentResponse(incidentId, externalId, IncidentStatus.RECEIVED);
        response.setMessage(message);
        return response;
    }

    /**
     * Create an error response
     */
    public static IncidentResponse error(String externalId, String message) {
        IncidentResponse response = new IncidentResponse();
        response.setExternalId(externalId);
        response.setStatus(IncidentStatus.FAILED);
        response.setMessage(message);
        return response;
    }

    /**
     * Nested class for integration status details
     */
    public static class IntegrationStatus {
        
        @JsonProperty("slack_enabled")
        private boolean slackEnabled = false;
        
        @JsonProperty("slack_channel_created")
        private boolean slackChannelCreated = false;
        
        @JsonProperty("slack_channel_id")
        private String slackChannelId;
        
        @JsonProperty("jira_enabled")
        private boolean jiraEnabled = false;
        
        @JsonProperty("jira_ticket_created")
        private boolean jiraTicketCreated = false;
        
        @JsonProperty("jira_ticket_key")
        private String jiraTicketKey;
        
        @JsonProperty("ai_processing_completed")
        private boolean aiProcessingCompleted = false;

        // Constructors
        public IntegrationStatus() {
        }

        // Getters and Setters
        public boolean isSlackEnabled() {
            return slackEnabled;
        }

        public void setSlackEnabled(boolean slackEnabled) {
            this.slackEnabled = slackEnabled;
        }

        public boolean isSlackChannelCreated() {
            return slackChannelCreated;
        }

        public void setSlackChannelCreated(boolean slackChannelCreated) {
            this.slackChannelCreated = slackChannelCreated;
        }

        public String getSlackChannelId() {
            return slackChannelId;
        }

        public void setSlackChannelId(String slackChannelId) {
            this.slackChannelId = slackChannelId;
        }

        public boolean isJiraEnabled() {
            return jiraEnabled;
        }

        public void setJiraEnabled(boolean jiraEnabled) {
            this.jiraEnabled = jiraEnabled;
        }

        public boolean isJiraTicketCreated() {
            return jiraTicketCreated;
        }

        public void setJiraTicketCreated(boolean jiraTicketCreated) {
            this.jiraTicketCreated = jiraTicketCreated;
        }

        public String getJiraTicketKey() {
            return jiraTicketKey;
        }

        public void setJiraTicketKey(String jiraTicketKey) {
            this.jiraTicketKey = jiraTicketKey;
        }

        public boolean isAiProcessingCompleted() {
            return aiProcessingCompleted;
        }

        public void setAiProcessingCompleted(boolean aiProcessingCompleted) {
            this.aiProcessingCompleted = aiProcessingCompleted;
        }
    }

    @Override
    public String toString() {
        return "IncidentResponse{" +
                "incidentId=" + incidentId +
                ", externalId='" + externalId + '\'' +
                ", status=" + status +
                ", aiClassifiedSeverity=" + aiClassifiedSeverity +
                ", processedAt=" + processedAt +
                ", message='" + message + '\'' +
                '}';
    }
}