package com.xlbiz.incident.agent.controller;

import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Record;
import com.twilio.twiml.voice.Say;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import java.net.URI;
import com.xlbiz.incident.agent.config.TwilioConfig;
import com.xlbiz.incident.agent.dto.VoiceIncidentResponse;
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
@RequestMapping("/api/twilio/voice")
public class TwilioVoiceController {

    private static final Logger logger = LoggerFactory.getLogger(TwilioVoiceController.class);

    private final TwilioConfig twilioConfig;
    private final VoiceIntegrationService voiceIntegrationService;

    @Autowired
    public TwilioVoiceController(TwilioConfig twilioConfig, 
                               VoiceIntegrationService voiceIntegrationService) {
        this.twilioConfig = twilioConfig;
        this.voiceIntegrationService = voiceIntegrationService;
    }

    /**
     * Twilio webhook endpoint for incoming calls
     * Returns TwiML response to handle the call
     */
    @PostMapping(value = "/incoming", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleIncomingCall(@RequestParam(required = false) String From,
                                                   @RequestParam(required = false) String To,
                                                   @RequestParam(required = false) String CallSid,
                                                   @RequestParam(required = false) String CallStatus,
                                                   HttpServletRequest request) {
        
        logger.info("Incoming Twilio call from: {} to: {} (CallSid: {}, Status: {})", From, To, CallSid, CallStatus);
        
        try {
            if (!twilioConfig.isEnabled()) {
                logger.warn("Twilio integration is disabled");
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(generateDisabledTwiML());
            }
            
            // Generate TwiML response for Twilio
            String twiml = generateAnswerTwiML();
            
            logger.info("Generated TwiML for call SID: {}", CallSid);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
            
        } catch (Exception e) {
            logger.error("Error handling incoming call: {}", e.getMessage(), e);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(generateErrorTwiML());
        }
    }

    /**
     * Twilio webhook endpoint for recording completion
     */
    @PostMapping("/recording")
    public ResponseEntity<String> handleRecording(@RequestParam(required = false) String RecordingUrl,
                                                @RequestParam(required = false) String CallSid,
                                                @RequestParam(required = false) String From,
                                                @RequestParam(required = false) String RecordingDuration,
                                                HttpServletRequest request) {
        
        logger.info("Received Twilio recording: URL={}, CallSid={}, From={}, Duration={}", 
                   RecordingUrl, CallSid, From, RecordingDuration);
        
        try {
            if (!twilioConfig.isEnabled()) {
                logger.warn("Twilio integration is disabled");
                return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
            }
            
            // Process the voice recording and create incident
            VoiceIncidentResponse response = voiceIntegrationService.processVoiceRecording(
                RecordingUrl,
                CallSid,
                From
            );
            
            logger.info("Twilio voice incident processing completed: {}", response.getIncidentId());
            
            // Return empty TwiML response (Twilio expects XML)
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
            
        } catch (Exception e) {
            logger.error("Error processing Twilio recording: {}", e.getMessage(), e);
            
            // Return empty TwiML response even on error
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
        }
    }

    /**
     * Twilio webhook endpoint for call status updates
     */
    @PostMapping("/status")
    public ResponseEntity<String> handleCallStatus(@RequestParam(required = false) String CallSid,
                                                 @RequestParam(required = false) String CallStatus,
                                                 @RequestParam(required = false) String From,
                                                 @RequestParam(required = false) String To,
                                                 HttpServletRequest request) {
        
        logger.info("Twilio call status update - CallSid: {}, Status: {}, From: {}, To: {}", 
                   CallSid, CallStatus, From, To);
        
        // You can add call status tracking logic here if needed
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
    }

    /**
     * Health check endpoint for Twilio integration
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getTwilioHealth() {
        Map<String, Object> health = Map.of(
            "service", "twilio-voice-integration",
            "enabled", twilioConfig.isEnabled(),
            "phone_number", twilioConfig.getPhoneNumber(),
            "status", twilioConfig.isEnabled() ? "UP" : "DISABLED",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(health);
    }

    /**
     * Demo endpoint to simulate a complete call flow for testing/demo purposes
     */
    @PostMapping("/demo-call")
    public ResponseEntity<Map<String, Object>> simulateCall(@RequestParam(defaultValue = "+918985710576") String fromNumber,
                                                           @RequestParam(defaultValue = "Critical database outage in production affecting all users") String transcription) {
        logger.info("Simulating call from: {} with transcription: {}", fromNumber, transcription);
        
        try {
            // Step 1: Simulate incoming call
            String callSid = "DEMO-" + System.currentTimeMillis();
            logger.info("Demo call started - CallSid: {}", callSid);
            
            // Step 2: Generate TwiML (what user would hear)
            String twiml = generateAnswerTwiML();
            
            // Step 3: Try to simulate recording processing, with fallback
            Map<String, Object> demoResult;
            
            try {
                VoiceIncidentResponse response = voiceIntegrationService.processVoiceRecording(
                    "demo://mock-recording-url",
                    callSid,
                    fromNumber
                );
                
                demoResult = Map.of(
                    "call_sid", callSid,
                    "from_number", fromNumber,
                    "twiml_response", twiml,
                    "incident_created", response.getIncidentId(),
                    "ai_classification", response.getAiClassification(),
                    "processing_status", response.getProcessingStatus(),
                    "message", "Demo call completed successfully - This simulates the complete voice incident flow"
                );
                
            } catch (Exception voiceError) {
                logger.warn("VoiceIntegrationService error, using fallback demo: {}", voiceError.getMessage());
                
                // Fallback demo response
                demoResult = Map.of(
                    "call_sid", callSid,
                    "from_number", fromNumber,
                    "twiml_response", twiml,
                    "transcription", transcription,
                    "demo_mode", true,
                    "message", "Demo mode - Voice integration simulated (VoiceIntegrationService not fully configured)",
                    "note", "In production, this would create incident, Slack channel, and Jira ticket"
                );
            }
            
            logger.info("Demo call completed: {}", callSid);
            return ResponseEntity.ok(demoResult);
            
        } catch (Exception e) {
            logger.error("Error in demo call: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = Map.of(
                "error", "Demo call failed",
                "message", e.getMessage(),
                "from_number", fromNumber,
                "call_sid", "DEMO-ERROR-" + System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    /**
     * Simple test endpoint to verify TwiML generation
     */
    @GetMapping("/test-twiml")
    public ResponseEntity<String> testTwiML() {
        try {
            String twiml = generateAnswerTwiML();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
        } catch (Exception e) {
            logger.error("Error generating TwiML: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error generating TwiML: " + e.getMessage());
        }
    }

    /**
     * TwiML endpoint for outbound calls
     */
    @GetMapping(value = "/outbound-twiml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getOutboundTwiML(@RequestParam(defaultValue = "This is a test call from XLBiz Incident Automation System") String message) {
        logger.info("Serving outbound TwiML with message: {}", message);
        
        try {
            String twiml = generateOutboundTwiML(message);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
        } catch (Exception e) {
            logger.error("Error generating outbound TwiML: {}", e.getMessage(), e);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(generateErrorTwiML());
        }
    }

    /**
     * Make an outbound call to a specified number
     */
    @PostMapping("/outbound-call")
    public ResponseEntity<Map<String, Object>> makeOutboundCall(@RequestParam String toNumber,
                                                               @RequestParam(defaultValue = "This is a test call from XLBiz Incident Automation System") String message) {
        logger.info("Making outbound call to: {} with message: {}", toNumber, message);
        
        try {
            if (!twilioConfig.isEnabled()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Twilio integration is disabled",
                    "enabled", false
                ));
            }
            
            // Create TwiML URL with the message as parameter
            String twimlUrl = twilioConfig.getWebhookBaseUrl() + "/api/twilio/voice/outbound-twiml?message=" + 
                             java.net.URLEncoder.encode(message, "UTF-8");
            
            // Create the call
            Call call = Call.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(twilioConfig.getPhoneNumber()),
                URI.create(twimlUrl)
            ).create();
            
            logger.info("Outbound call created successfully - CallSid: {}", call.getSid());
            
            Map<String, Object> response = Map.of(
                "call_sid", call.getSid(),
                "to_number", toNumber,
                "from_number", twilioConfig.getPhoneNumber(),
                "status", call.getStatus().toString(),
                "message", message,
                "twiml_url", twimlUrl,
                "created_at", call.getDateCreated().toString()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error making outbound call: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "error", "Failed to make outbound call",
                "message", e.getMessage(),
                "to_number", toNumber
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Helper methods

    private String generateAnswerTwiML() {
        String recordingUrl = twilioConfig.getWebhookBaseUrl() + "/api/twilio/voice/recording";
        
        try {
            VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("Hello, you have reached the XLBiz Incident Automation Hotline. " +
                                   "Please describe your incident clearly after the beep. " +
                                   "You have up to " + twilioConfig.getRecording().getMaxDurationSeconds() + " seconds. " +
                                   "Press the star key when finished.")
                    .voice(Say.Voice.ALICE)
                    .build())
                .record(new Record.Builder()
                    .action(recordingUrl)
                    .method(com.twilio.http.HttpMethod.POST)
                    .timeout(twilioConfig.getRecording().getTimeoutSeconds())
                    .maxLength(twilioConfig.getRecording().getMaxDurationSeconds())
                    .finishOnKey("*")
                    .build())
                .say(new Say.Builder("Thank you. Your incident has been recorded and will be processed immediately. " +
                                   "You will receive updates via Slack and Jira. Goodbye.")
                    .voice(Say.Voice.ALICE)
                    .build())
                .build();
            
            return response.toXml();
            
        } catch (Exception e) {
            logger.error("Error generating TwiML: {}", e.getMessage(), e);
            return generateErrorTwiML();
        }
    }

    private String generateDisabledTwiML() {
        try {
            VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("Sorry, the incident hotline is currently unavailable. " +
                                   "Please contact your system administrator or use alternative reporting methods. Goodbye.")
                    .voice(Say.Voice.ALICE)
                    .build())
                .build();
            
            return response.toXml();
            
        } catch (Exception e) {
            logger.error("Error generating disabled TwiML: {}", e.getMessage(), e);
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>Service unavailable</Say></Response>";
        }
    }

    private String generateErrorTwiML() {
        try {
            VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("We're experiencing technical difficulties with the incident hotline. " +
                                   "Please try again later or contact your system administrator. Goodbye.")
                    .voice(Say.Voice.ALICE)
                    .build())
                .build();
            
            return response.toXml();
            
        } catch (Exception e) {
            logger.error("Error generating error TwiML: {}", e.getMessage(), e);
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>Technical difficulties</Say></Response>";
        }
    }

    private String generateOutboundTwiML(String message) {
        try {
            VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder(message)
                    .voice(Say.Voice.ALICE)
                    .build())
                .build();
            
            return response.toXml();
            
        } catch (Exception e) {
            logger.error("Error generating outbound TwiML: {}", e.getMessage(), e);
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>" + message + "</Say></Response>";
        }
    }
}