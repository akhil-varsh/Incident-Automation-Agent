package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentStatus;
import com.xlbiz.incident.agent.model.IncidentType;

import java.time.LocalDateTime;

/**
 * DTO for incident summary information used in list views.
 * Provides essential incident details without full metadata.
 */
public class IncidentSummary {

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
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("has_ai_processing")
    private boolean hasAiProcessing;

    @JsonProperty("has_slack_integration")
    private boolean hasSlackIntegration;

    @JsonProperty("has_jira_integration")
    private boolean hasJiraIntegration;

    @JsonProperty("slack_channel_id")
    private String slackChannelId;

    @JsonProperty("jira_ticket_key")
    private String jiraTicketKey;

    // Constructors

    public IncidentSummary() {
    }

    public IncidentSummary(Incident incident) {
        this.incidentId = incident.getId();
        this.externalId = incident.getExternalId();
        this.type = incident.getType();
        this.description = truncateDescription(incident.getDescription());
        this.severity = incident.getSeverity();
        this.status = incident.getStatus();
        this.source = incident.getSource();
        this.createdAt = incident.getCreatedAt();
        this.updatedAt = incident.getUpdatedAt();
        this.hasAiProcessing = incident.isAiProcessed();
        this.hasSlackIntegration = incident.hasSlackIntegration();
        this.hasJiraIntegration = incident.hasJiraIntegration();
        this.slackChannelId = incident.getSlackChannelId();
        this.jiraTicketKey = incident.getJiraTicketKey();
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

    public boolean isHasAiProcessing() {
        return hasAiProcessing;
    }

    public void setHasAiProcessing(boolean hasAiProcessing) {
        this.hasAiProcessing = hasAiProcessing;
    }

    public boolean isHasSlackIntegration() {
        return hasSlackIntegration;
    }

    public void setHasSlackIntegration(boolean hasSlackIntegration) {
        this.hasSlackIntegration = hasSlackIntegration;
    }

    public boolean isHasJiraIntegration() {
        return hasJiraIntegration;
    }

    public void setHasJiraIntegration(boolean hasJiraIntegration) {
        this.hasJiraIntegration = hasJiraIntegration;
    }

    public String getSlackChannelId() {
        return slackChannelId;
    }

    public void setSlackChannelId(String slackChannelId) {
        this.slackChannelId = slackChannelId;
    }

    public String getJiraTicketKey() {
        return jiraTicketKey;
    }

    public void setJiraTicketKey(String jiraTicketKey) {
        this.jiraTicketKey = jiraTicketKey;
    }

    // Utility methods

    /**
     * Truncate description for summary view
     */
    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() <= 200) {
            return description;
        }
        return description.substring(0, 197) + "...";
    }

    @Override
    public String toString() {
        return "IncidentSummary{" +
                "incidentId=" + incidentId +
                ", externalId='" + externalId + '\'' +
                ", type=" + type +
                ", severity=" + severity +
                ", status=" + status +
                ", source='" + source + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}