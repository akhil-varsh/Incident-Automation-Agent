package com.xlbiz.incident.agent.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

/**
 * Service interface for incident processing and management.
 * Defines the core business operations for incident automation workflows.
 */
public interface IncidentService {

    /**
     * Process a new incident from an external monitoring system.
     * This is the main entry point that triggers the entire incident workflow.
     *
     * @param request The incident request from external system
     * @return Response with processing status and incident details
     */
    IncidentResponse processIncident(IncidentRequest request);

    /**
     * Get detailed status information for a specific incident.
     *
     * @param externalId The external incident ID
     * @return Detailed incident status or empty if not found
     */
    Optional<IncidentStatusResponse> getIncidentStatus(String externalId);

    /**
     * List incidents with filtering and pagination support.
     *
     * @param type Optional filter by incident type
     * @param severity Optional filter by severity level
     * @param status Optional filter by status
     * @param source Optional filter by source system
     * @param startDate Optional filter for incidents after this date
     * @param endDate Optional filter for incidents before this date
     * @param pageable Pagination parameters
     * @return Page of incident summaries
     */
    Page<IncidentSummary> listIncidents(
            IncidentType type,
            IncidentSeverity severity,
            IncidentStatus status,
            String source,
            // LocalDateTime startDate,
            // LocalDateTime endDate,
            Pageable pageable);
    

    /**
     * Get aggregated incident statistics for dashboard metrics.
     *
     * @return Comprehensive incident statistics
     */
    IncidentStats getIncidentStatistics();

    /**
     * Get an incident by external ID.
     *
     * @param externalId The external incident ID
     * @return Incident entity or empty if not found
     */
    Optional<Incident> getIncidentByExternalId(String externalId);

    /**
     * Update incident status.
     *
     * @param externalId The external incident ID
     * @param status New status
     * @return Updated incident or empty if not found
     */
    Optional<Incident> updateIncidentStatus(String externalId, IncidentStatus status);
    
    /**
     * Find similar incidents using knowledge base vector search.
     *
     * @param externalId The external incident ID to find similarities for
     * @param maxResults Maximum number of results to return
     * @return List of similar incidents with similarity scores
     */
    List<SimilarityMatch> findSimilarIncidents(String externalId, int maxResults);
    
    /**
     * Search knowledge base by description text.
     *
     * @param description The incident description to search for
     * @param maxResults Maximum number of results to return
     * @return List of similar knowledge entries with similarity scores
     */
    List<SimilarityMatch> searchKnowledgeBase(String description, int maxResults);

    /**
     * Get an incident by ID.
     *
     * @param id The incident ID
     * @return Incident entity or empty if not found
     */
    Optional<Incident> getIncidentById(Long id);

    /**
     * Update an existing incident.
     *
     * @param incident The incident to update
     * @return Updated incident
     */
    Incident updateIncident(Incident incident);
}
