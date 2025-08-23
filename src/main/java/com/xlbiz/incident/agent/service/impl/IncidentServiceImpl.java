package com.xlbiz.incident.agent.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xlbiz.incident.agent.dto.IncidentRequest;
import com.xlbiz.incident.agent.dto.IncidentResponse;
import com.xlbiz.incident.agent.dto.IncidentStats;
import com.xlbiz.incident.agent.dto.IncidentStatusResponse;
import com.xlbiz.incident.agent.dto.IncidentSummary;
import com.xlbiz.incident.agent.dto.SimilarityMatch;
import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentStatus;
import com.xlbiz.incident.agent.model.IncidentType;
import com.xlbiz.incident.agent.repository.IncidentRepository;
import com.xlbiz.incident.agent.service.AiClassificationService;
import com.xlbiz.incident.agent.service.IncidentService;
import com.xlbiz.incident.agent.service.KnowledgeBaseService;
import com.xlbiz.incident.agent.service.TwilioOutboundCallService;
import com.xlbiz.incident.agent.service.VectorSearchKnowledgeBaseService;
import com.xlbiz.incident.agent.service.SlackIntegrationService;
import com.xlbiz.incident.agent.config.OutboundCallConfig;

/**
 * Basic implementation of IncidentService.
 * This is a simplified implementation for Task 3 - the full AI/integration logic will be added in later tasks.
 */
