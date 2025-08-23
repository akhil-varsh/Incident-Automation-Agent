package com.xlbiz.incident.agent.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentType;

/**
 * Service for managing Slack integrations for incident management
 * Handles stakeholder notifications, channel management, and escalation
 */
@Service
public class SlackIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SlackIntegrationService.class);
    
    private final SlackClient slackClient;
    
    // Stakeholder mappings based on incident type and severity
    private final Map<IncidentType, List<String>> typeStakeholders = new HashMap<>();
    private final Map<IncidentSeverity, List<String>> severityStakeholders = new HashMap<>();
    
    public SlackIntegrationService(SlackClient slackClient) {
        this.slackClient = slackClient;
        initializeStakeholderMappings();
    }
    
    /**
     * Process Slack integration for a new incident
     */
    public SlackIntegrationResult processIncidentSlackIntegration(Incident incident, String aiSuggestion) {
        logger.info("Processing Slack integration for incident: {}", incident.getExternalId());
        
        SlackIntegrationResult result = new SlackIntegrationResult();
        result.setIncidentId(incident.getExternalId());
        
        try {
            // Create dedicated channel for the incident
            Optional<String> channelId = slackClient.createIncidentChannel(incident);
            
            if (channelId.isPresent()) {
                result.setChannelCreated(true);
                result.setChannelId(channelId.get());
                logger.info("Created Slack channel {} for incident {}", channelId.get(), incident.getExternalId());
                
                // Post initial incident notification
                boolean notificationSent = slackClient.postIncidentNotification(channelId.get(), incident, aiSuggestion);
                result.setNotificationSent(notificationSent);
                
                if (notificationSent) {
                    logger.info("Posted incident notification to channel {} for incident {}", 
                               channelId.get(), incident.getExternalId());
                } else {
                    logger.warn("Failed to post incident notification for incident {}", incident.getExternalId());
                }
                
                // Invite stakeholders based on incident type and severity
                List<String> stakeholders = getStakeholdersForIncident(incident);
                result.setInvitedStakeholders(stakeholders);
                
                if (!stakeholders.isEmpty()) {
                    logger.info("Identified {} stakeholders for incident {}: {}", 
                               stakeholders.size(), incident.getExternalId(), stakeholders);
                }
                
            } else {
                result.setChannelCreated(false);
                logger.error("Failed to create Slack channel for incident: {}", incident.getExternalId());
            }
            
        } catch (Exception e) {
            logger.error("Error processing Slack integration for incident {}: {}", 
                        incident.getExternalId(), e.getMessage(), e);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Update incident status in Slack
     */
    public boolean updateIncidentStatus(String channelId, Incident incident, String updateMessage) {
        logger.info("Updating incident status in Slack channel {} for incident {}", 
                   channelId, incident.getExternalId());
        
        try {
            boolean success = slackClient.postStatusUpdate(channelId, incident, updateMessage);
            
            if (success) {
                logger.info("Successfully updated incident status in Slack for incident {}", 
                           incident.getExternalId());
            } else {
                logger.warn("Failed to update incident status in Slack for incident {}", 
                           incident.getExternalId());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error updating incident status in Slack for incident {}: {}", 
                        incident.getExternalId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Archive channel when incident is resolved
     */
    public boolean archiveIncidentChannel(String channelId, Incident incident) {
        logger.info("Archiving Slack channel {} for resolved incident {}", 
                   channelId, incident.getExternalId());
        
        try {
            // Post final status update before archiving
            String finalMessage = String.format("✅ *INCIDENT RESOLVED*\n\n" +
                "Incident %s has been resolved and this channel will be archived.\n" +
                "Final Status: %s\n" +
                "Resolution Time: %s",
                incident.getExternalId(),
                incident.getStatus().name(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            slackClient.postStatusUpdate(channelId, incident, finalMessage);
            
            // Archive the channel
            boolean success = slackClient.archiveChannel(channelId);
            
            if (success) {
                logger.info("Successfully archived Slack channel {} for incident {}", 
                           channelId, incident.getExternalId());
            } else {
                logger.warn("Failed to archive Slack channel {} for incident {}", 
                           channelId, incident.getExternalId());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error archiving Slack channel for incident {}: {}", 
                        incident.getExternalId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Test Slack integration connectivity
     */
    public boolean testSlackIntegration() {
        logger.info("Testing Slack integration connectivity");
        
        try {
            boolean connected = slackClient.testConnection();
            
            if (connected) {
                logger.info("Slack integration test successful");
            } else {
                logger.error("Slack integration test failed");
            }
            
            return connected;
            
        } catch (Exception e) {
            logger.error("Error testing Slack integration: {}", e.getMessage(), e);
            return false;
        }
    }
    
    // Private helper methods
    
    private void initializeStakeholderMappings() {
        // Initialize stakeholder mappings based on incident types
        typeStakeholders.put(IncidentType.DATABASE_CONNECTION_ERROR, 
            Arrays.asList("database-team", "backend-team"));
        typeStakeholders.put(IncidentType.HIGH_CPU, 
            Arrays.asList("infrastructure-team", "performance-team"));
        typeStakeholders.put(IncidentType.DISK_FULL, 
            Arrays.asList("infrastructure-team", "ops-team"));
        typeStakeholders.put(IncidentType.NETWORK_ISSUE, 
            Arrays.asList("network-team", "infrastructure-team"));
        typeStakeholders.put(IncidentType.API_FAILURE, 
            Arrays.asList("api-team", "backend-team"));
        typeStakeholders.put(IncidentType.MEMORY_LEAK, 
            Arrays.asList("backend-team", "performance-team"));
        typeStakeholders.put(IncidentType.SECURITY_BREACH, 
            Arrays.asList("security-team", "incident-response"));
        typeStakeholders.put(IncidentType.SERVICE_DOWN, 
            Arrays.asList("ops-team", "infrastructure-team", "on-call"));
        typeStakeholders.put(IncidentType.DATA_CORRUPTION, 
            Arrays.asList("database-team", "data-team", "backup-team"));
        typeStakeholders.put(IncidentType.DEPLOYMENT_FAILURE, 
            Arrays.asList("deployment-team", "devops-team"));
        
        // Initialize stakeholder mappings based on severity levels
        severityStakeholders.put(IncidentSeverity.HIGH, 
            Arrays.asList("on-call", "incident-commander", "team-leads"));
        severityStakeholders.put(IncidentSeverity.MEDIUM, 
            Arrays.asList("team-leads"));
        severityStakeholders.put(IncidentSeverity.LOW, 
            Collections.emptyList());
        severityStakeholders.put(IncidentSeverity.UNKNOWN, 
            Arrays.asList("on-call"));
    }
    
    private List<String> getStakeholdersForIncident(Incident incident) {
        Set<String> stakeholders = new HashSet<>();
        
        // Add stakeholders based on incident type
        List<String> typeBasedStakeholders = typeStakeholders.get(incident.getType());
        if (typeBasedStakeholders != null) {
            stakeholders.addAll(typeBasedStakeholders);
        }
        
        // Add stakeholders based on severity
        List<String> severityBasedStakeholders = severityStakeholders.get(incident.getSeverity());
        if (severityBasedStakeholders != null) {
            stakeholders.addAll(severityBasedStakeholders);
        }
        
        return new ArrayList<>(stakeholders);
    }
    
    /**
     * Result object for Slack integration operations
     */
    public static class SlackIntegrationResult {
        private String incidentId;
        private boolean channelCreated;
        private String channelId;
        private boolean notificationSent;
        private List<String> invitedStakeholders = new ArrayList<>();
        private String errorMessage;
        
        // Getters and setters
        public String getIncidentId() {
            return incidentId;
        }
        
        public void setIncidentId(String incidentId) {
            this.incidentId = incidentId;
        }
        
        public boolean isChannelCreated() {
            return channelCreated;
        }
        
        public void setChannelCreated(boolean channelCreated) {
            this.channelCreated = channelCreated;
        }
        
        public String getChannelId() {
            return channelId;
        }
        
        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }
        
        public boolean isNotificationSent() {
            return notificationSent;
        }
        
        public void setNotificationSent(boolean notificationSent) {
            this.notificationSent = notificationSent;
        }
        
        public List<String> getInvitedStakeholders() {
            return invitedStakeholders;
        }
        
        public void setInvitedStakeholders(List<String> invitedStakeholders) {
            this.invitedStakeholders = invitedStakeholders;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccessful() {
            return channelCreated && notificationSent && errorMessage == null;
        }
    }
}
