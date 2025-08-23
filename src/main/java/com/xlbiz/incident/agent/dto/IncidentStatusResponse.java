package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentStatus;
import com.xlbiz.incident.agent.model.IncidentType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for incident status queries.
 * Provides detailed information about an incident's current state and processing history.
 */
public class IncidentStatusResponse {

    @JsonProperty("incident_id")
    private Long incidentId;

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("type")
    private IncidentType type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("severity")
    private IncidentSeverity severity;

    @JsonProperty("status")
    private IncidentStatus status;

    @JsonProperty("source")
    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("incident_timestamp")
    private LocalDateTime incidentTimestamp;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("ai_analysis")
    private AiAnalysis aiAnalysis;

    @JsonProperty("integrations")
    private IntegrationDetails integrations;

    // Constructors

    public IncidentStatusResponse() {
    }

    public IncidentStatusResponse(Incident incident) {
        this.incidentId = incident.getId();
        this.externalId = incident.getExternalId();
        this.type = incident.getType();
        this.description = incident.getDescription();
        this.severity = incident.getSeverity();
        this.status = incident.getStatus();
        this.source = incident.getSource();
        this.incidentTimestamp = incident.getIncidentTimestamp();
        this.createdAt = incident.getCreatedAt();
        this.updatedAt = incident.getUpdatedAt();
        this.resolvedAt = incident.getResolvedAt();
        this.metadata = incident.getMetadata();
        
        // Set AI analysis
        this.aiAnalysis = new AiAnalysis();
        this.aiAnalysis.setSuggestion(incident.getAiSuggestion());
        this.aiAnalysis.setReasoning(incident.getAiReasoning());
        this.aiAnalysis.setConfidence(incident.getAiConfidence());
        this.aiAnalysis.setProcessed(incident.isAiProcessed());
        
        // Set integration details
        this.integrations = new IntegrationDetails();
        this.integrations.setSlackChannelId(incident.getSlackChannelId());
        this.integrations.setSlackMessageTs(incident.getSlackMessageTs());
        this.integrations.setJiraTicketKey(incident.getJiraTicketKey());
        this.integrations.setSlackIntegrated(incident.hasSlackIntegration());
        this.integrations.setJiraIntegrated(incident.hasJiraIntegration());
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public AiAnalysis getAiAnalysis() {
        return aiAnalysis;
    }

    public void setAiAnalysis(AiAnalysis aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }

    public IntegrationDetails getIntegrations() {
        return integrations;
    }

    public void setIntegrations(IntegrationDetails integrations) {
        this.integrations = integrations;
    }

    /**
     * Nested class for AI analysis details
     */
    public static class AiAnalysis {
        
        @JsonProperty("suggestion")
        private String suggestion;
        
        @JsonProperty("reasoning")
        private String reasoning;
        
        @JsonProperty("confidence")
        private Double confidence;
        
        @JsonProperty("processed")
        private boolean processed;

        // Getters and Setters
        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public boolean isProcessed() {
            return processed;
        }

        public void setProcessed(boolean processed) {
            this.processed = processed;
        }
    }

    /**
     * Nested class for integration details
     */
    public static class IntegrationDetails {
        
        @JsonProperty("slack_channel_id")
        private String slackChannelId;
        
        @JsonProperty("slack_message_ts")
        private String slackMessageTs;
        
        @JsonProperty("slack_integrated")
        private boolean slackIntegrated;
        
        @JsonProperty("jira_ticket_key")
        private String jiraTicketKey;
        
        @JsonProperty("jira_integrated")
        private boolean jiraIntegrated;

        // Getters and Setters
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

        public boolean isSlackIntegrated() {
            return slackIntegrated;
        }

        public void setSlackIntegrated(boolean slackIntegrated) {
            this.slackIntegrated = slackIntegrated;
        }

        public String getJiraTicketKey() {
            return jiraTicketKey;
        }

        public void setJiraTicketKey(String jiraTicketKey) {
            this.jiraTicketKey = jiraTicketKey;
        }

        public boolean isJiraIntegrated() {
            return jiraIntegrated;
        }

        public void setJiraIntegrated(boolean jiraIntegrated) {
            this.jiraIntegrated = jiraIntegrated;
        }
    }
}