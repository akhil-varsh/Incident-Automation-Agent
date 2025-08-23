package com.xlbiz.incident.agent.service;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import com.xlbiz.incident.agent.config.TwilioConfig;
import com.xlbiz.incident.agent.dto.OutboundCallRequest;
import com.xlbiz.incident.agent.dto.OutboundCallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TwilioOutboundCallService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioOutboundCallService.class);

    private final TwilioConfig twilioConfig;

    @Autowired
    public TwilioOutboundCallService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    /**
     * Make an outbound call to notify about an incident with AI suggestions
     */
    public OutboundCallResponse makeIncidentNotificationCall(String toPhoneNumber, String incidentId, String severity, String description, String aiSuggestion) {
        logger.info("Making enhanced incident notification call to {} for incident {}", toPhoneNumber, incidentId);
        
        OutboundCallRequest request = new OutboundCallRequest();
        request.setToPhoneNumber(toPhoneNumber);
        request.setCallType("INCIDENT_NOTIFICATION_ENHANCED");
        request.setIncidentId(incidentId);
        request.setSeverity(severity);
        request.setMessage(description);
        
        // Add AI suggestion to parameters
        Map<String, String> parameters = new HashMap<>();
        parameters.put("aiSuggestion", aiSuggestion);
        request.setParameters(parameters);
        
        return makeOutboundCall(request);
    }

    /**
     * Make an outbound call to notify about an incident (backward compatibility)
     */
    public OutboundCallResponse makeIncidentNotificationCall(String toPhoneNumber, String incidentId, String severity, String description) {
        return makeIncidentNotificationCall(toPhoneNumber, incidentId, severity, description, null);
    }

    /**
     * Make an outbound call for incident status update
     */
    public OutboundCallResponse makeIncidentUpdateCall(String toPhoneNumber, String incidentId, String status, String updateMessage) {
        logger.info("Making incident update call to {} for incident {}", toPhoneNumber, incidentId);
        
        OutboundCallRequest request = new OutboundCallRequest();
        request.setToPhoneNumber(toPhoneNumber);
        request.setCallType("INCIDENT_UPDATE");
        request.setIncidentId(incidentId);
        request.setStatus(status);
        request.setMessage(updateMessage);
        
        return makeOutboundCall(request);
    }

    /**
     * Make a general outbound call with custom TwiML
     */
    public OutboundCallResponse makeCustomCall(String toPhoneNumber, String twimlUrl, Map<String, String> parameters) {
        logger.info("Making custom outbound call to {} with TwiML URL: {}", toPhoneNumber, twimlUrl);
        
        OutboundCallRequest request = new OutboundCallRequest();
        request.setToPhoneNumber(toPhoneNumber);
        request.setCallType("CUSTOM");
        request.setTwimlUrl(twimlUrl);
        request.setParameters(parameters);
        
        return makeOutboundCall(request);
    }

    /**
     * Core method to make outbound calls using Twilio
     */
    private OutboundCallResponse makeOutboundCall(OutboundCallRequest request) {
        OutboundCallResponse response = new OutboundCallResponse();
        response.setRequestedAt(LocalDateTime.now());
        response.setToPhoneNumber(request.getToPhoneNumber());
        response.setCallType(request.getCallType());
        
        try {
            if (!twilioConfig.isEnabled()) {
                throw new RuntimeException("Twilio integration is disabled");
            }

            logger.info("Initiating Twilio call to {} using TwiML App: {}", request.getToPhoneNumber(), twilioConfig.getTwimlAppSid());
            
            // Create the call using TwiML App SID (cleaner approach for outbound calls)
            Call call = Call.creator(
                new PhoneNumber(request.getToPhoneNumber()), // To number
                new PhoneNumber(twilioConfig.getPhoneNumber()), // From number (your Twilio number)
                twilioConfig.getTwimlAppSid() // TwiML App SID
            )
            .setStatusCallback(URI.create(twilioConfig.getWebhookBaseUrl() + "/api/twilio/outbound/status"))
            .setStatusCallbackEvent(java.util.Arrays.asList("initiated", "ringing", "answered", "completed"))
            .setStatusCallbackMethod(com.twilio.http.HttpMethod.POST)
            .setRecord(true) // Record the call for quality/compliance
            .setTimeout(30) // Ring for 30 seconds
            .create();

            // Set successful response
            response.setCallSid(call.getSid());
            response.setStatus("INITIATED");
            response.setTwimlUrl("Using TwiML App: " + twilioConfig.getTwimlAppSid());
            response.setSuccess(true);
            response.setMessage("Call initiated successfully using TwiML App");
            
            logger.info("Outbound call initiated successfully. Call SID: {}", call.getSid());
            
        } catch (Exception e) {
            logger.error("Failed to make outbound call to {}: {}", request.getToPhoneNumber(), e.getMessage(), e);
            
            response.setSuccess(false);
            response.setStatus("FAILED");
            response.setMessage("Failed to initiate call: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Generate TwiML URL based on call type and parameters
     */
    private String generateTwiMLUrl(OutboundCallRequest request) {
        String baseUrl = twilioConfig.getWebhookBaseUrl() + "/api/twilio/outbound/twiml";
        
        switch (request.getCallType()) {
            case "INCIDENT_NOTIFICATION":
                return baseUrl + "/incident-notification" +
                       "?incidentId=" + encode(request.getIncidentId()) +
                       "&severity=" + encode(request.getSeverity()) +
                       "&message=" + encode(truncateForUrl(request.getMessage(), 500));
                       
            case "INCIDENT_NOTIFICATION_ENHANCED":
                // Use simple endpoint to avoid URL length issues
                // All incident details will be available in Slack anyway
                return baseUrl + "/simple-incident";
                       
            case "INCIDENT_UPDATE":
                return baseUrl + "/incident-update" +
                       "?incidentId=" + encode(request.getIncidentId()) +
                       "&status=" + encode(request.getStatus()) +
                       "&message=" + encode(truncateForUrl(request.getMessage(), 500));
                       
            case "CUSTOM":
                if (request.getTwimlUrl() != null && !request.getTwimlUrl().isEmpty()) {
                    return request.getTwimlUrl();
                }
                return baseUrl + "/custom" + buildParameterString(request.getParameters());
                
            default:
                return baseUrl + "/default";
        }
    }

    /**
     * Build parameter string for URL
     */
    private String buildParameterString(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            // Truncate parameter values to avoid URL length issues
            String value = truncateForUrl(entry.getValue(), 300);
            sb.append(encode(entry.getKey())).append("=").append(encode(value));
            first = false;
        }
        
        return sb.toString();
    }

    /**
     * URL encode string
     */
    private String encode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Truncate text for URL parameters to avoid length limits
     */
    private String truncateForUrl(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        // Find the last space within the limit to avoid cutting words
        String truncated = text.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        
        if (lastSpace > maxLength / 2) {
            return text.substring(0, lastSpace) + "...";
        } else {
            return truncated + "...";
        }
    }

    /**
     * Generate TwiML for incident notification calls with AI suggestions
     */
    public String generateIncidentNotificationTwiML(String incidentId, String severity, String description, String aiSuggestion) {
        String priorityText = getPriorityText(severity);
        String shortDescription = truncateForSpeech(description, 100);
        String shortSuggestion = extractKeyPoints(aiSuggestion);
        
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Hello, this is an automated call from XLBiz Incident Management System.</Say>
                <Pause length="1"/>
                <Say voice="alice">We have detected a %s priority incident with ID %s.</Say>
                <Pause length="1"/>
                <Say voice="alice">Incident description: %s</Say>
                <Pause length="2"/>
                <Say voice="alice">AI recommended solution: %s</Say>
                <Pause length="2"/>
                <Say voice="alice">For complete details and step-by-step instructions, please check the Slack channel and Jira ticket that have been automatically created for this incident.</Say>
                <Pause length="1"/>
                <Say voice="alice">Press 1 to acknowledge this incident, or press 2 to escalate to the next level.</Say>
                <Gather input="dtmf" numDigits="1" action="%s/api/twilio/outbound/response" method="POST">
                    <Say voice="alice">Press 1 to acknowledge, or 2 to escalate.</Say>
                </Gather>
                <Say voice="alice">No response received. This incident remains unacknowledged. Goodbye.</Say>
            </Response>
            """, 
            priorityText,
            incidentId,
            sanitizeForSpeech(shortDescription),
            sanitizeForSpeech(shortSuggestion),
            twilioConfig.getWebhookBaseUrl()
        );
    }

    /**
     * Generate TwiML for incident notification calls (backward compatibility)
     */
    public String generateIncidentNotificationTwiML(String incidentId, String severity, String message) {
        return generateIncidentNotificationTwiML(incidentId, severity, message, null);
    }

    /**
     * Generate TwiML for incident update calls
     */
    public String generateIncidentUpdateTwiML(String incidentId, String status, String message) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Hello, this is an update from XLBiz Incident Management System.</Say>
                <Pause length="1"/>
                <Say voice="alice">Incident %s has been updated to status: %s.</Say>
                <Pause length="1"/>
                <Say voice="alice">Update details: %s</Say>
                <Pause length="2"/>
                <Say voice="alice">Thank you for your attention. Goodbye.</Say>
            </Response>
            """, 
            incidentId,
            status.toLowerCase().replace("_", " "),
            sanitizeForSpeech(message)
        );
    }

    /**
     * Generate default TwiML for outbound calls
     */
    public String generateDefaultTwiML() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Hello, this is a call from XLBiz Incident Management System.</Say>
                <Pause length="1"/>
                <Say voice="alice">Please check your notifications for important updates.</Say>
                <Pause length="1"/>
                <Say voice="alice">Thank you. Goodbye.</Say>
            </Response>
            """;
    }

    /**
     * Convert severity to human-readable priority text
     */
    private String getPriorityText(String severity) {
        if (severity == null) {
            return "unknown";
        }
        
        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return "critical";
            case "HIGH":
                return "high";
            case "MEDIUM":
                return "medium";
            case "LOW":
                return "low";
            default:
                return "unknown";
        }
    }

    /**
     * Sanitize text for speech synthesis
     */
    private String sanitizeForSpeech(String text) {
        if (text == null) {
            return "";
        }
        
        // Remove or replace characters that might cause issues with TTS
        return text
            .replaceAll("[<>&\"']", "") // Remove XML special characters
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();
    }

    /**
     * Truncate text for speech to keep calls concise
     */
    private String truncateForSpeech(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        // Find the last complete sentence within the limit
        String truncated = text.substring(0, maxLength);
        int lastPeriod = truncated.lastIndexOf('.');
        int lastExclamation = truncated.lastIndexOf('!');
        int lastQuestion = truncated.lastIndexOf('?');
        
        int lastSentenceEnd = Math.max(Math.max(lastPeriod, lastExclamation), lastQuestion);
        
        if (lastSentenceEnd > maxLength / 2) {
            return text.substring(0, lastSentenceEnd + 1);
        } else {
            // If no sentence boundary found, truncate at word boundary
            int lastSpace = truncated.lastIndexOf(' ');
            if (lastSpace > maxLength / 2) {
                return text.substring(0, lastSpace) + "...";
            } else {
                return truncated + "...";
            }
        }
    }

    /**
     * Extract key points from AI suggestion for voice delivery
     */
    private String extractKeyPoints(String aiSuggestion) {
        if (aiSuggestion == null || aiSuggestion.trim().isEmpty()) {
            return "Check Slack for detailed solution steps.";
        }
        
        // Look for numbered steps or bullet points
        String[] lines = aiSuggestion.split("\\n");
        StringBuilder keyPoints = new StringBuilder();
        int pointCount = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Extract first few action items or steps
            if (line.matches("^\\d+\\..*") || line.startsWith("- ") || line.startsWith("* ") || 
                line.toLowerCase().contains("step") || line.toLowerCase().contains("action")) {
                
                if (pointCount < 2) { // Limit to 2 key points for voice
                    String cleanLine = line.replaceAll("^[\\d\\-\\*\\.\\s]+", "").trim();
                    if (cleanLine.length() > 10) { // Only meaningful points
                        keyPoints.append(cleanLine);
                        if (!cleanLine.endsWith(".")) {
                            keyPoints.append(".");
                        }
                        keyPoints.append(" ");
                        pointCount++;
                    }
                }
            }
        }
        
        if (keyPoints.length() > 0) {
            return truncateForSpeech(keyPoints.toString().trim(), 150);
        } else {
            // Fallback: extract first meaningful sentence
            String firstSentence = aiSuggestion.split("[.!?]")[0];
            return truncateForSpeech(firstSentence, 100) + ". Check Slack for complete details.";
        }
    }

    /**
     * Check if outbound calling is enabled and configured
     */
    public boolean isOutboundCallingEnabled() {
        return twilioConfig.isEnabled() && 
               twilioConfig.getPhoneNumber() != null && 
               !twilioConfig.getPhoneNumber().isEmpty() &&
               twilioConfig.getWebhookBaseUrl() != null &&
               !twilioConfig.getWebhookBaseUrl().isEmpty();
    }

    /**
     * Get configuration status for debugging
     */
    public Map<String, Object> getOutboundCallConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("twilio_enabled", twilioConfig.isEnabled());
        config.put("phone_number_configured", twilioConfig.getPhoneNumber() != null && !twilioConfig.getPhoneNumber().isEmpty());
        config.put("webhook_url_configured", twilioConfig.getWebhookBaseUrl() != null && !twilioConfig.getWebhookBaseUrl().isEmpty());
        config.put("outbound_calling_enabled", isOutboundCallingEnabled());
        config.put("from_phone_number", twilioConfig.getPhoneNumber());
        config.put("webhook_base_url", twilioConfig.getWebhookBaseUrl());
        return config;
    }
}