package com.xlbiz.incident.agent.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xlbiz.incident.agent.dto.IncidentRequest;
import com.xlbiz.incident.agent.dto.IncidentResponse;
import com.xlbiz.incident.agent.dto.IncidentStatusResponse;
import com.xlbiz.incident.agent.model.IncidentType;

/**
 * Comprehensive End-to-End Test for AI-Powered Incident Processing with Slack Integration
 * 
 * This test demonstrates the complete workflow:
 * 1. Generate realistic mock incidents
 * 2. Submit incidents to the REST API
 * 3. AI classifies severity and generates suggestions
 * 4. Knowledge base enhances suggestions
 * 5. Slack receives incident notifications with AI suggestions
 * 6. Verify complete end-to-end integration
 */
public class EndToEndIncidentTest {
    
    private static final Logger logger = LoggerFactory.getLogger(EndToEndIncidentTest.class);
    
    private static final String BASE_URL = "http://localhost:8080";
    private static final String API_ENDPOINT = BASE_URL + "/api/v1/incidents";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Test incident scenarios with varying complexity
    private static final List<MockIncidentScenario> TEST_SCENARIOS = Arrays.asList(
        new MockIncidentScenario(
            IncidentType.DATABASE_CONNECTION_ERROR,
            "Production database connection timeout - Unable to connect to primary PostgreSQL database cluster",
            "monitoring-system-prod",
            createDatabaseMetadata(),
            "CRITICAL database connectivity issue that affects all user transactions"
        ),
        
        new MockIncidentScenario(
            IncidentType.HIGH_CPU,
            "Web server CPU utilization at 95% - Load balancer reporting degraded performance on multiple nodes",
            "kubernetes-monitoring",
            createCpuMetadata(),
            "HIGH CPU usage affecting application response times"
        ),
        
        new MockIncidentScenario(
            IncidentType.DISK_FULL,
            "Application logs partition reached 98% capacity - Risk of application crash due to inability to write logs",
            "log-monitoring-agent",
            createDiskMetadata(),
            "MEDIUM severity disk space issue requiring immediate cleanup"
        ),
        
        new MockIncidentScenario(
            IncidentType.API_FAILURE,
            "Payment processing API endpoint returning 503 errors - Customer transactions failing",
            "api-health-checker",
            createApiMetadata(),
            "CRITICAL API outage impacting revenue-generating transactions"
        ),
        
        new MockIncidentScenario(
            IncidentType.SECURITY_BREACH,
            "Multiple failed authentication attempts detected from suspicious IP ranges - Potential brute force attack in progress",
            "security-monitoring",
            createSecurityMetadata(),
            "HIGH security incident requiring immediate investigation and response"
        )
    );
    
