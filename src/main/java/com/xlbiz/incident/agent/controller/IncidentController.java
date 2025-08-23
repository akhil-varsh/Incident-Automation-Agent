package com.xlbiz.incident.agent.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xlbiz.incident.agent.dto.IncidentRequest;
import com.xlbiz.incident.agent.dto.IncidentResponse;
import com.xlbiz.incident.agent.dto.IncidentStats;
import com.xlbiz.incident.agent.dto.IncidentStatusResponse;
import com.xlbiz.incident.agent.dto.IncidentSummary;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentStatus;
import com.xlbiz.incident.agent.model.IncidentType;
import com.xlbiz.incident.agent.service.IncidentService;

import jakarta.validation.Valid;

/**
 * REST Controller for incident management endpoints.
 * Provides the main API for external monitoring systems to submit incidents
 * and query incident status and statistics.
 */
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private static final Logger logger = LoggerFactory.getLogger(IncidentController.class);
    
    private final IncidentService incidentService;

    @Autowired
    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /**
     * Endpoint to create a new incident (for POST /api/v1/incidents)
     * Accepts incident data and initiates the automated processing workflow.
     *
     * @param request Valid incident request payload
     * @return 201 Created with processing status, or 400 for validation errors
     */
    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody IncidentRequest request) {
        logger.info("Received incident create request for ID: {}", request.getId());
        try {
            IncidentResponse response = incidentService.processIncident(request);
            if (response.getStatus() != IncidentStatus.FAILED) {
                logger.info("Incident {} created and accepted for processing", request.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.warn("Incident {} creation failed: {}", request.getId(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Unexpected error creating incident {}: {}", request.getId(), e.getMessage(), e);
            IncidentResponse errorResponse = new IncidentResponse();
            errorResponse.setExternalId(request.getId());
            errorResponse.setStatus(IncidentStatus.FAILED);
            errorResponse.setMessage("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get detailed status information for a specific incident.
     *
     * @param externalId External incident ID from the source system
     * @return 200 with incident details, or 404 if not found
     */
    @GetMapping("/{externalId}/status")
    public ResponseEntity<IncidentStatusResponse> getIncidentStatus(@PathVariable String externalId) {
        logger.debug("Getting status for incident: {}", externalId);
        
        Optional<IncidentStatusResponse> status = incidentService.getIncidentStatus(externalId);
        
        if (status.isPresent()) {
            return ResponseEntity.ok(status.get());
        } else {
            logger.warn("Incident not found: {}", externalId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List incidents with optional filtering and pagination.
     * Supports filtering by type, severity, status, source, and date range.
     *
     * @param type Optional incident type filter
     * @param severity Optional severity level filter
     * @param status Optional status filter
     * @param source Optional source system filter
     * @param startDate Optional start date filter (ISO format)
     * @param endDate Optional end date filter (ISO format)
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @param sortBy Sort field (default: createdAt)
     * @param sortDir Sort direction (asc/desc, default: desc)
     * @return 200 with paginated incident list
     */
    @GetMapping
    public ResponseEntity<Page<IncidentSummary>> listIncidents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            // @RequestParam(required = false)
            // @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String startDate,
            // @RequestParam(required = false)
            // @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        logger.debug("Listing incidents with filters - type: {}, severity: {}, status: {}, source: {}", 
                    type, severity, status, source);
        try {
            // Validate and limit page size
            if (size > 100) {
                size = 100;
            }
            if (size < 1) {
                size = 20;
            }

            // Parse enum filters
            IncidentType incidentType = parseIncidentType(type);
            IncidentSeverity incidentSeverity = parseIncidentSeverity(severity);
            IncidentStatus incidentStatus = parseIncidentStatus(status);

            // Create sort direction
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, sortBy);

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<IncidentSummary> incidents = incidentService.listIncidents(
                incidentType, incidentSeverity, incidentStatus, source, pageable
            );

            return ResponseEntity.ok(incidents);

        } catch (Exception e) {
            logger.error("Error listing incidents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get aggregated incident statistics for dashboard metrics.
     * Includes counts by status, severity, type, source, and trend data.
     *
     * @return 200 with comprehensive incident statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<IncidentStats> getIncidentStatistics() {
        logger.debug("Generating incident statistics");
        
        try {
            IncidentStats stats = incidentService.getIncidentStatistics();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error generating incident statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods for parsing enum parameters

    private IncidentType parseIncidentType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        
        try {
            return IncidentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid incident type filter: {}", type);
            return null;
        }
    }

    private IncidentSeverity parseIncidentSeverity(String severity) {
        if (severity == null || severity.trim().isEmpty()) {
            return null;
        }
        
        try {
            return IncidentSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid incident severity filter: {}", severity);
            return null;
        }
    }

    private IncidentStatus parseIncidentStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        
        try {
            return IncidentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid incident status filter: {}", status);
            return null;
        }
    }
}
