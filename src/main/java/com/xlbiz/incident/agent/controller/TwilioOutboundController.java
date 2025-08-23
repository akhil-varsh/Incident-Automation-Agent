package com.xlbiz.incident.agent.controller;

import com.xlbiz.incident.agent.dto.OutboundCallRequest;
import com.xlbiz.incident.agent.dto.OutboundCallResponse;
import com.xlbiz.incident.agent.service.TwilioOutboundCallService;
import com.xlbiz.incident.agent.model.VoiceCall;
import com.xlbiz.incident.agent.model.VoiceProcessingStatus;
import com.xlbiz.incident.agent.repository.VoiceCallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/twilio/outbound")
public class TwilioOutboundController {

    private static final Logger logger = LoggerFactory.getLogger(TwilioOutboundController.class);

    private final TwilioOutboundCallService outboundCallService;
    private final VoiceCallRepository voiceCallRepository;

    @Autowired
    public TwilioOutboundController(TwilioOutboundCallService outboundCallService,
                                  VoiceCallRepository voiceCallRepository) {
        this.outboundCallService = outboundCallService;
        this.voiceCallRepository = voiceCallRepository;
    }

    /**
     * Make an outbound call for incident notification
     */
    @PostMapping("/call/incident-notification")
    public ResponseEntity<OutboundCallResponse> makeIncidentNotificationCall(@RequestBody OutboundCallRequest request) {
        logger.info("Received request to make incident notification call to: {}", request.getToPhoneNumber());
        
        try {
            OutboundCallResponse response = outboundCallService.makeIncidentNotificationCall(
                request.getToPhoneNumber(),
                request.getIncidentId(),
                request.getSeverity(),
                request.getMessage()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to make incident notification call: {}", e.getMessage(), e);
            
            OutboundCallResponse errorResponse = new OutboundCallResponse(request.getToPhoneNumber(), "INCIDENT_NOTIFICATION");
            errorResponse.setSuccess(false);
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("Failed to initiate call: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Make an outbound call for incident update
     */
    @PostMapping("/call/incident-update")
    public ResponseEntity<OutboundCallResponse> makeIncidentUpdateCall(@RequestBody OutboundCallRequest request) {
        logger.info("Received request to make incident update call to: {}", request.getToPhoneNumber());
        
        try {
            OutboundCallResponse response = outboundCallService.makeIncidentUpdateCall(
                request.getToPhoneNumber(),
                request.getIncidentId(),
                request.getStatus(),
                request.getMessage()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to make incident update call: {}", e.getMessage(), e);
            
            OutboundCallResponse errorResponse = new OutboundCallResponse(request.getToPhoneNumber(), "INCIDENT_UPDATE");
            errorResponse.setSuccess(false);
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("Failed to initiate call: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Make a custom outbound call
     */
    @PostMapping("/call/custom")
    public ResponseEntity<OutboundCallResponse> makeCustomCall(@RequestBody OutboundCallRequest request) {
        logger.info("Received request to make custom call to: {}", request.getToPhoneNumber());
        
        try {
            OutboundCallResponse response = outboundCallService.makeCustomCall(
                request.getToPhoneNumber(),
                request.getTwimlUrl(),
                request.getParameters()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to make custom call: {}", e.getMessage(), e);
            
            OutboundCallResponse errorResponse = new OutboundCallResponse(request.getToPhoneNumber(), "CUSTOM");
            errorResponse.setSuccess(false);
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("Failed to initiate call: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * TwiML endpoint for incident notification calls
     */
    @RequestMapping(value = "/twiml/incident-notification", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getIncidentNotificationTwiML(
            @RequestParam String incidentId,
            @RequestParam String severity,
            @RequestParam String message) {
        
        logger.info("Generating incident notification TwiML for incident: {}", incidentId);
        
        try {
            String twiml = outboundCallService.generateIncidentNotificationTwiML(incidentId, severity, message);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
                
        } catch (Exception e) {
            logger.error("Failed to generate incident notification TwiML: {}", e.getMessage(), e);
            
            String errorTwiml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="alice">Sorry, there was an error processing this call. Please try again later.</Say>
                </Response>
                """;
            
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_XML)
                .body(errorTwiml);
        }
    }

    /**
     * TwiML endpoint for enhanced incident notification calls with AI suggestions
     * Handles both GET and POST requests from Twilio
     */
    @RequestMapping(value = "/twiml/incident-notification-enhanced", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getIncidentNotificationEnhancedTwiML(
            @RequestParam(required = false) String incidentId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String aiSuggestion,
            @RequestParam(required = false) String CallSid,
            @RequestParam(required = false) String From,
            @RequestParam(required = false) String To) {
        
        logger.info("Generating enhanced incident notification TwiML - Incident: {}, CallSid: {}, From: {}, To: {}", 
            incidentId, CallSid, From, To);
        
        try {
            // Use defaults if parameters are missing (fallback for POST requests)
            String finalIncidentId = incidentId != null ? incidentId : "UNKNOWN";
            String finalSeverity = severity != null ? severity : "HIGH";
            String finalMessage = message != null ? message : "An incident requires your attention";
            String finalAiSuggestion = aiSuggestion != null ? aiSuggestion : "Check Slack for detailed solution steps";
            
            String twiml = outboundCallService.generateIncidentNotificationTwiML(
                finalIncidentId, finalSeverity, finalMessage, finalAiSuggestion);
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
                
        } catch (Exception e) {
            logger.error("Error generating enhanced incident notification TwiML for incident {}: {}", 
                incidentId, e.getMessage(), e);
            
            // Return a fallback TwiML
            String fallbackTwiml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="alice">Hello, this is an automated call from XLBiz Incident Management System.</Say>
                    <Pause length="1"/>
                    <Say voice="alice">There is an incident that requires your attention. Please check your notifications.</Say>
                    <Pause length="1"/>
                    <Say voice="alice">Thank you. Goodbye.</Say>
                </Response>
                """;
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(fallbackTwiml);
        }
    }

    /**
     * TwiML endpoint for incident update calls
     */
    @RequestMapping(value = "/twiml/incident-update", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getIncidentUpdateTwiML(
            @RequestParam String incidentId,
            @RequestParam String status,
            @RequestParam String message) {
        
        logger.info("Generating incident update TwiML for incident: {}", incidentId);
        
        try {
            String twiml = outboundCallService.generateIncidentUpdateTwiML(incidentId, status, message);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
                
        } catch (Exception e) {
            logger.error("Failed to generate incident update TwiML: {}", e.getMessage(), e);
            
            String errorTwiml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="alice">Sorry, there was an error processing this call. Please try again later.</Say>
                </Response>
                """;
            
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_XML)
                .body(errorTwiml);
        }
    }

    /**
     * Default TwiML endpoint
     */
    @RequestMapping(value = "/twiml/default", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getDefaultTwiML() {
        logger.info("Generating default TwiML");
        
        String twiml = outboundCallService.generateDefaultTwiML();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(twiml);
    }

    /**
     * Custom TwiML endpoint
     */
    @RequestMapping(value = "/twiml/custom", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getCustomTwiML(@RequestParam Map<String, String> parameters) {
        logger.info("Generating custom TwiML with parameters: {}", parameters);
        
        // Generate custom TwiML based on parameters
        String message = parameters.getOrDefault("message", "This is a custom message from XLBiz Incident Management System.");
        
        String twiml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">%s</Say>
                <Pause length="1"/>
                <Say voice="alice">Thank you. Goodbye.</Say>
            </Response>
            """, message);
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(twiml);
    }

    /**
     * Handle call status callbacks from Twilio
     */
    @PostMapping("/status")
    public ResponseEntity<String> handleCallStatus(@RequestParam Map<String, String> parameters) {
        String callSid = parameters.get("CallSid");
        String callStatus = parameters.get("CallStatus");
        String to = parameters.get("To");
        String from = parameters.get("From");
        String callDuration = parameters.get("CallDuration");
        
        logger.info("Call status update - SID: {}, Status: {}, To: {}, From: {}, Duration: {}", 
            callSid, callStatus, to, from, callDuration);
        
        try {
            // Find or create voice call record
            java.util.Optional<VoiceCall> existingCall = voiceCallRepository.findByCallSid(callSid);
            VoiceCall voiceCall;
            
            if (existingCall.isPresent()) {
                voiceCall = existingCall.get();
                logger.debug("Updating existing voice call record for CallSid: {}", callSid);
            } else {
                // Create new voice call record
                voiceCall = new VoiceCall();
                voiceCall.setCallSid(callSid);
                voiceCall.setConversationUuid(callSid); // Use CallSid as conversation UUID for outbound calls
                voiceCall.setPhoneNumber(to);
                voiceCall.setCallerNumber(to); // Set caller number for outbound calls
                voiceCall.setDirection("outbound");
                voiceCall.setCallType("INCIDENT_NOTIFICATION");
                voiceCall.setCreatedAt(java.time.LocalDateTime.now());
                logger.debug("Creating new voice call record for CallSid: {}", callSid);
            }
            
            // Update call status and timing
            voiceCall.setTwilioStatus(callStatus);
            voiceCall.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Map Twilio status to our processing status
            VoiceProcessingStatus processingStatus = mapTwilioStatusToProcessingStatus(callStatus);
            voiceCall.setProcessingStatus(processingStatus);
            
            // Update duration if provided
            if (callDuration != null && !callDuration.isEmpty()) {
                try {
                    voiceCall.setDurationSeconds(Integer.parseInt(callDuration));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid call duration format: {}", callDuration);
                }
            }
            
            // Set end time for completed calls
            if ("completed".equalsIgnoreCase(callStatus) || 
                "failed".equalsIgnoreCase(callStatus) || 
                "canceled".equalsIgnoreCase(callStatus) ||
                "busy".equalsIgnoreCase(callStatus) ||
                "no-answer".equalsIgnoreCase(callStatus)) {
                
                if (voiceCall.getEndedAt() == null) {
                    voiceCall.setEndedAt(java.time.LocalDateTime.now());
                }
            }
            
            // Save the updated voice call record
            voiceCallRepository.save(voiceCall);
            
            logger.info("Voice call status updated successfully - CallSid: {}, Status: {}", 
                callSid, processingStatus);
            
        } catch (Exception e) {
            logger.error("Error processing outbound call status callback for CallSid {}: {}", 
                callSid, e.getMessage(), e);
            // Don't return error to Twilio - we don't want them to retry
        }
        
        return ResponseEntity.ok("OK");
    }

    /**
     * Map Twilio call status to our internal processing status
     */
    private VoiceProcessingStatus mapTwilioStatusToProcessingStatus(String twilioStatus) {
        if (twilioStatus == null) {
            return VoiceProcessingStatus.PENDING;
        }
        
        switch (twilioStatus.toLowerCase()) {
            case "queued":
            case "initiated":
                return VoiceProcessingStatus.PENDING;
                
            case "ringing":
                return VoiceProcessingStatus.PROCESSING;
                
            case "in-progress":
            case "answered":
                return VoiceProcessingStatus.PROCESSING;
                
            case "completed":
                return VoiceProcessingStatus.COMPLETED;
                
            case "failed":
            case "canceled":
            case "busy":
            case "no-answer":
                return VoiceProcessingStatus.FAILED;
                
            default:
                logger.warn("Unknown Twilio call status: {}", twilioStatus);
                return VoiceProcessingStatus.PENDING;
        }
    }

    /**
     * Handle user responses during calls (DTMF input)
     */
    @PostMapping("/response")
    public ResponseEntity<String> handleCallResponse(
            @RequestParam(required = false) String Digits,
            @RequestParam(required = false) String CallSid,
            @RequestParam(required = false) String From,
            @RequestParam Map<String, String> allParams) {
        
        logger.info("🎯 DTMF RESPONSE RECEIVED!");
        logger.info("Digits: {}", Digits);
        logger.info("CallSid: {}", CallSid);
        logger.info("From: {}", From);
        logger.info("All parameters: {}", allParams);
        
        try {
        
            String responseTwiml;
            
            if ("1".equals(Digits)) {
                // Incident acknowledged
                logger.info("✅ Incident acknowledged by developer via call: {}", CallSid);
                responseTwiml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                        <Say voice="alice">Thank you for acknowledging this incident. You will receive detailed information via Slack and Jira.</Say>
                        <Pause length="1"/>
                        <Say voice="alice">Please begin working on the resolution. Goodbye.</Say>
                    </Response>
                    """;
            } else if ("2".equals(Digits)) {
                // Incident escalated
                logger.info("⬆️ Incident escalated by developer via call: {}", CallSid);
                responseTwiml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                        <Say voice="alice">This incident has been escalated to the next level. The escalation team will be notified immediately.</Say>
                        <Pause length="1"/>
                        <Say voice="alice">Thank you. Goodbye.</Say>
                    </Response>
                    """;
            } else {
                // Invalid or no response
                logger.warn("❌ Invalid DTMF response received: {} from call: {}", Digits, CallSid);
                responseTwiml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                        <Say voice="alice">Invalid response received. This incident remains unacknowledged.</Say>
                        <Pause length="1"/>
                        <Say voice="alice">Please check your notifications for incident details. Goodbye.</Say>
                    </Response>
                    """;
            }
            
            logger.info("🎵 Returning TwiML response: {}", responseTwiml.substring(0, Math.min(100, responseTwiml.length())));
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(responseTwiml);
                
        } catch (Exception e) {
            logger.error("💥 ERROR in handleCallResponse: {}", e.getMessage(), e);
            
            // Return a simple fallback TwiML
            String errorTwiml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="alice">Thank you for your response. Goodbye.</Say>
                </Response>
                """;
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(errorTwiml);
        }
    }

    /**
     * Get outbound calling configuration status
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = outboundCallService.getOutboundCallConfiguration();
        return ResponseEntity.ok(config);
    }

    /**
     * Test endpoint to verify webhook connectivity
     */
    @GetMapping("/test")
    public ResponseEntity<String> testWebhook() {
        logger.info("Webhook test endpoint called successfully");
        return ResponseEntity.ok("Webhook is working! Current time: " + java.time.LocalDateTime.now());
    }

    /**
     * Simple debug response endpoint that always works
     */
    @RequestMapping(value = "/debug-response", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> debugResponse(@RequestParam Map<String, String> allParams) {
        logger.info("🔍 DEBUG RESPONSE ENDPOINT CALLED");
        logger.info("All parameters: {}", allParams);
        
        String twiml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Debug response received successfully. Thank you. Goodbye.</Say>
            </Response>
            """;
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(twiml);
    }

    /**
     * Test TwiML generation
     */
    @RequestMapping(value = "/test-twiml", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> testTwiML() {
        logger.info("Test TwiML endpoint called");
        
        String testTwiml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Hello! This is a test call from XLBiz Incident Management System.</Say>
                <Pause length="1"/>
                <Say voice="alice">If you can hear this message, the webhook is working correctly.</Say>
                <Pause length="1"/>
                <Say voice="alice">Thank you. Goodbye.</Say>
            </Response>
            """;
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(testTwiml);
    }

    /**
     * Main TwiML App endpoint - this is what should be configured in Twilio TwiML App
     * This endpoint handles the initial call and DTMF responses
     */
    @RequestMapping(value = "/twiml-app", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> twimlAppHandler(
            @RequestParam(required = false) String Digits,
            @RequestParam(required = false) String CallSid,
            @RequestParam(required = false) String From,
            @RequestParam(required = false) String To,
            @RequestParam Map<String, String> allParams) {
        
        logger.info("🎵 TwiML App Handler called");
        logger.info("Digits: {}, CallSid: {}, From: {}, To: {}", Digits, CallSid, From, To);
        logger.info("All parameters: {}", allParams);
        
        // If we have DTMF input, handle it
        if (Digits != null && !Digits.trim().isEmpty()) {
            logger.info("🎯 Processing DTMF input in TwiML App: {}", Digits);
            return handleDtmfInput(Digits);
        }
        
        // Otherwise, provide the initial incident notification with DTMF options
        String selfUrl = outboundCallService.getOutboundCallConfiguration()
            .get("webhook_base_url") + "/api/twilio/outbound/twiml-app";
        
        logger.info("🔗 Using self-referencing URL: {}", selfUrl);
        
        String twiml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Hello, this is an automated call from XLBiz Incident Management System.</Say>
                <Pause length="1"/>
                <Say voice="alice">We have detected a high priority incident that requires your immediate attention.</Say>
                <Pause length="2"/>
                <Say voice="alice">Press 1 to acknowledge this incident, or press 2 to escalate to the next level.</Say>
                <Gather input="dtmf" numDigits="1" action="%s" method="POST" timeout="15">
                    <Say voice="alice">Please press 1 to acknowledge, or press 2 to escalate.</Say>
                    <Pause length="2"/>
                    <Say voice="alice">Press 1 for acknowledge, or 2 for escalate.</Say>
                </Gather>
                <Say voice="alice">No response received. This incident remains unacknowledged. Please check your notifications for important updates. Thank you. Goodbye.</Say>
            </Response>
            """, selfUrl);
        
        logger.info("🎵 Generated TwiML App response");
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(twiml);
    }

    /**
     * Simple incident notification TwiML (no parameters needed)
     * Following Twilio best practices for DTMF handling
     */
    @RequestMapping(value = "/twiml/simple-incident", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSimpleIncidentTwiML(
            @RequestParam(required = false) String Digits,
            @RequestParam(required = false) String CallSid,
            @RequestParam Map<String, String> allParams) {
        
        logger.info("🎵 Simple incident TwiML called - Digits: {}, CallSid: {}", Digits, CallSid);
        logger.info("🔍 All parameters: {}", allParams);
        
        // If this is a response to DTMF input, handle it here
        if (Digits != null && !Digits.trim().isEmpty()) {
            logger.info("🎯 Processing DTMF input: {}", Digits);
            return handleDtmfInput(Digits);
        }
        
        // Otherwise, generate the initial prompt
        String dtmfHandlerUrl = outboundCallService.getOutboundCallConfiguration()
            .get("webhook_base_url") + "/api/twilio/outbound/twiml/dtmf-handler";
        
        logger.info("🔗 Using dedicated DTMF handler URL: {}", dtmfHandlerUrl);
        
        String twiml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Hello, this is an automated call from XLBiz Incident Management System.</Say>
                <Pause length="1"/>
                <Say voice="alice">We have detected a high priority incident that requires your immediate attention.</Say>
                <Pause length="2"/>
                <Say voice="alice">Press 1 to acknowledge this incident, or press 2 to escalate to the next level.</Say>
                <Gather input="dtmf" numDigits="1" action="%s" method="POST" timeout="15">
                    <Say voice="alice">Please press 1 to acknowledge, or press 2 to escalate.</Say>
                    <Pause length="2"/>
                    <Say voice="alice">Press 1 for acknowledge, or 2 for escalate.</Say>
                </Gather>
                <Say voice="alice">No response received. This incident remains unacknowledged. Please check your notifications for important updates. Thank you. Goodbye.</Say>
            </Response>
            """, dtmfHandlerUrl);
        
        logger.info("🎵 Generated initial TwiML prompt with self-referencing URL");
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(twiml);
    }

    /**
     * Dedicated DTMF handler endpoint - separate from main TwiML
     */
    @RequestMapping(value = "/twiml/dtmf-handler", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleDtmfResponse(
            @RequestParam(required = false) String Digits,
            @RequestParam(required = false) String CallSid,
            @RequestParam Map<String, String> allParams) {
        
        logger.info("🎯 DTMF Handler called - Digits: {}, CallSid: {}", Digits, CallSid);
        logger.info("🔍 All DTMF parameters: {}", allParams);
        
        return handleDtmfInput(Digits);
    }

    /**
     * Handle DTMF input following Twilio best practices
     */
    private ResponseEntity<String> handleDtmfInput(String digits) {
        logger.info("🎯 Processing DTMF digits: {}", digits);
        
        String twiml;
        
        if ("1".equals(digits)) {
            logger.info("✅ Incident acknowledged");
            twiml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="alice">Thank you for acknowledging this incident.</Say>
                    <Pause length="1"/>
                    <Say voice="alice">You will receive detailed information via Slack and Jira.</Say>
                    <Pause length="1"/>
                    <Say voice="alice">Please begin working on the resolution. Goodbye.</Say>
                </Response>
                """;
        } else if ("2".equals(digits)) {
            logger.info("⬆️ Incident escalated");
            twiml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="alice">This incident has been escalated to the next level.</Say>
                    <Pause length="1"/>
                    <Say voice="alice">The escalation team will be notified immediately.</Say>
                    <Pause length="1"/>
                    <Say voice="alice">Thank you. Goodbye.</Say>
                </Response>
                """;
        } else {
            logger.warn("❌ Invalid DTMF input: {}", digits);
            // Give them another chance with clearer instructions
            String retryUrl = outboundCallService.getOutboundCallConfiguration()
                .get("webhook_base_url") + "/api/twilio/outbound/twiml/dtmf-handler";
                
            twiml = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="alice">I didn't understand that input. Let me repeat the options.</Say>
                    <Pause length="1"/>
                    <Say voice="alice">Press 1 to acknowledge this incident, or press 2 to escalate.</Say>
                    <Gather input="dtmf" numDigits="1" action="%s" method="POST" timeout="10">
                        <Say voice="alice">Press 1 for acknowledge, or 2 for escalate.</Say>
                    </Gather>
                    <Say voice="alice">No response received. This incident remains unacknowledged. Please check your notifications. Goodbye.</Say>
                </Response>
                """, retryUrl);
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(twiml);
    }

    /**
     * Get incident details for outbound calling
     */
    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<Map<String, Object>> getIncidentForCall(@PathVariable String incidentId) {
        logger.info("Fetching incident details for outbound call: {}", incidentId);
        
        try {
            // This would typically fetch from your incident service
            // For now, return a mock response structure
            Map<String, Object> incidentDetails = new HashMap<>();
            incidentDetails.put("id", incidentId);
            incidentDetails.put("status", "OPEN");
            incidentDetails.put("severity", "HIGH");
            incidentDetails.put("type", "DATABASE_CONNECTION_ERROR");
            incidentDetails.put("description", "Database connection failure detected");
            incidentDetails.put("affectedServices", java.util.Arrays.asList("user-service", "payment-service"));
            incidentDetails.put("estimatedResolutionTime", "30 minutes");
            incidentDetails.put("assignedTeam", "DevOps Team");
            
            return ResponseEntity.ok(incidentDetails);
            
        } catch (Exception e) {
            logger.error("Failed to fetch incident details for {}: {}", incidentId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}