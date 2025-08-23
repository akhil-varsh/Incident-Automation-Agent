package com.xlbiz.incident.agent.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xlbiz.incident.agent.config.SlackConfig;
import com.xlbiz.incident.agent.dto.SlackApiResponse;
import com.xlbiz.incident.agent.dto.SlackChannelResponse;
import com.xlbiz.incident.agent.dto.SlackMessageResponse;
import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.IncidentSeverity;

/**
 * Service for interacting with Slack Web API
 * Handles channel creation, message posting, and archiving for incident management
 */
@Service
public class SlackClient {

    private static final Logger logger = LoggerFactory.getLogger(SlackClient.class);

    @Autowired
    private SlackConfig config;

    @Autowired
    @Qualifier("slackRestTemplate")
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> channelCache = new HashMap<>();

    /**
     * Creates a dedicated Slack channel for an incident
     */
    public Optional<String> createIncidentChannel(Incident incident) {
        try {
            String channelName = generateChannelName(incident);
            
            // Check if channel already exists in cache
            if (channelCache.containsKey(incident.getId().toString())) {
                return Optional.of(channelCache.get(incident.getId().toString()));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", channelName);
            requestBody.put("is_private", false);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                config.getSlackApiUrl() + "/conversations.create",
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                SlackChannelResponse channelResponse = objectMapper.readValue(
                    response.getBody(), SlackChannelResponse.class
                );

                if (channelResponse.isOk()) {
                    String channelId = channelResponse.getChannel().getId();
                    channelCache.put(incident.getId().toString(), channelId);
                    
                    // Set channel topic with incident details
                    setChannelTopic(channelId, generateChannelTopic(incident));
                    
                    logger.info("Created Slack channel: {} for incident: {}", 
                        channelName, incident.getId());
                    return Optional.of(channelId);
                } else {
                    logger.error("Failed to create Slack channel: {}", channelResponse.getError());
                }
            } else {
                logger.error("Failed to create Slack channel, status: {}", response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error creating Slack channel: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating Slack channel", e);
        }

        return Optional.empty();
    }

    /**
     * Posts incident notification to specified channel
     */
    public boolean postIncidentNotification(String channelId, Incident incident) {
        try {
            String message = formatIncidentMessage(incident);
            return postMessage(channelId, message);
        } catch (Exception e) {
            logger.error("Error posting incident notification", e);
            return false;
        }
    }

    /**
     * Posts incident notification with AI suggestion to specified channel
     */
    public boolean postIncidentNotification(String channelId, Incident incident, String aiSuggestion) {
        try {
            String message = formatIncidentMessage(incident, aiSuggestion);
            return postMessage(channelId, message);
        } catch (Exception e) {
            logger.error("Error posting incident notification with AI suggestion", e);
            return false;
        }
    }

    /**
     * Posts incident update to specified channel
     */
    public boolean postIncidentUpdate(String channelId, Incident incident, String updateMessage) {
        try {
            String message = formatIncidentUpdate(incident, updateMessage);
            return postMessage(channelId, message);
        } catch (Exception e) {
            logger.error("Error posting incident update", e);
            return false;
        }
    }

    /**
     * Posts status update to specified channel
     */
    public boolean postStatusUpdate(String channelId, Incident incident, String statusMessage) {
        try {
            String message = formatStatusUpdate(incident, statusMessage);
            return postMessage(channelId, message);
        } catch (Exception e) {
            logger.error("Error posting status update", e);
            return false;
        }
    }

    /**
     * Notifies stakeholders about incident
     */
    public boolean notifyStakeholders(String channelId, List<String> stakeholders, Incident incident) {
        try {
            String message = formatStakeholderNotification(stakeholders, incident);
            return postMessage(channelId, message);
        } catch (Exception e) {
            logger.error("Error notifying stakeholders", e);
            return false;
        }
    }

    /**
     * Archives incident channel when resolved
     */
    public boolean archiveChannel(String channelId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("channel", channelId);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                config.getSlackApiUrl() + "/conversations.archive",
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                SlackApiResponse apiResponse = objectMapper.readValue(
                    response.getBody(), SlackApiResponse.class
                );

                if (apiResponse.isOk()) {
                    logger.info("Successfully archived Slack channel: {}", channelId);
                    return true;
                } else {
                    logger.error("Failed to archive channel: {}", apiResponse.getError());
                }
            } else {
                logger.error("Failed to archive channel, status: {}", response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error archiving channel: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error archiving channel", e);
        }

        return false;
    }

    /**
     * Tests Slack API connectivity
     */
    public boolean testConnection() {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                config.getSlackApiUrl() + "/auth.test",
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                SlackApiResponse apiResponse = objectMapper.readValue(
                    response.getBody(), SlackApiResponse.class
                );
                
                if (apiResponse.isOk()) {
                    logger.info("Slack API connection test successful");
                    return true;
                } else {
                    logger.error("Slack API connection test failed: {}", apiResponse.getError());
                }
            } else {
                logger.error("Slack API connection test failed, status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error testing Slack API connection", e);
        }

        return false;
    }

    /**
     * Posts a message to specified channel
     */
    private boolean postMessage(String channelId, String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("channel", channelId);
            requestBody.put("text", message);
            requestBody.put("username", "IncidentBot");
            requestBody.put("icon_emoji", ":warning:");

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                config.getSlackApiUrl() + "/chat.postMessage",
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                SlackMessageResponse messageResponse = objectMapper.readValue(
                    response.getBody(), SlackMessageResponse.class
                );

                if (messageResponse.isOk()) {
                    return true;
                } else {
                    logger.error("Failed to post message: {}", messageResponse.getError());
                }
            } else {
                logger.error("Failed to post message, status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error posting message to Slack", e);
        }

        return false;
    }

