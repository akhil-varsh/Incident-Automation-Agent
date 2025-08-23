package com.xlbiz.incident.agent.controller;

import com.xlbiz.incident.agent.dto.VoiceCallRequest;
import com.xlbiz.incident.agent.dto.VoiceIncidentResponse;
import com.xlbiz.incident.agent.dto.VoiceRecordingRequest;
import com.xlbiz.incident.agent.service.VoiceIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceController.class);

    private final VoiceIntegrationService voiceIntegrationService;

    @Autowired
    public VoiceController(VoiceIntegrationService voiceIntegrationService) {
        this.voiceIntegrationService = voiceIntegrationService;
    }

    /**
     * Vonage webhook endpoint for incoming calls
     * Returns NCCO (Nexmo Call Control Object) to handle the call
     */
    @PostMapping(value = "/answer", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleIncomingCall(@RequestBody VoiceCallRequest callRequest,
                                                   HttpServletRequest request) {
        logger.info("Incoming voice call from: {} to: {} (UUID: {})", 
                   callRequest.getFrom(), callRequest.getTo(), callRequest.getUuid());
        
        try {
            if (!voiceIntegrationService.isEnabled()) {
                logger.warn("Voice integration is disabled");
                return ResponseEntity.ok(generateDisabledNcco());
            }
            
            // Log call details for audit
            logger.info("Processing incoming call: {}", callRequest);
            
            // Generate NCCO response for Vonage
            String ncco = voiceIntegrationService.generateAnswerTwiML();
            
            logger.info("Generated NCCO for call UUID: {}", callRequest.getUuid());
            return ResponseEntity.ok(ncco);
            
        } catch (Exception e) {
            logger.error("Error handling incoming call: {}", e.getMessage(), e);
            return ResponseEntity.ok(generateErrorNcco());
        }
    }

    /**
     * Vonage webhook endpoint for call events (optional)
     */
    @PostMapping("/event")
    public ResponseEntity<Void> handleCallEvent(@RequestBody Map<String, Object> eventData,
                                              HttpServletRequest request) {
        logger.info("Received call event: {}", eventData);
        
        try {
            String status = (String) eventData.get("status");
            String uuid = (String) eventData.get("uuid");
            
            logger.info("Call event - UUID: {}, Status: {}", uuid, status);
            
            // You can add call event processing logic here if needed
            // For example, tracking call duration, failed calls, etc.
            
        } catch (Exception e) {
            logger.error("Error processing call event: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Vonage webhook endpoint for recording completion
     * This is where the actual incident processing happens
     */
    @PostMapping("/recording")
    public ResponseEntity<VoiceIncidentResponse> handleRecording(@RequestBody VoiceRecordingRequest recordingRequest,
                                                               HttpServletRequest request) {
        logger.info("Received recording webhook: URL={}, UUID={}, Duration={}s", 
                   recordingRequest.getRecordingUrl(), 
                   recordingRequest.getRecordingUuid(),
                   recordingRequest.getDuration());
        
        try {
            if (!voiceIntegrationService.isEnabled()) {
                logger.warn("Voice integration is disabled");
                VoiceIncidentResponse response = new VoiceIncidentResponse();
                response.setProcessingStatus("DISABLED");
                response.setMessage("Voice integration is currently disabled");
                return ResponseEntity.ok(response);
            }
            
            // Extract caller information (you might need to store this from the answer webhook)
            String callerNumber = extractCallerNumber(recordingRequest);
            
            // Process the voice recording and create incident
            VoiceIncidentResponse response = voiceIntegrationService.processVoiceRecording(
                recordingRequest.getRecordingUrl(),
                recordingRequest.getConversationUuid(),
                callerNumber
            );
            
            logger.info("Voice incident processing completed: {}", response.getIncidentId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing voice recording: {}", e.getMessage(), e);
            
            VoiceIncidentResponse errorResponse = new VoiceIncidentResponse();
            errorResponse.setCallUuid(recordingRequest.getConversationUuid());
            errorResponse.setProcessingStatus("ERROR");
            errorResponse.setMessage("Failed to process recording: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for voice integration
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getVoiceHealth() {
        Map<String, Object> health = Map.of(
            "service", "voice-integration",
            "enabled", voiceIntegrationService.isEnabled(),
            "status", voiceIntegrationService.isEnabled() ? "UP" : "DISABLED",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(health);
    }

    /**
     * Manual test endpoint for voice processing (for development/testing)
     */
    @PostMapping("/test")
    public ResponseEntity<VoiceIncidentResponse> testVoiceProcessing(@RequestParam String transcription,
                                                                   @RequestParam(defaultValue = "+1234567890") String callerNumber) {
        logger.info("Testing voice processing with transcription: {}", transcription);
        
        try {
            // Create a mock recording request for testing
            String testUuid = "test-" + System.currentTimeMillis();
            
            // Process as if it came from a real voice call
            VoiceIncidentResponse response = voiceIntegrationService.processVoiceRecording(
                "test://mock-recording-url",
                testUuid,
                callerNumber
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in voice test: {}", e.getMessage(), e);
            
            VoiceIncidentResponse errorResponse = new VoiceIncidentResponse();
            errorResponse.setProcessingStatus("ERROR");
            errorResponse.setMessage("Test failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Helper methods

    private String generateDisabledNcco() {
        return """
            [
              {
                "action": "talk",
                "text": "Sorry, the incident hotline is currently unavailable. Please contact your system administrator or use alternative reporting methods. Goodbye.",
                "voiceName": "Amy"
              }
            ]
            """;
    }

    private String generateErrorNcco() {
        return """
            [
              {
                "action": "talk",
                "text": "We're experiencing technical difficulties with the incident hotline. Please try again later or contact your system administrator. Goodbye.",
                "voiceName": "Amy"
              }
            ]
            """;
    }

    private String extractCallerNumber(VoiceRecordingRequest recordingRequest) {
        // In a real implementation, you would need to correlate this with the original call
        // For now, return a placeholder - you might need to store call data in a cache/database
        return "+1234567890"; // This should be extracted from the original call data
    }
}