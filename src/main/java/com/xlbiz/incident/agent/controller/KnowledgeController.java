package com.xlbiz.incident.agent.controller;

import com.xlbiz.incident.agent.dto.SimilarityMatch;
import com.xlbiz.incident.agent.model.KnowledgeEntry;
import com.xlbiz.incident.agent.service.IncidentService;
import com.xlbiz.incident.agent.service.KnowledgeBaseService;
import com.xlbiz.incident.agent.service.EnhancedKnowledgeBaseService;
import com.xlbiz.incident.agent.service.VectorSearchKnowledgeBaseService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for knowledge base operations and similarity search.
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeController.class);
    
    private final KnowledgeBaseService knowledgeBaseService;
    private final EnhancedKnowledgeBaseService enhancedKnowledgeBaseService;
    private final VectorSearchKnowledgeBaseService vectorSearchService;
    private final IncidentService incidentService;
    
    @Autowired
    public KnowledgeController(KnowledgeBaseService knowledgeBaseService,
                              EnhancedKnowledgeBaseService enhancedKnowledgeBaseService,
                              VectorSearchKnowledgeBaseService vectorSearchService,
                              IncidentService incidentService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.enhancedKnowledgeBaseService = enhancedKnowledgeBaseService;
        this.vectorSearchService = vectorSearchService;
        this.incidentService = incidentService;
    }
    
    /**
     * Search knowledge base by description text
     */
    @GetMapping("/search")
    public ResponseEntity<List<SimilarityMatch>> searchKnowledgeBase(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) Integer limit) {
        
        logger.info("Searching knowledge base for query: {} (limit: {})", 
            query.length() > 50 ? query.substring(0, 50) + "..." : query, limit);
        
        try {
            List<SimilarityMatch> matches = incidentService.searchKnowledgeBase(query, limit);
            
            logger.info("Found {} matches for knowledge base search", matches.size());
            return ResponseEntity.ok(matches);
            
        } catch (Exception e) {
            logger.error("Error searching knowledge base: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Find similar incidents for a specific incident
     */
    @GetMapping("/incidents/{externalId}/similar")
    public ResponseEntity<List<SimilarityMatch>> findSimilarIncidents(
            @PathVariable String externalId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) Integer limit) {
        
        logger.info("Finding similar incidents for: {} (limit: {})", externalId, limit);
        
        try {
            List<SimilarityMatch> matches = incidentService.findSimilarIncidents(externalId, limit);
            
            if (matches.isEmpty()) {
                // Check if the incident exists
                if (incidentService.getIncidentByExternalId(externalId).isEmpty()) {
                    logger.warn("Incident not found: {}", externalId);
                    return ResponseEntity.notFound().build();
                }
                logger.info("No similar incidents found for: {}", externalId);
            } else {
                logger.info("Found {} similar incidents for: {}", matches.size(), externalId);
            }
            
            return ResponseEntity.ok(matches);
            
        } catch (Exception e) {
            logger.error("Error finding similar incidents for {}: {}", externalId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all knowledge entries (for management purposes)
     */
    @GetMapping("/entries")
    public ResponseEntity<List<KnowledgeEntry>> getAllKnowledgeEntries() {
        logger.info("Retrieving all knowledge entries");
        
        try {
            List<KnowledgeEntry> entries = knowledgeBaseService.getAllKnowledgeEntries();
            
            logger.info("Retrieved {} knowledge entries", entries.size());
            return ResponseEntity.ok(entries);
            
        } catch (Exception e) {
            logger.error("Error retrieving knowledge entries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Add a new knowledge entry
     */
    @PostMapping("/entries")
    public ResponseEntity<String> addKnowledgeEntry(@RequestBody KnowledgeEntry entry) {
        logger.info("Adding new knowledge entry: {}", entry.getTitle());
        
        try {
            // Validate required fields
            if (entry.getTitle() == null || entry.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Title is required");
            }
            if (entry.getSymptoms() == null || entry.getSymptoms().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Symptoms are required");
            }
            if (entry.getSolution() == null || entry.getSolution().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Solution is required");
            }
            
            knowledgeBaseService.addKnowledgeEntry(entry);
            
            logger.info("Successfully added knowledge entry: {}", entry.getTitle());
            return ResponseEntity.status(201).body("Knowledge entry added successfully");
            
        } catch (Exception e) {
            logger.error("Error adding knowledge entry '{}': {}", entry.getTitle(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error adding knowledge entry: " + e.getMessage());
        }
    }
    
    /**
     * Search knowledge base using enhanced embeddings
     */
    @GetMapping("/search-enhanced")
    public ResponseEntity<List<SimilarityMatch>> searchKnowledgeBaseEnhanced(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        
        logger.info("Enhanced knowledge base search request: query='{}', maxResults={}", query, maxResults);
        
        try {
            List<SimilarityMatch> matches = enhancedKnowledgeBaseService.searchByDescriptionWithEmbeddings(query, maxResults);
            
            logger.info("Enhanced search found {} matches for query: {}", matches.size(), query);
            
            return ResponseEntity.ok(matches);
            
        } catch (Exception e) {
            logger.error("Error in enhanced knowledge base search: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    /**
     * Get enhanced knowledge base service health
     */
    @GetMapping("/health-enhanced")
    public ResponseEntity<Map<String, Object>> getEnhancedKnowledgeHealth() {
        try {
            Map<String, Object> health = enhancedKnowledgeBaseService.getServiceHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Error getting enhanced knowledge base health: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Initialize enhanced knowledge base (manual trigger)
     */
    @PostMapping("/initialize-enhanced")
    public ResponseEntity<Map<String, Object>> initializeEnhancedKnowledgeBase() {
        try {
            logger.info("Manual initialization of enhanced knowledge base requested");
            
            enhancedKnowledgeBaseService.initializeCollection();
            
            Map<String, Object> result = Map.of(
                "status", "success",
                "message", "Enhanced knowledge base initialized successfully with embeddings",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error initializing enhanced knowledge base: {}", e.getMessage(), e);
            
            Map<String, Object> result = Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * Force re-initialization of enhanced knowledge base with fresh embeddings
     */
    @PostMapping("/reinitialize-enhanced")
    public ResponseEntity<Map<String, Object>> reinitializeEnhancedKnowledgeBase() {
        try {
            logger.info("Force re-initialization of enhanced knowledge base requested");
            
            // This will recreate the collection and add sample data with embeddings
            enhancedKnowledgeBaseService.initializeCollection();
            
            Map<String, Object> result = Map.of(
                "status", "success",
                "message", "Enhanced knowledge base re-initialized with fresh embeddings",
                "timestamp", System.currentTimeMillis(),
                "note", "All sample knowledge entries have been recreated with Ollama embeddings"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error re-initializing enhanced knowledge base: {}", e.getMessage(), e);
            
            Map<String, Object> result = Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    // Vector Search Endpoints
    
    /**
     * Search using vector embeddings (new clean implementation)
     */
    @GetMapping("/search-vector")
    public ResponseEntity<List<SimilarityMatch>> searchKnowledgeBaseVector(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        
        try {
            List<SimilarityMatch> matches = vectorSearchService.searchWithEmbeddings(query, maxResults);
            return ResponseEntity.ok(matches);
            
        } catch (Exception e) {
            logger.error("Vector search failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    /**
     * Get vector search service health
     */
    @GetMapping("/health-vector")
    public ResponseEntity<Map<String, Object>> getVectorKnowledgeHealth() {
        try {
            Map<String, Object> health = vectorSearchService.getVectorServiceHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Error getting vector knowledge base health: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Initialize vector knowledge base
     */
    @PostMapping("/initialize-vector")
    public ResponseEntity<Map<String, Object>> initializeVectorKnowledgeBase() {
        try {
            logger.info("Manual initialization of vector knowledge base requested");
            
            vectorSearchService.initializeVectorCollection();
            
            Map<String, Object> result = Map.of(
                "status", "success",
                "message", "Vector knowledge base initialized successfully with Ollama embeddings",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error initializing vector knowledge base: {}", e.getMessage(), e);
            
            Map<String, Object> result = Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Delete a knowledge entry by ID
     */
    @DeleteMapping("/entries/{id}")
    public ResponseEntity<String> deleteKnowledgeEntry(@PathVariable String id) {
        
        logger.info("Deleting knowledge entry: {}", id);
        
        try {
            boolean deleted = knowledgeBaseService.deleteKnowledgeEntry(id);
            
            if (deleted) {
                logger.info("Successfully deleted knowledge entry: {}", id);
                return ResponseEntity.ok("Knowledge entry deleted successfully");
            } else {
                logger.warn("Knowledge entry not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error deleting knowledge entry '{}': {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error deleting knowledge entry: " + e.getMessage());
        }
    }
}
