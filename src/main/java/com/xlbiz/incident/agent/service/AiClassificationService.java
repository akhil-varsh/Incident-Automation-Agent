package com.xlbiz.incident.agent.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.xlbiz.incident.agent.dto.IncidentRequest;
import com.xlbiz.incident.agent.dto.SimilarityMatch;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentType;
import com.xlbiz.incident.agent.model.KnowledgeEntry;

/**
 * Service for AI-powered incident classification using Groq API.
 * Provides intelligent severity classification, reasoning, and remediation suggestions.
 */
@Service
public class AiClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(AiClassificationService.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    
    @Value("${app.incident.ai.classification-timeout:10}")
    private int classificationTimeoutSeconds;
    
    @Value("${app.incident.ai.use-knowledge-base:true}")
    private boolean useKnowledgeBase;
    
    @Value("${app.incident.ai.store-results:true}")
    private boolean storeResults;

    // Enhanced classification prompt template
    private static final String CLASSIFICATION_PROMPT = """
            You are an expert incident management AI assistant with deep knowledge of IT operations, DevOps, and incident response. 
            Analyze the following incident and provide comprehensive classification and actionable recommendations.
            
            INCIDENT DETAILS:
            - ID: {id}
            - Type: {type}
            - Description: {description}
            - Source: {source}
            - Timestamp: {timestamp}
            - Metadata: {metadata}
            
            SEVERITY CLASSIFICATION GUIDELINES:
            
            HIGH SEVERITY (Immediate Response Required):
            - Complete service outages or critical system failures
            - Security breaches or data corruption incidents
            - Production database connectivity issues
            - Payment system failures or authentication system down
            - Classify as HIGH severity if the incident causes complete service outages, security breaches, production database issues, payment/auth system failures, data loss, or affects >10% of users or critical business operations; provide actionable remediation steps."
            - Data loss or corruption scenarios
            - Infrastructure failures in production environments
            
            MEDIUM SEVERITY (Urgent Response Needed):
            - Partial service degradation or performance issues
            - Non-critical system failures with user impact
            - Production incidents affecting
            - Memory leaks, high CPU usage, or disk space issues in production
            - API failures or deployment issues with moderate impact
            - Network connectivity problems affecting multiple services
            - Staging/pre-production critical issues
            
            LOW SEVERITY (Standard Response):
            - Minor issues with minimal user impact
            - Development or test environment issues
            - Performance degradation affecting.
            - Non-urgent maintenance or configuration issues
            - Monitoring alerts without immediate service impact
            - Documentation or process-related incidents
            
            ANALYSIS FACTORS:
            1. Environment Impact: Production > Staging > Development
            2. User Impact: Number of affected users and business criticality
            3. Service Criticality: Core services (auth, payment, database) vs supporting services
            4. Time Sensitivity: Business hours vs off-hours, SLA requirements
            5. Data Integrity: Risk of data loss or corruption
            6. Security Implications: Potential security risks or breaches
            7. Cascading Effects: Potential to cause other system failures
            
            RESPONSE REQUIREMENTS:
            - Provide specific, actionable remediation steps
            - Include immediate actions, investigation steps, and prevention measures
            - Consider the incident context (time, environment, affected systems)
            - Base suggestions on industry best practices and common resolution patterns
            - Include escalation criteria and stakeholder notification requirements
            
            RESPONSE FORMAT (provide exactly in this format):
            SEVERITY: [LOW|MEDIUM|HIGH]
            CONFIDENCE: [0.0-1.0]
            REASONING: [Detailed explanation of classification decision including key factors considered]
            SUGGESTION: [Comprehensive step-by-step remediation plan with immediate actions, investigation steps, and preventive measures]
            """;

    @Autowired
    public AiClassificationService(ChatClient chatClient, KnowledgeBaseService knowledgeBaseService) {
        this.chatClient = chatClient;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * Classifies incident severity and generates suggestions using AI
     * First searches knowledge base, then uses AI if no match found
     */
    public AiClassificationResult classifyIncident(IncidentRequest request) {
        logger.info("Starting AI classification for incident: {}", request.getId());
        
        try {
            // First, try to find similar incidents in knowledge base
            if (useKnowledgeBase) {
                AiClassificationResult knowledgeResult = searchKnowledgeBase(request);
                if (knowledgeResult != null) {
                    logger.info("Found matching knowledge entry for incident {}, using cached solution", request.getId());
                    return knowledgeResult;
                }
            }
            // Prepare the prompt with incident data
            PromptTemplate promptTemplate = new PromptTemplate(CLASSIFICATION_PROMPT);
            Map<String, Object> promptVariables = Map.of(
                "id", request.getId(),
                "type", request.getType(), // This returns String, not IncidentType
                "description", request.getDescription(),
                "source", request.getSource(),
                "timestamp", request.getTimestamp().toString(),
                "metadata", formatMetadata(request.getMetadata())
            );
            
            Prompt prompt = promptTemplate.create(promptVariables);
            
            // Call Groq API through Spring AI
            logger.debug("Sending classification request to Groq API");
            ChatResponse response = chatClient.call(prompt);
            
            String aiResponse = response.getResult().getOutput().getContent();
            logger.debug("Received AI response: {}", aiResponse);
            
            // Parse AI response
            AiClassificationResult result = parseAiResponse(aiResponse, request);
            
            // Validate and enhance result
            validateAndEnhanceResult(result, request);
            
            // Store successful AI result back to knowledge base for future use
            if (storeResults && result.getConfidence() > 0.7) {
                storeResultInKnowledgeBase(request, result);
            }
            
            logger.info("AI classification completed for incident {}: severity={}, confidence={}", 
                request.getId(), result.getSeverity(), result.getConfidence());
            
            return result;
            
        } catch (Exception e) {
            logger.error("AI classification failed for incident {}: {}", request.getId(), e.getMessage(), e);
            
            // Return a basic result with UNKNOWN severity - no fallback service
            return createFallbackResult(request, "AI classification failed: " + e.getMessage());
        }
    }

    /**
     * Parses severity from Slack message format
     */
    public IncidentSeverity parseSeverityFromSlackMessage(String slackMessage) {
        if (slackMessage == null || slackMessage.trim().isEmpty()) {
            return IncidentSeverity.UNKNOWN;
        }
        
        // Clean up message - remove asterisks and normalize whitespace
        String cleanMessage = slackMessage.replaceAll("\\*", "").replaceAll("\\s+", " ").trim();
        
        try {
            // Pattern to match "Severity: MEDIUM" format in Slack messages
            Pattern severityPattern = Pattern.compile("Severity:\\s*(LOW|MEDIUM|HIGH|CRITICAL)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = severityPattern.matcher(cleanMessage);
            
            if (matcher.find()) {
                String severityStr = matcher.group(1).toUpperCase();
                // Map CRITICAL to HIGH if needed
                if ("CRITICAL".equals(severityStr)) {
                    return IncidentSeverity.HIGH;
                }
                return IncidentSeverity.valueOf(severityStr);
            }
            
            // Fallback: look for severity keywords anywhere in the message
            String upperMessage = cleanMessage.toUpperCase();
            if (upperMessage.contains("SEVERITY: HIGH") || upperMessage.contains("CRITICAL")) {
                return IncidentSeverity.HIGH;
            } else if (upperMessage.contains("SEVERITY: MEDIUM")) {
                return IncidentSeverity.MEDIUM;
            } else if (upperMessage.contains("SEVERITY: LOW")) {
                return IncidentSeverity.LOW;
            }
            
            logger.debug("No severity found in Slack message, returning UNKNOWN");
            return IncidentSeverity.UNKNOWN;
            
        } catch (Exception e) {
            logger.warn("Failed to parse severity from Slack message: {}", e.getMessage());
            return IncidentSeverity.UNKNOWN;
        }
    }

    /**
     * Parses incident details from Slack message format
     */
    public SlackIncidentDetails parseSlackIncidentMessage(String slackMessage) {
        if (slackMessage == null || slackMessage.trim().isEmpty()) {
            return new SlackIncidentDetails();
        }
        
        // Clean up message - remove asterisks and normalize whitespace
        String cleanMessage = slackMessage.replaceAll("\\*", "").replaceAll("\\s+", " ").trim();
        
        SlackIncidentDetails details = new SlackIncidentDetails();
        
        try {
            // Extract Type
            Pattern typePattern = Pattern.compile("Type:\\s*([A-Z_]+)", Pattern.CASE_INSENSITIVE);
            Matcher typeMatcher = typePattern.matcher(cleanMessage);
            if (typeMatcher.find()) {
                details.setType(typeMatcher.group(1));
            }
            
            // Extract Severity
            details.setSeverity(parseSeverityFromSlackMessage(slackMessage));
            
            // Extract Description
            Pattern descPattern = Pattern.compile("Description:\\s*(.+?)(?=Created:|AI Analysis|$)", Pattern.DOTALL);
            Matcher descMatcher = descPattern.matcher(cleanMessage);
            if (descMatcher.find()) {
                details.setDescription(descMatcher.group(1).trim());
            }
            
            // Extract Incident ID
            Pattern idPattern = Pattern.compile("Incident ID:\\s*(\\d+)");
            Matcher idMatcher = idPattern.matcher(cleanMessage);
            if (idMatcher.find()) {
                details.setIncidentId(idMatcher.group(1));
            }
            
            // Extract AI Analysis & Suggestions
            Pattern analysisPattern = Pattern.compile("AI Analysis & Suggestions:\\s*(.+?)(?=Incident ID:|$)", Pattern.DOTALL);
            Matcher analysisMatcher = analysisPattern.matcher(cleanMessage);
            if (analysisMatcher.find()) {
                details.setAiAnalysis(analysisMatcher.group(1).trim());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to parse Slack incident message: {}", e.getMessage());
        }
        
        return details;
    }

    /**
     * Parses the AI response into structured classification result
     */
    private AiClassificationResult parseAiResponse(String aiResponse, IncidentRequest request) {
        AiClassificationResult result = new AiClassificationResult();
        result.setProcessed(true);
        result.setTimestamp(LocalDateTime.now());
        
        try {
            // Extract severity
            Pattern severityPattern = Pattern.compile("SEVERITY:\\s*(LOW|MEDIUM|HIGH)", Pattern.CASE_INSENSITIVE);
            Matcher severityMatcher = severityPattern.matcher(aiResponse);
            if (severityMatcher.find()) {
                result.setSeverity(IncidentSeverity.valueOf(severityMatcher.group(1).toUpperCase()));
            } else {
                result.setSeverity(IncidentSeverity.UNKNOWN);
            }
            
            // Extract confidence
            Pattern confidencePattern = Pattern.compile("CONFIDENCE:\\s*([0-9]*\\.?[0-9]+)");
            Matcher confidenceMatcher = confidencePattern.matcher(aiResponse);
            if (confidenceMatcher.find()) {
                result.setConfidence(Double.parseDouble(confidenceMatcher.group(1)));
            } else {
                result.setConfidence(0.5); // Neutral confidence when not provided
            }
            
            // Extract reasoning
            Pattern reasoningPattern = Pattern.compile("REASONING:\\s*(.+?)(?=SUGGESTION:|$)", Pattern.DOTALL);
            Matcher reasoningMatcher = reasoningPattern.matcher(aiResponse);
            if (reasoningMatcher.find()) {
                result.setReasoning(reasoningMatcher.group(1).trim());
            }
            
            // Extract suggestion
            Pattern suggestionPattern = Pattern.compile("SUGGESTION:\\s*(.+?)$", Pattern.DOTALL);
            Matcher suggestionMatcher = suggestionPattern.matcher(aiResponse);
            if (suggestionMatcher.find()) {
                result.setSuggestion(suggestionMatcher.group(1).trim());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to parse AI response for incident {}: {}", request.getId(), e.getMessage());
            // Set minimal defaults if parsing fails - let AI handle classification logic
            result.setSeverity(IncidentSeverity.UNKNOWN);
            result.setConfidence(0.0);
            result.setReasoning("AI response parsing failed - unable to extract classification details");
            result.setSuggestion("Please review this incident manually for proper classification and resolution steps.");
        }
        
        return result;
    }

    /**
     * Validates the classification result (minimal validation only)
     */
    private void validateAndEnhanceResult(AiClassificationResult result, IncidentRequest request) {
        // Validate confidence score range only
        if (result.getConfidence() < 0.0 || result.getConfidence() > 1.0) {
            logger.warn("Invalid confidence score {} for incident {}, clamping to valid range", 
                result.getConfidence(), request.getId());
            result.setConfidence(Math.max(0.0, Math.min(1.0, result.getConfidence())));
        }
        
        // Ensure we have basic content if AI response was incomplete
        if (result.getSuggestion() == null || result.getSuggestion().trim().isEmpty()) {
            result.setSuggestion("Please review this incident manually for proper resolution steps.");
        }
        
        if (result.getReasoning() == null || result.getReasoning().trim().isEmpty()) {
            result.setReasoning("AI classification completed based on incident analysis");
        }
    }

    /**
     * Formats metadata for inclusion in the AI prompt
     */
    private String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "None provided";
        }
        
        StringBuilder sb = new StringBuilder();
        metadata.forEach((key, value) -> {
            sb.append(key).append(": ").append(value).append("; ");
        });
        
        return sb.toString();
    }

    /**
     * Search knowledge base for similar incidents
     */
    private AiClassificationResult searchKnowledgeBase(IncidentRequest request) {
        try {
            // Search for similar incidents based on description and type
            String searchQuery = request.getDescription() + " " + request.getType();
            var similarityMatches = knowledgeBaseService.searchByDescription(searchQuery, 3);
            
            if (!similarityMatches.isEmpty()) {
                KnowledgeEntry bestMatch = similarityMatches.get(0).getKnowledgeEntry();
                
                // Check if it's a good match (you might want to add similarity scoring)
                if (isGoodMatch(request, bestMatch)) {
                    return convertKnowledgeToResult(bestMatch);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to search knowledge base for incident {}: {}", request.getId(), e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Check if knowledge entry is a good match for the incident
     */
    private boolean isGoodMatch(IncidentRequest request, KnowledgeEntry entry) {
        // Simple matching logic - you can enhance this with more sophisticated similarity scoring
        String requestType = request.getType().toLowerCase();
        String entryType = entry.getPatternType().toLowerCase();
        
        // Check if types match or are similar
        return entryType.contains(requestType) || requestType.contains(entryType) || 
               entry.getConfidenceScore() > 0.8;
    }
    
    /**
     * Convert knowledge entry to classification result
     */
    private AiClassificationResult convertKnowledgeToResult(KnowledgeEntry entry) {
        AiClassificationResult result = new AiClassificationResult();
        result.setSeverity(IncidentSeverity.valueOf(entry.getSeverity()));
        result.setConfidence(entry.getConfidenceScore());
        result.setReasoning("Found similar incident in knowledge base: " + entry.getTitle());
        result.setSuggestion(entry.getSolution());
        result.setProcessed(true);
        result.setTimestamp(LocalDateTime.now());
        
        return result;
    }
    
    /**
     * Store successful AI classification result back to knowledge base
     */
    private void storeResultInKnowledgeBase(IncidentRequest request, AiClassificationResult result) {
        try {
            KnowledgeEntry entry = new KnowledgeEntry();
            entry.setId(UUID.randomUUID().toString());
            entry.setTitle("AI Classified: " + request.getDescription().substring(0, Math.min(50, request.getDescription().length())));
            entry.setPatternType(request.getType());
            entry.setSeverity(result.getSeverity().name());
            entry.setSymptoms(request.getDescription());
            entry.setRootCause(result.getReasoning());
            entry.setSolution(result.getSuggestion());
            entry.setConfidenceScore(result.getConfidence());
            entry.setSuccessRate(0.8); // Default success rate for AI classifications
            entry.setResolutionTimeMinutes(30); // Default resolution time
            entry.setEnvironments(Arrays.asList("production")); // Default environment
            entry.setTechnologies(Arrays.asList("ai-classified")); // Tag as AI classified
            entry.setTags(Arrays.asList("ai-generated", "incident-" + request.getId(), result.getSeverity().name().toLowerCase()));
            
            knowledgeBaseService.addKnowledgeEntry(entry);
            logger.info("Stored AI classification result for incident {} in knowledge base", request.getId());
            
        } catch (Exception e) {
            logger.warn("Failed to store AI result in knowledge base for incident {}: {}", request.getId(), e.getMessage());
        }
    }

    /**
     * Creates a fallback result when AI classification fails
     */
    private AiClassificationResult createFallbackResult(IncidentRequest request, String errorMessage) {
        AiClassificationResult result = new AiClassificationResult();
        result.setSeverity(IncidentSeverity.UNKNOWN);
        result.setConfidence(0.0);
        result.setReasoning("AI classification unavailable: " + errorMessage);
        result.setSuggestion("Please review this incident manually for proper classification and resolution.");
        result.setProcessed(false);
        result.setTimestamp(LocalDateTime.now());
        
        return result;
    }



    /**
     * Classification result data structure
     */
    public static class AiClassificationResult {
        private IncidentSeverity severity;
        private Double confidence;
        private String reasoning;
        private String suggestion;
        private boolean processed;
        private LocalDateTime timestamp;

        // Getters and Setters
        public IncidentSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(IncidentSeverity severity) {
            this.severity = severity;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public boolean isProcessed() {
            return processed;
        }

        public void setProcessed(boolean processed) {
            this.processed = processed;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * Slack incident details data structure
     */
    public static class SlackIncidentDetails {
        private String type;
        private IncidentSeverity severity;
        private String description;
        private String incidentId;
        private String aiAnalysis;

        public SlackIncidentDetails() {
            this.severity = IncidentSeverity.UNKNOWN;
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public IncidentSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(IncidentSeverity severity) {
            this.severity = severity;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getIncidentId() {
            return incidentId;
        }

        public void setIncidentId(String incidentId) {
            this.incidentId = incidentId;
        }

        public String getAiAnalysis() {
            return aiAnalysis;
        }

        public void setAiAnalysis(String aiAnalysis) {
            this.aiAnalysis = aiAnalysis;
        }
    }
}