    /**
     * Sets channel topic
     */
    private void setChannelTopic(String channelId, String topic) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("channel", channelId);
            requestBody.put("topic", topic);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                config.getSlackApiUrl() + "/conversations.setTopic",
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                logger.warn("Failed to set channel topic, status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.warn("Error setting channel topic: {}", e.getMessage());
        }
    }

    /**
     * Creates authorization headers for Slack API
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getBotToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Generates channel name for incident
     */
    private String generateChannelName(Incident incident) {
        String idString = incident.getId().toString();
        // Use the full ID if it's shorter than 8 characters, otherwise use first 8
        String shortId = idString.length() >= 8 ? idString.substring(0, 8) : idString;
        return String.format("incident-%s", shortId.toLowerCase());
    }

    /**
     * Generates channel topic for incident
     */
    private String generateChannelTopic(Incident incident) {
        String incidentTitle = incident.getExternalId() != null ? 
            incident.getExternalId() : incident.getDescription().substring(0, Math.min(50, incident.getDescription().length()));
        
        return String.format("Incident: %s | Type: %s | Severity: %s | Created: %s",
            incidentTitle,
            incident.getType().name(),
            incident.getSeverity().name(),
            incident.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Formats incident message for Slack
     */
    private String formatIncidentMessage(Incident incident) {
        StringBuilder message = new StringBuilder();
        String incidentTitle = incident.getExternalId() != null ? 
            incident.getExternalId() : "Incident #" + getShortId(incident.getId());
            
        message.append(String.format("%s *NEW INCIDENT ALERT*\n\n", getSeverityEmoji(incident.getSeverity())));
        message.append(String.format("*Incident:* %s\n", incidentTitle));
        message.append(String.format("*Type:* %s\n", incident.getType().name()));
        message.append(String.format("*Severity:* %s\n", incident.getSeverity().name()));
        message.append(String.format("*Description:* %s\n", incident.getDescription()));
        message.append(String.format("*Created:* %s\n", 
            incident.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        message.append(String.format("*Incident ID:* %s\n", incident.getId()));
        
        return message.toString();
    }

    /**
     * Formats incident message with AI suggestion for Slack
     */
    private String formatIncidentMessage(Incident incident, String aiSuggestion) {
        StringBuilder message = new StringBuilder();
        String incidentTitle = incident.getExternalId() != null ? 
            incident.getExternalId() : "Incident #" + getShortId(incident.getId());
            
        message.append(String.format("%s *NEW INCIDENT ALERT*\n\n", getSeverityEmoji(incident.getSeverity())));
        message.append(String.format("*Type:* %s\n", incident.getType().name()));
        message.append(String.format("*Severity:* %s\n", incident.getSeverity().name()));
        message.append(String.format("*Description:* %s\n", incident.getDescription()));
        message.append(String.format("*Created:* %s\n", 
            incident.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        
        if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
            message.append(String.format("\n🤖 *AI Analysis & Suggestions:*\n%s\n", aiSuggestion));
        }
        
        message.append(String.format("*Incident ID:* %s\n", incident.getId()));
        
        return message.toString();
    }

    /**
     * Formats incident update message
     */
    private String formatIncidentUpdate(Incident incident, String updateMessage) {
        StringBuilder message = new StringBuilder();
        String incidentTitle = incident.getExternalId() != null ? 
            incident.getExternalId() : "Incident #" + getShortId(incident.getId());
            
        message.append(String.format("🔄 *INCIDENT UPDATE*\n\n"));
        message.append(String.format("*Incident:* %s\n", incidentTitle));
        message.append(String.format("*Update:* %s\n", updateMessage));
        message.append(String.format("*Updated:* %s\n", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        
        return message.toString();
    }

    /**
     * Formats status update message
     */
    private String formatStatusUpdate(Incident incident, String statusMessage) {
        StringBuilder message = new StringBuilder();
        String incidentTitle = incident.getExternalId() != null ? 
            incident.getExternalId() : "Incident #" + getShortId(incident.getId());
            
        message.append(String.format("📊 *STATUS UPDATE*\n\n"));
        message.append(String.format("*Status:* %s\n", statusMessage));
        message.append(String.format("*Updated:* %s\n", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        
        return message.toString();
    }

    /**
     * Formats stakeholder notification message
     */
    private String formatStakeholderNotification(List<String> stakeholders, Incident incident) {
        StringBuilder message = new StringBuilder();
        String incidentTitle = incident.getExternalId() != null ? 
            incident.getExternalId() : "Incident #" + getShortId(incident.getId());
            
        message.append("📢 *STAKEHOLDER NOTIFICATION*\n\n");
        message.append(String.format("*Incident:* %s\n", incidentTitle));
        message.append("*Notifying:*\n");
        
        for (String stakeholder : stakeholders) {
            message.append(String.format("• @%s\n", stakeholder));
        }
        
        message.append(String.format("\n*Please review incident details and take appropriate action.*"));
        
        return message.toString();
    }

    /**
     * Gets emoji for incident severity
     */
    private String getSeverityEmoji(IncidentSeverity severity) {
        if (severity == null) {
            return "❓";
        }
        
        switch (severity) {
            case HIGH:
                return "🔥";
            case MEDIUM:
                return "⚠️";
            case LOW:
                return "ℹ️";
            case UNKNOWN:
                return "❓";
            default:
                return "⚠️";
        }
    }

    /**
     * Helper to get a short ID for incident
     */
    private String getShortId(Long id) {
        String idString = id.toString();
        return idString.length() >= 8 ? idString.substring(0, 8) : idString;
    }
}
