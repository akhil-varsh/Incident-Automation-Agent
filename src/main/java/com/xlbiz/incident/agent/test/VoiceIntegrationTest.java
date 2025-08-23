package com.xlbiz.incident.agent.test;

import com.xlbiz.incident.agent.dto.VoiceIncidentResponse;
import com.xlbiz.incident.agent.service.VoiceIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("voice-test")
public class VoiceIntegrationTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(VoiceIntegrationTest.class);

    @Autowired
    private VoiceIntegrationService voiceIntegrationService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== Voice Integration Test Started ===");
        
        if (!voiceIntegrationService.isEnabled()) {
            logger.warn("Voice integration is disabled. Enable it in configuration to run tests.");
            return;
        }
        
        // Test 1: Generate NCCO
        testNccoGeneration();
        
        // Test 2: Process mock voice recording
        testVoiceRecordingProcessing();
        
        logger.info("=== Voice Integration Test Completed ===");
    }

    private void testNccoGeneration() {
        logger.info("Testing NCCO generation...");
        
        try {
            String ncco = voiceIntegrationService.generateAnswerTwiML();
            logger.info("Generated NCCO: {}", ncco);
            
            // Verify NCCO contains expected elements
            if (ncco.contains("talk") && ncco.contains("record")) {
                logger.info("✅ NCCO generation test PASSED");
            } else {
                logger.error("❌ NCCO generation test FAILED - Missing required elements");
            }
            
        } catch (Exception e) {
            logger.error("❌ NCCO generation test FAILED: {}", e.getMessage(), e);
        }
    }

    private void testVoiceRecordingProcessing() {
        logger.info("Testing voice recording processing...");
        
        try {
            // Test with different incident scenarios
            String[] testScenarios = {
                "Critical database outage in production affecting all users",
                "High CPU usage on web servers causing slow response times",
                "Network connectivity issues in the data center",
                "Authentication service is down, users cannot login",
                "Disk space full on application servers"
            };
            
            for (int i = 0; i < testScenarios.length; i++) {
                String scenario = testScenarios[i];
                logger.info("Testing scenario {}: {}", i + 1, scenario);
                
                VoiceIncidentResponse response = voiceIntegrationService.processVoiceRecording(
                    "test://mock-recording-" + i,
                    "test-conversation-" + i,
                    "+1555000" + String.format("%04d", i)
                );
                
                if ("PROCESSED".equals(response.getProcessingStatus())) {
                    logger.info("✅ Scenario {} PASSED - Incident ID: {}, Classification: {}", 
                               i + 1, response.getIncidentId(), response.getAiClassification());
                } else {
                    logger.error("❌ Scenario {} FAILED - Status: {}, Message: {}", 
                                i + 1, response.getProcessingStatus(), response.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ Voice recording processing test FAILED: {}", e.getMessage(), e);
        }
    }
}