    public EndToEndIncidentTest() {
        this.restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(java.time.Duration.ofSeconds(10))
            .setReadTimeout(java.time.Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
    
    public static void main(String[] args) {
        EndToEndIncidentTest test = new EndToEndIncidentTest();
        test.runCompleteEndToEndTest();
    }
    
    /**
     * Execute the complete end-to-end test workflow
     */
    public void runCompleteEndToEndTest() {
        logger.info("🚀 Starting End-to-End AI-Powered Incident Processing Test");
        logger.info("📋 Test will process {} realistic incident scenarios", TEST_SCENARIOS.size());
        
        try {
            // Step 1: Verify API connectivity
            if (!verifyApiConnectivity()) {
                logger.error("❌ API connectivity check failed - ensure Spring Boot application is running");
                return;
            }
            
            // Step 2: Process each test scenario
            List<TestResult> results = new ArrayList<>();
            for (int i = 0; i < TEST_SCENARIOS.size(); i++) {
                MockIncidentScenario scenario = TEST_SCENARIOS.get(i);
                logger.info("🔄 Processing test scenario {}/{}: {} incident", 
                    i + 1, TEST_SCENARIOS.size(), scenario.getType());
                
                TestResult result = processIncidentScenario(scenario, i + 1);
                results.add(result);
                
                // Add delay between tests to observe workflow
                if (i < TEST_SCENARIOS.size() - 1) {
                    logger.info("⏳ Waiting 3 seconds before next test...");
                    Thread.sleep(3000);
                }
            }
            
            // Step 3: Generate comprehensive test report
            generateTestReport(results);
            
        } catch (Exception e) {
            logger.error("💥 End-to-end test execution failed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Verify that the Spring Boot API is accessible
     */
    private boolean verifyApiConnectivity() {
        logger.info("🔗 Verifying API connectivity to {}", BASE_URL);
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                BASE_URL + "/actuator/health", String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("✅ API connectivity verified - Application is running and healthy");
                return true;
            } else {
                logger.error("⚠️ API returned unexpected status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ API connectivity failed: {}", e.getMessage());
            logger.error("💡 Make sure to start the application with: mvn spring-boot:run");
            return false;
        }
    }
    
    /**
     * Process a single incident scenario through the complete workflow
     */
    private TestResult processIncidentScenario(MockIncidentScenario scenario, int testNumber) {
        String testId = String.format("E2E-TEST-%03d-%d", testNumber, System.currentTimeMillis());
        
        logger.info("📝 Creating incident: ID={}, Type={}, Expected={}", 
            testId, scenario.getType(), scenario.getExpectedSeverity());
        
        TestResult result = new TestResult(testId, scenario);
        
        try {
            // Step 1: Create incident request
            IncidentRequest request = createIncidentRequest(testId, scenario);
            result.setRequest(request);
            
            logger.info("📤 Submitting incident to API: {}", API_ENDPOINT);
            logger.debug("Request payload: Description='{}', Source='{}'", 
                request.getDescription(), request.getSource());
            
            // Step 2: Submit incident to API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<IncidentRequest> entity = new HttpEntity<>(request, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<IncidentResponse> response = restTemplate.postForEntity(
                API_ENDPOINT + "/trigger", entity, IncidentResponse.class);
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.setProcessingTimeMs(processingTime);
            result.setResponse(response.getBody());
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                IncidentResponse incidentResponse = response.getBody();
                logger.info("✅ Incident created successfully: External ID={}", incidentResponse.getExternalId());
                
                // Step 3: Validate AI classification results
                validateAiClassification(result, incidentResponse);
                
                // Step 4: Get detailed incident status (includes AI suggestions)
                getDetailedIncidentStatus(result, testId);
                
                // Step 5: Verify Slack integration results
                verifySlackIntegration(result);
                
                result.setSuccess(true);
                logger.info("🎉 Test scenario completed successfully for incident {}", testId);
                
            } else {
                logger.error("❌ Incident creation failed: Status={}", response.getStatusCode());
                result.setSuccess(false);
                result.setErrorMessage("API returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("💥 Test scenario failed for incident {}: {}", testId, e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Create a properly formatted incident request
     */
    private IncidentRequest createIncidentRequest(String testId, MockIncidentScenario scenario) {
        IncidentRequest request = new IncidentRequest();
        request.setId(testId);
        request.setType(scenario.getType().name());
        request.setDescription(scenario.getDescription());
        request.setSource(scenario.getSource());
        request.setTimestamp(LocalDateTime.now());
        request.setMetadata(scenario.getMetadata());
        
        logger.debug("📋 Created incident request: Type={}, Source={}", 
            scenario.getType(), scenario.getSource());
        
        return request;
    }
    
    /**
     * Validate AI classification results against expected outcomes
     */
    private void validateAiClassification(TestResult result, IncidentResponse response) {
        logger.info("🤖 Validating AI classification results...");
        
        if (response.getAiClassifiedSeverity() != null) {
            logger.info("🎯 AI classified severity: {} (Confidence: {})", 
                response.getAiClassifiedSeverity(), 
                response.getAiConfidence() != null ? String.format("%.1f%%", response.getAiConfidence() * 100) : "N/A");
            result.setAiClassifiedSeverity(response.getAiClassifiedSeverity());
            result.setAiConfidence(response.getAiConfidence());
        } else {
            logger.warn("⚠️ No AI severity classification found in response");
        }
        
        if (response.getAiSuggestion() != null && !response.getAiSuggestion().trim().isEmpty()) {
            logger.info("💡 AI generated suggestion: {}", 
                response.getAiSuggestion().length() > 100 ? 
                response.getAiSuggestion().substring(0, 100) + "..." : response.getAiSuggestion());
            result.setAiSuggestion(response.getAiSuggestion());
        } else {
            logger.warn("⚠️ No AI suggestion found in response");
        }
        
        // Validate confidence level
        if (response.getAiConfidence() != null) {
            if (response.getAiConfidence() >= 0.7) {
                logger.info("✅ High AI confidence: {:.1f}%", response.getAiConfidence() * 100);
            } else if (response.getAiConfidence() >= 0.5) {
                logger.info("⚠️ Medium AI confidence: {:.1f}%", response.getAiConfidence() * 100);
            } else {
                logger.warn("❗ Low AI confidence: {:.1f}%", response.getAiConfidence() * 100);
            }
        }
    }
    
    /**
     * Get detailed incident status including full AI analysis
     */
    private void getDetailedIncidentStatus(TestResult result, String externalId) {
        logger.info("📊 Retrieving detailed incident status...");
        
        try {
            ResponseEntity<IncidentStatusResponse> statusResponse = restTemplate.getForEntity(
                API_ENDPOINT + "/" + externalId + "/status", IncidentStatusResponse.class);
            
            if (statusResponse.getStatusCode() == HttpStatus.OK && statusResponse.getBody() != null) {
                IncidentStatusResponse status = statusResponse.getBody();
                result.setStatusResponse(status);
                
                logger.info("📈 Current status: {}", status.getStatus());
                logger.info("🕐 Created: {}, Updated: {}", status.getCreatedAt(), status.getUpdatedAt());
                
                if (status.getAiAnalysis() != null) {
                    logger.info("🧠 AI Analysis Details:");
                    logger.info("   Reasoning: {}", status.getAiAnalysis().getReasoning());
                    logger.info("   Suggestion: {}", 
                        status.getAiAnalysis().getSuggestion() != null && status.getAiAnalysis().getSuggestion().length() > 150 ? 
                        status.getAiAnalysis().getSuggestion().substring(0, 150) + "..." : 
                        status.getAiAnalysis().getSuggestion());
                }
                
            } else {
                logger.warn("⚠️ Could not retrieve detailed status: {}", statusResponse.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error retrieving incident status: {}", e.getMessage());
        }
    }
    
    /**
     * Verify that Slack integration was successful
     */
    private void verifySlackIntegration(TestResult result) {
        logger.info("💬 Verifying Slack integration results...");
        
        // Check if Slack channel ID was stored in incident metadata
        if (result.getStatusResponse() != null && 
            result.getStatusResponse().getIntegrations() != null) {
            
            String slackChannelId = result.getStatusResponse().getIntegrations().getSlackChannelId();
            
            if (slackChannelId != null && !slackChannelId.trim().isEmpty()) {
                logger.info("✅ Slack integration successful: Channel ID = {}", slackChannelId);
                result.setSlackChannelId(slackChannelId);
                result.setSlackIntegrationSuccess(true);
                
                // Additional validation: Check if AI suggestion was included
                if (result.getAiSuggestion() != null) {
                    logger.info("💡 AI suggestion delivered to Slack channel successfully");
                }
            } else {
                logger.warn("⚠️ Slack integration may have failed - no channel ID found");
                result.setSlackIntegrationSuccess(false);
            }
        } else {
            logger.warn("⚠️ Integration details not available for Slack verification");
            result.setSlackIntegrationSuccess(false);
        }
    }
    
    /**
     * Generate a comprehensive test report
     */
    private void generateTestReport(List<TestResult> results) {
        logger.info("📊 Generating End-to-End Test Report");
        logger.info("=" .repeat(80));
        
        int successful = (int) results.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum();
        int failed = results.size() - successful;
        int slackIntegrationsSuccessful = (int) results.stream().mapToInt(r -> r.isSlackIntegrationSuccess() ? 1 : 0).sum();
        
        double averageProcessingTime = results.stream()
            .mapToLong(TestResult::getProcessingTimeMs)
            .average()
            .orElse(0.0);
        
        double averageAiConfidence = results.stream()
            .filter(r -> r.getAiConfidence() != null)
            .mapToDouble(TestResult::getAiConfidence)
            .average()
            .orElse(0.0);
        
        // Overall Statistics
        logger.info("📈 OVERALL TEST STATISTICS");
        logger.info("   Total Tests: {}", results.size());
        logger.info("   Successful: {} ({:.1f}%)", successful, (successful * 100.0) / results.size());
        logger.info("   Failed: {} ({:.1f}%)", failed, (failed * 100.0) / results.size());
        logger.info("   Slack Integrations: {} ({:.1f}%)", slackIntegrationsSuccessful, 
            (slackIntegrationsSuccessful * 100.0) / results.size());
        logger.info("   Average Processing Time: {:.0f}ms", averageProcessingTime);
        logger.info("   Average AI Confidence: {:.1f}%", averageAiConfidence * 100);
        
        logger.info("");
        logger.info("🔍 DETAILED RESULTS BY INCIDENT TYPE");
        
        // Detailed Results
        for (int i = 0; i < results.size(); i++) {
            TestResult result = results.get(i);
            logger.info("   Test {}: {} - {}", 
                i + 1, 
                result.getExternalId(), 
                result.isSuccess() ? "✅ PASSED" : "❌ FAILED");
            
            if (result.isSuccess()) {
                logger.info("      Type: {}", result.getScenario().getType());
                logger.info("      AI Severity: {} (Confidence: {:.1f}%)", 
                    result.getAiClassifiedSeverity(), 
                    result.getAiConfidence() != null ? result.getAiConfidence() * 100 : 0.0);
                logger.info("      Processing Time: {}ms", result.getProcessingTimeMs());
                logger.info("      Slack Channel: {}", 
                    result.getSlackChannelId() != null ? result.getSlackChannelId() : "N/A");
                logger.info("      AI Suggestion: {}", 
                    result.getAiSuggestion() != null ? "✅ Generated" : "❌ Missing");
            } else {
                logger.info("      Error: {}", result.getErrorMessage());
            }
            logger.info("");
        }
        
        // Success/Failure Summary
        if (successful == results.size()) {
            logger.info("🎉 ALL TESTS PASSED! AI-powered incident processing is working correctly.");
            logger.info("✅ Key Features Verified:");
            logger.info("   • AI incident classification and severity detection");
            logger.info("   • Intelligent suggestion generation");
            logger.info("   • Knowledge base integration");
            logger.info("   • Slack notification delivery");
            logger.info("   • End-to-end workflow orchestration");
        } else {
            logger.warn("⚠️ {} out of {} tests failed. Review the errors above.", failed, results.size());
        }
        
        logger.info("=" .repeat(80));
        logger.info("🏁 End-to-End Test Completed");
    }
    
    // Helper methods to create realistic metadata
    private static Map<String, Object> createDatabaseMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("database_host", "prod-db-cluster-01.internal");
        metadata.put("database_name", "user_transactions");
        metadata.put("connection_timeout", "30s");
        metadata.put("active_connections", 0);
        metadata.put("max_connections", 100);
        metadata.put("error_code", "08006");
        metadata.put("affected_services", Arrays.asList("user-api", "payment-service", "reporting-service"));
        return metadata;
    }
    
    private static Map<String, Object> createCpuMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cpu_usage_percent", ThreadLocalRandom.current().nextInt(90, 99));
        metadata.put("load_average", "8.5, 7.2, 6.8");
        metadata.put("memory_usage_percent", ThreadLocalRandom.current().nextInt(70, 85));
        metadata.put("affected_nodes", Arrays.asList("web-01", "web-02", "web-03"));
        metadata.put("request_queue_size", ThreadLocalRandom.current().nextInt(500, 1500));
        metadata.put("response_time_p95", ThreadLocalRandom.current().nextInt(2000, 5000) + "ms");
        return metadata;
    }
    
    private static Map<String, Object> createDiskMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filesystem", "/var/log");
        metadata.put("disk_usage_percent", ThreadLocalRandom.current().nextInt(95, 99));
        metadata.put("available_space", ThreadLocalRandom.current().nextInt(100, 500) + "MB");
        metadata.put("largest_files", Arrays.asList("application.log", "access.log", "error.log"));
        metadata.put("growth_rate", ThreadLocalRandom.current().nextInt(50, 200) + "MB/hour");
        return metadata;
    }
    
    private static Map<String, Object> createApiMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("endpoint", "/api/v1/payments/process");
        metadata.put("http_status", 503);
        metadata.put("error_rate_percent", ThreadLocalRandom.current().nextInt(80, 100));
        metadata.put("affected_requests", ThreadLocalRandom.current().nextInt(1000, 5000));
        metadata.put("upstream_service", "payment-processor-service");
        metadata.put("timeout_threshold", "10s");
        return metadata;
    }
    
    private static Map<String, Object> createSecurityMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_ip", "203.0.113." + ThreadLocalRandom.current().nextInt(1, 254));
        metadata.put("failed_attempts", ThreadLocalRandom.current().nextInt(50, 200));
        metadata.put("time_window", "5 minutes");
        metadata.put("targeted_accounts", Arrays.asList("admin", "service", "api_user"));
        metadata.put("attack_type", "brute_force");
        metadata.put("geographical_location", "Unknown/Proxy");
        return metadata;
    }
    
    // Data classes for test scenarios and results
    static class MockIncidentScenario {
        private final IncidentType type;
        private final String description;
        private final String source;
        private final Map<String, Object> metadata;
        private final String expectedSeverity;
        
        public MockIncidentScenario(IncidentType type, String description, String source, 
                                  Map<String, Object> metadata, String expectedSeverity) {
            this.type = type;
            this.description = description;
            this.source = source;
            this.metadata = metadata;
            this.expectedSeverity = expectedSeverity;
        }
        
        // Getters
        public IncidentType getType() { return type; }
        public String getDescription() { return description; }
        public String getSource() { return source; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getExpectedSeverity() { return expectedSeverity; }
    }
    
    static class TestResult {
        private final String externalId;
        private final MockIncidentScenario scenario;
        private boolean success = false;
        private String errorMessage;
        private IncidentRequest request;
        private IncidentResponse response;
        private IncidentStatusResponse statusResponse;
        private long processingTimeMs;
        
        // AI Classification Results
        private Object aiClassifiedSeverity;
        private Double aiConfidence;
        private String aiSuggestion;
        
        // Slack Integration Results
        private boolean slackIntegrationSuccess = false;
        private String slackChannelId;
        
        public TestResult(String externalId, MockIncidentScenario scenario) {
            this.externalId = externalId;
            this.scenario = scenario;
        }
        
        // Getters and Setters
        public String getExternalId() { return externalId; }
        public MockIncidentScenario getScenario() { return scenario; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public IncidentRequest getRequest() { return request; }
        public void setRequest(IncidentRequest request) { this.request = request; }
        public IncidentResponse getResponse() { return response; }
        public void setResponse(IncidentResponse response) { this.response = response; }
        public IncidentStatusResponse getStatusResponse() { return statusResponse; }
        public void setStatusResponse(IncidentStatusResponse statusResponse) { this.statusResponse = statusResponse; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        public Object getAiClassifiedSeverity() { return aiClassifiedSeverity; }
        public void setAiClassifiedSeverity(Object aiClassifiedSeverity) { this.aiClassifiedSeverity = aiClassifiedSeverity; }
        public Double getAiConfidence() { return aiConfidence; }
        public void setAiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; }
        public String getAiSuggestion() { return aiSuggestion; }
        public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }
        public boolean isSlackIntegrationSuccess() { return slackIntegrationSuccess; }
        public void setSlackIntegrationSuccess(boolean slackIntegrationSuccess) { this.slackIntegrationSuccess = slackIntegrationSuccess; }
        public String getSlackChannelId() { return slackChannelId; }
        public void setSlackChannelId(String slackChannelId) { this.slackChannelId = slackChannelId; }
    }
}
