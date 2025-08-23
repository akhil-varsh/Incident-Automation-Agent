package com.xlbiz.incident.agent.controller;

import com.xlbiz.incident.agent.config.OutboundCallConfig;
import com.xlbiz.incident.agent.dto.OutboundCallResponse;
import com.xlbiz.incident.agent.service.TwilioOutboundCallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test controller for outbound call functionality
 */
@RestController
@RequestMapping("/api/test/outbound-calls")
public class OutboundCallTestController {

    private static final Logger logger = LoggerFactory.getLogger(OutboundCallTestController.class);

    private final TwilioOutboundCallService twilioOutboundCallService;
    private final OutboundCallConfig outboundCallConfig;

    @Autowired
    public OutboundCallTestController(TwilioOutboundCallService twilioOutboundCallService,
                                    OutboundCallConfig outboundCallConfig) {
        this.twilioOutboundCallService = twilioOutboundCallService;
        this.outboundCallConfig = outboundCallConfig;
    }

    /**
     * Test incident notification call
     */
    @PostMapping("/test-incident-call")
    public ResponseEntity<OutboundCallResponse> testIncidentCall(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(defaultValue = "TEST-001") String incidentId,
            @RequestParam(defaultValue = "HIGH") String severity,
            @RequestParam(defaultValue = "Test incident for outbound call verification") String description,
            @RequestParam(defaultValue = "This is a test AI suggestion for the incident. Please check Slack for full details.") String aiSuggestion) {
        
        logger.info("Testing incident notification call for incident: {}", incidentId);
        
        try {
            // Use provided phone number or default from config
            String targetPhoneNumber = phoneNumber != null ? phoneNumber : 
                outboundCallConfig.getPhoneNumberForSeverity(severity);
            
            if (targetPhoneNumber == null || targetPhoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("No phone number provided and none configured"));
            }
            
            OutboundCallResponse response = twilioOutboundCallService.makeIncidentNotificationCall(
                targetPhoneNumber, incidentId, severity, description, aiSuggestion);
            
            logger.info("Test incident call initiated: {}", response.getCallSid());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to initiate test incident call: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to initiate call: " + e.getMessage()));
        }
    }

    /**
     * Get outbound call configuration status
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getOutboundCallConfig() {
        logger.info("Retrieving outbound call configuration status");
        
        Map<String, Object> config = twilioOutboundCallService.getOutboundCallConfiguration();
        config.put("outbound_config_enabled", outboundCallConfig.isEnabled());
        config.put("outbound_config_configured", outboundCallConfig.isConfigured());
        config.put("developer_phone_configured", outboundCallConfig.getDeveloperPhoneNumber() != null);
        config.put("escalation_phone_configured", outboundCallConfig.getEscalationPhoneNumber() != null);
        
        return ResponseEntity.ok(config);
    }

    /**
     * Test basic outbound call functionality
     */
    @PostMapping("/test-basic-call")
    public ResponseEntity<OutboundCallResponse> testBasicCall(
            @RequestParam(required = false) String phoneNumber) {
        
        logger.info("Testing basic outbound call functionality");
        
        try {
            String targetPhoneNumber = phoneNumber != null ? phoneNumber : 
                outboundCallConfig.getDeveloperPhoneNumber();
            
            if (targetPhoneNumber == null || targetPhoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("No phone number provided and none configured"));
            }
            
            OutboundCallResponse response = twilioOutboundCallService.makeIncidentNotificationCall(
                targetPhoneNumber, 
                "TEST-BASIC", 
                "MEDIUM", 
                "This is a basic test call from XLBiz Incident Management System");
            
            logger.info("Test basic call initiated: {}", response.getCallSid());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to initiate test basic call: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to initiate call: " + e.getMessage()));
        }
    }

    /**
     * Create error response
     */
    private OutboundCallResponse createErrorResponse(String message) {
        OutboundCallResponse response = new OutboundCallResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatus("FAILED");
        return response;
    }
}