@Service
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentServiceImpl.class);

    private final IncidentRepository incidentRepository;
    private final AiClassificationService aiClassificationService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorSearchKnowledgeBaseService vectorSearchService;
    private final SlackIntegrationService slackIntegrationService;
    private final TwilioOutboundCallService twilioOutboundCallService;
    private final OutboundCallConfig outboundCallConfig;

    @Autowired
    public IncidentServiceImpl(IncidentRepository incidentRepository, 
                             AiClassificationService aiClassificationService,
                             KnowledgeBaseService knowledgeBaseService,
                             VectorSearchKnowledgeBaseService vectorSearchService,
                             SlackIntegrationService slackIntegrationService,
                             TwilioOutboundCallService twilioOutboundCallService,
                             OutboundCallConfig outboundCallConfig) {
        this.incidentRepository = incidentRepository;
        this.aiClassificationService = aiClassificationService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.vectorSearchService = vectorSearchService;
        this.slackIntegrationService = slackIntegrationService;
        this.twilioOutboundCallService = twilioOutboundCallService;
        this.outboundCallConfig = outboundCallConfig;
    }

    @Override
    public IncidentResponse processIncident(IncidentRequest request) {
        logger.info("Processing incident with external ID: {}", request.getId());

        try {
            // Check if incident already exists
            if (incidentRepository.existsByExternalId(request.getId())) {
                logger.warn("Incident with external ID {} already exists", request.getId());
                return createErrorResponse(request.getId(), "Incident already exists");
            }

            // Create initial incident with minimal data for vector search
            Incident incident = new Incident();
            incident.setExternalId(request.getId());
            incident.setDescription(request.getDescription());
            incident.setSource(request.getSource());
            incident.setIncidentTimestamp(request.getTimestamp());
            incident.setMetadata(request.getMetadata());
            incident.setStatus(IncidentStatus.RECEIVED);
            
            // Set initial type and severity to UNKNOWN for vector search
            incident.setType(IncidentType.OTHER);
            incident.setSeverity(IncidentSeverity.UNKNOWN);
            
            incident = incidentRepository.save(incident);
            logger.info("Incident {} saved with internal ID: {} - starting vector search", request.getId(), incident.getId());

            // STEP 1: Search ChromaDB for similar incidents using vector embeddings
            logger.info("Searching ChromaDB for similar incidents to: {}", request.getId());
            List<SimilarityMatch> similarIncidents = vectorSearchService.searchWithEmbeddings(request.getDescription(), 5);
            
            boolean foundGoodMatch = false;
            String solution = null;
            
            if (!similarIncidents.isEmpty()) {
                // Check if we have a high-confidence match (similarity > 0.8)
                SimilarityMatch bestMatch = similarIncidents.get(0);
                if (bestMatch.getSimilarityScore() > 0.8) {
                    foundGoodMatch = true;
                    solution = buildSolutionFromKnowledgeBase(bestMatch, similarIncidents);
                    
                    // Update incident with knowledge base solution
                    incident.setAiSuggestion(solution);
                    incident.setAiConfidence(bestMatch.getSimilarityScore());
                    incident.setAiReasoning("High similarity match found in knowledge base (similarity: " + 
                                          String.format("%.2f", bestMatch.getSimilarityScore()) + ")");
                    
                    // Try to infer type and severity from knowledge base
                    if (bestMatch.getKnowledgeEntry().getPatternType() != null) {
                        try {
                            IncidentType inferredType = IncidentType.valueOf(bestMatch.getKnowledgeEntry().getPatternType());
                            incident.setType(inferredType);
                        } catch (IllegalArgumentException e) {
                            incident.setType(IncidentType.OTHER);
                        }
                    }
                    
                    if (bestMatch.getKnowledgeEntry().getSeverity() != null) {
                        try {
                            IncidentSeverity inferredSeverity = IncidentSeverity.valueOf(bestMatch.getKnowledgeEntry().getSeverity());
                            incident.setSeverity(inferredSeverity);
                        } catch (IllegalArgumentException e) {
                            incident.setSeverity(IncidentSeverity.MEDIUM);
                        }
                    }
                    
                    incident.setStatus(IncidentStatus.PROCESSING);
                    logger.info("Found high-confidence knowledge base match for incident {} (similarity: {:.2f})", 
                              request.getId(), bestMatch.getSimilarityScore());
                }
            }
            
            // STEP 2: If no good match found, use AI classification
            AiClassificationService.AiClassificationResult classificationResult = null;
            if (!foundGoodMatch) {
                logger.info("No high-confidence match found in knowledge base, proceeding with AI classification for incident: {}", request.getId());
                
                incident.setStatus(IncidentStatus.CLASSIFYING);
                incident = incidentRepository.save(incident);
                
                // Perform AI classification
                classificationResult = aiClassificationService.classifyIncident(request);
                
                // Update incident with AI classification results
                if (classificationResult.getSeverity() != IncidentSeverity.UNKNOWN) {
                    incident.setSeverity(classificationResult.getSeverity());
                }
                
                // Parse and validate incident type from AI or request
                IncidentType incidentType = IncidentType.OTHER;
                if (request.getType() != null && !request.getType().trim().isEmpty()) {
                    incidentType = IncidentType.fromString(request.getType());
                }
                incident.setType(incidentType);
                
                incident.setAiSuggestion(classificationResult.getSuggestion());
                incident.setAiReasoning(classificationResult.getReasoning());
                incident.setAiConfidence(classificationResult.getConfidence());
                
                // Enhance AI suggestion with lower-confidence knowledge base matches if available
                if (!similarIncidents.isEmpty()) {
                    String enhancedSuggestion = enhanceAiSuggestionWithKnowledge(
                        classificationResult.getSuggestion(), similarIncidents);
                    incident.setAiSuggestion(enhancedSuggestion);
                    logger.info("Enhanced AI suggestion with knowledge base recommendations for incident {}", request.getId());
                }
                
                incident.setStatus(IncidentStatus.PROCESSING);
            }
            
            incident = incidentRepository.save(incident);
            
            // Log completion with appropriate details
            if (foundGoodMatch) {
                logger.info("Knowledge base match processing completed for incident {}: type={}, severity={}, confidence={}", 
                    request.getId(), incident.getType(), incident.getSeverity(), incident.getAiConfidence());
            } else if (classificationResult != null) {
                logger.info("AI classification and knowledge base search completed for incident {}: severity={}, confidence={}", 
                    request.getId(), classificationResult.getSeverity(), classificationResult.getConfidence());
            }

            // Integrate with Slack for incident notifications and collaboration
            logger.info("Starting Slack integration for incident {}", request.getId());
            SlackIntegrationService.SlackIntegrationResult slackResult = 
                slackIntegrationService.processIncidentSlackIntegration(incident, incident.getAiSuggestion());
            
            if (slackResult.isSuccessful()) {
                logger.info("Slack integration successful for incident {}: channel={}", 
                    request.getId(), slackResult.getChannelId());
                
                // Store the Slack channel ID in incident metadata for future reference
                if (slackResult.getChannelId() != null) {
                    if (incident.getMetadata() == null) {
                        incident.setMetadata(new java.util.HashMap<>());
                    }
                    incident.getMetadata().put("slackChannelId", slackResult.getChannelId());
                    logger.debug("Stored Slack channel ID {} for incident {}", 
                        slackResult.getChannelId(), request.getId());
                }
                
                incident.setStatus(IncidentStatus.PROCESSING); // Keep as PROCESSING but with Slack active
            } else {
                logger.warn("Slack integration failed for incident {}: {}", 
                    request.getId(), slackResult.getErrorMessage());
                // Continue processing even if Slack integration fails
            }
            
            incident = incidentRepository.save(incident);

            // FINAL STEP: Trigger outbound call notification after incident is fully processed
            // Only call if we have a complete incident with description and AI suggestions
            if (incident.getAiSuggestion() != null && !incident.getAiSuggestion().trim().isEmpty() && 
                outboundCallConfig.isConfigured()) {
                
                logger.info("Triggering outbound call notification for fully processed incident {}", request.getId());
                
                try {
                    // Get appropriate phone number based on severity
                    String phoneNumber = outboundCallConfig.getPhoneNumberForSeverity(incident.getSeverity().name());
                    
                    if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                        twilioOutboundCallService.makeIncidentNotificationCall(
                            phoneNumber,
                            incident.getExternalId(),
                            incident.getSeverity().name(),
                            incident.getDescription(),
                            incident.getAiSuggestion()
                        );
                        
                        logger.info("Outbound call notification initiated for incident {} to {}", 
                            request.getId(), phoneNumber);
                    } else {
                        logger.warn("No phone number configured for severity {} - skipping outbound call for incident {}", 
                            incident.getSeverity(), request.getId());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to initiate outbound call for incident {}: {}", 
                        request.getId(), e.getMessage());
                    // Don't fail the incident processing if outbound call fails
                }
            } else {
                if (incident.getAiSuggestion() == null || incident.getAiSuggestion().trim().isEmpty()) {
                    logger.info("Skipping outbound call for incident {} - no AI suggestion available", request.getId());
                } else if (!outboundCallConfig.isConfigured()) {
                    logger.info("Skipping outbound call for incident {} - outbound calling not configured", request.getId());
                }
            }

            // Create response with AI classification results
            IncidentResponse response = new IncidentResponse(
                incident.getId(),
                incident.getExternalId(),
                incident.getStatus()
            );
            response.setAiClassifiedSeverity(incident.getSeverity());
            response.setAiSuggestion(incident.getAiSuggestion());
            response.setAiConfidence(incident.getAiConfidence());
            response.setMessage("Incident processed with AI classification and notifications sent - ready for developer action");

            return response;

        } catch (Exception e) {
            logger.error("Error processing incident {}: {}", request.getId(), e.getMessage(), e);
            return createErrorResponse(request.getId(), "Internal server error: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncidentStatusResponse> getIncidentStatus(String externalId) {
        logger.debug("Getting status for incident: {}", externalId);
        
        return incidentRepository.findByExternalId(externalId)
            .map(incident -> new IncidentStatusResponse(incident));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentSummary> listIncidents(
            IncidentType type,
            IncidentSeverity severity,
            IncidentStatus status,
            String source,
            // LocalDateTime startDate,
            // LocalDateTime endDate,
            Pageable pageable) {
        
        logger.debug("Listing incidents with filters - type: {}, severity: {}, status: {}", type, severity, status);
        
        Page<Incident> incidentPage = incidentRepository.findByCriteria(
            type, severity, status, source, pageable
        );
        
        List<IncidentSummary> summaries = incidentPage.getContent().stream()
            .map(incident -> new IncidentSummary(incident))
            .collect(Collectors.toList());
        
        return new PageImpl<>(summaries, pageable, incidentPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentStats getIncidentStatistics() {
        logger.debug("Generating incident statistics");
        
        IncidentStats stats = new IncidentStats();
        
        try {
            // Get basic counts using repository statistics query
            Object[] statsArray = incidentRepository.getIncidentStatistics(LocalDateTime.now().minusHours(24));
            
            if (statsArray != null && statsArray.length >= 5) {
                IncidentStats.Summary summary = stats.getSummary();
                summary.setTotalIncidents(((Number) statsArray[0]).longValue());
                summary.setActiveIncidents(((Number) statsArray[1]).longValue());
                summary.setHighSeverityIncidents(((Number) statsArray[2]).longValue());
                summary.setResolvedIncidents(((Number) statsArray[3]).longValue());
                summary.setRecentIncidents24h(((Number) statsArray[4]).longValue());
            }
            
            // Get counts by status
            List<Object[]> statusCounts = incidentRepository.countByStatus();
            for (Object[] row : statusCounts) {
                stats.getByStatus().put(row[0].toString(), ((Number) row[1]).longValue());
            }
            
            // Get counts by severity
            List<Object[]> severityCounts = incidentRepository.countBySeverity();
            for (Object[] row : severityCounts) {
                stats.getBySeverity().put(row[0].toString(), ((Number) row[1]).longValue());
            }
            
            // Get counts by type
            List<Object[]> typeCounts = incidentRepository.countByType();
            for (Object[] row : typeCounts) {
                stats.getByType().put(row[0].toString(), ((Number) row[1]).longValue());
            }
            
            // Get counts by source
            List<Object[]> sourceCounts = incidentRepository.countBySource();
            for (Object[] row : sourceCounts) {
                stats.getBySource().put(row[0].toString(), ((Number) row[1]).longValue());
            }
            
        } catch (Exception e) {
            logger.error("Error generating incident statistics: {}", e.getMessage(), e);
            // Return empty stats on error
        }
        
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> getIncidentByExternalId(String externalId) {
        return incidentRepository.findByExternalId(externalId);
    }

    @Override
    public Optional<Incident> updateIncidentStatus(String externalId, IncidentStatus status) {
        logger.info("Updating incident {} status to {}", externalId, status);
        
        Optional<Incident> incidentOpt = incidentRepository.findByExternalId(externalId);
        if (incidentOpt.isPresent()) {
            Incident incident = incidentOpt.get();
            IncidentStatus previousStatus = incident.getStatus();
            incident.setStatus(status);
            
            if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
                incident.setResolvedAt(LocalDateTime.now());
            }
            
            incident = incidentRepository.save(incident);
            
            // Send Slack status update notification
            try {
                logger.info("Sending Slack status update for incident {}: {} -> {}", 
                    externalId, previousStatus, status);
                    
                if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
                    // Handle incident resolution - archive the channel
                    String statusMessage = String.format("✅ *INCIDENT RESOLVED*\nIncident %s has been resolved and marked as %s.", 
                        incident.getExternalId(), status.name());
                    
                    // Try to get channel ID from incident metadata or find it
                    String channelId = getSlackChannelId(incident);
                    if (channelId != null) {
                        slackIntegrationService.updateIncidentStatus(channelId, incident, statusMessage);
                        slackIntegrationService.archiveIncidentChannel(channelId, incident);
                        logger.info("Slack resolution notification and archiving completed for incident {}", externalId);
                    } else {
                        logger.warn("Could not find Slack channel ID for incident {} - skipping Slack notification", externalId);
                    }
                } else {
                    // Handle general status update
                    String statusMessage = String.format("Status updated from %s to %s", 
                        previousStatus.name(), status.name());
                    
                    String channelId = getSlackChannelId(incident);
                    if (channelId != null) {
                        slackIntegrationService.updateIncidentStatus(channelId, incident, statusMessage);
                        logger.info("Slack status update sent for incident {}", externalId);
                    } else {
                        logger.warn("Could not find Slack channel ID for incident {} - skipping Slack notification", externalId);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to send Slack status update for incident {}: {}", 
                    externalId, e.getMessage());
                // Don't fail the status update if Slack integration fails
            }
            
            return Optional.of(incident);
        }
        
        return Optional.empty();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SimilarityMatch> findSimilarIncidents(String externalId, int maxResults) {
        logger.info("Finding similar incidents for: {}", externalId);
        
        Optional<Incident> incidentOpt = incidentRepository.findByExternalId(externalId);
        if (incidentOpt.isPresent()) {
            Incident incident = incidentOpt.get();
            List<SimilarityMatch> matches = vectorSearchService.findSimilarIncidentsVector(incident, maxResults);
            logger.info("Found {} similar incidents for {}", matches.size(), externalId);
            return matches;
        }
        
        logger.warn("Incident not found: {}", externalId);
        return List.of(); // Return empty list if incident not found
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SimilarityMatch> searchKnowledgeBase(String description, int maxResults) {
        logger.info("Searching knowledge base for description: {}", 
            description.length() > 50 ? description.substring(0, 50) + "..." : description);
        
        List<SimilarityMatch> matches = vectorSearchService.searchWithEmbeddings(description, maxResults);
        logger.info("Found {} knowledge base matches", matches.size());
        return matches;
    }
    
    /**
     * Build solution directly from knowledge base for high-confidence matches
     */
    private String buildSolutionFromKnowledgeBase(SimilarityMatch bestMatch, List<SimilarityMatch> allMatches) {
        StringBuilder solution = new StringBuilder();
        
        solution.append("## 🎯 Knowledge Base Solution\n");
        solution.append(String.format("**Match Found:** %s (Similarity: %s)\n\n", 
            bestMatch.getKnowledgeEntry().getTitle(), bestMatch.getFormattedSimilarity()));
        
        if (bestMatch.getKnowledgeEntry().getSymptoms() != null) {
            solution.append("### 🔍 Symptoms\n");
            solution.append(bestMatch.getKnowledgeEntry().getSymptoms()).append("\n\n");
        }
        
        if (bestMatch.getKnowledgeEntry().getRootCause() != null) {
            solution.append("### 🎯 Root Cause\n");
            solution.append(bestMatch.getKnowledgeEntry().getRootCause()).append("\n\n");
        }
        
        if (bestMatch.getKnowledgeEntry().getSolution() != null) {
            solution.append("### ✅ Recommended Solution\n");
            solution.append(bestMatch.getKnowledgeEntry().getSolution()).append("\n\n");
        }
        
        if (bestMatch.getKnowledgeEntry().getResolutionTimeMinutes() != null) {
            solution.append("### ⏱️ Expected Resolution Time\n");
            solution.append(bestMatch.getKnowledgeEntry().getResolutionTimeMinutes()).append(" minutes\n\n");
        }
        
        // Add additional similar matches if available
        if (allMatches.size() > 1) {
            solution.append("### 📋 Additional Similar Cases\n");
            for (int i = 1; i < Math.min(allMatches.size(), 3); i++) {
                SimilarityMatch match = allMatches.get(i);
                solution.append(String.format("- **%s** (Similarity: %s)\n", 
                    match.getKnowledgeEntry().getTitle(), match.getFormattedSimilarity()));
            }
            solution.append("\n");
        }
        
        solution.append("*This solution was automatically matched from the knowledge base based on incident similarity.*");
        
        return solution.toString();
    }

    /**
     * Enhance AI-generated suggestion with knowledge base recommendations
     */
    private String enhanceAiSuggestionWithKnowledge(String aiSuggestion, List<SimilarityMatch> similarIncidents) {
        if (similarIncidents.isEmpty()) {
            return aiSuggestion;
        }
        
        StringBuilder enhanced = new StringBuilder();
        enhanced.append("## AI Analysis\n");
        enhanced.append(aiSuggestion != null ? aiSuggestion : "No specific AI suggestion available.");
        enhanced.append("\n\n");
        
        enhanced.append("## Similar Incident Solutions\n");
        enhanced.append("Based on historical data, here are similar incidents and their solutions:\n\n");
        
        for (int i = 0; i < similarIncidents.size(); i++) {
            SimilarityMatch match = similarIncidents.get(i);
            enhanced.append(String.format("### %d. %s (Similarity: %s)\n", 
                i + 1, match.getKnowledgeEntry().getTitle(), match.getFormattedSimilarity()));
            
            if (match.getKnowledgeEntry().getRootCause() != null) {
                enhanced.append("**Root Cause:** ").append(match.getKnowledgeEntry().getRootCause()).append("\n\n");
            }
            
            if (match.getKnowledgeEntry().getSolution() != null) {
                enhanced.append("**Solution:**\n").append(match.getKnowledgeEntry().getSolution()).append("\n");
            }
            
            if (match.getKnowledgeEntry().getResolutionTimeMinutes() != null) {
                enhanced.append("**Expected Resolution Time:** ")
                    .append(match.getKnowledgeEntry().getResolutionTimeMinutes())
                    .append(" minutes\n");
            }
            
            if (match.getKnowledgeEntry().getSuccessRate() != null) {
                enhanced.append("**Historical Success Rate:** ")
                    .append(String.format("%.1f%%", match.getKnowledgeEntry().getSuccessRate() * 100))
                    .append("\n");
            }
            
            enhanced.append("\n---\n");
        }
        
        enhanced.append("\n*Note: Solutions are ranked by similarity to current incident. Always verify applicability to your specific environment.*");
        
        return enhanced.toString();
    }

    private IncidentResponse createErrorResponse(String externalId, String message) {
        IncidentResponse response = new IncidentResponse();
        response.setExternalId(externalId);
        response.setStatus(IncidentStatus.FAILED);
        response.setMessage(message);
        return response;
    }

    /**
     * Attempts to retrieve Slack channel ID for an incident.
     * This could be stored in metadata or derived from the incident ID.
     */
    private String getSlackChannelId(Incident incident) {
        // First try to get from metadata if it was stored there
        if (incident.getMetadata() != null && incident.getMetadata().containsKey("slackChannelId")) {
            Object channelId = incident.getMetadata().get("slackChannelId");
            return channelId != null ? channelId.toString() : null;
        }
        
        // If not in metadata, we could try to reconstruct the channel name
        // based on the pattern used in SlackClient.generateChannelName()
        // However, this is not ideal since we don't have the actual channel ID
        logger.debug("No Slack channel ID found in metadata for incident {}", incident.getExternalId());
        return null;
    }

    @Override
    public Optional<Incident> getIncidentById(Long id) {
        logger.debug("Retrieving incident by ID: {}", id);
        return incidentRepository.findById(id);
    }

    @Override
    public Incident updateIncident(Incident incident) {
        logger.info("Updating incident: {}", incident.getId());
        return incidentRepository.save(incident);
    }
}
