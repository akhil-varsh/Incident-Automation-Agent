package com.xlbiz.incident.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xlbiz.incident.agent.config.ChromaDBConfig;
import com.xlbiz.incident.agent.dto.SimilarityMatch;
import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.KnowledgeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Knowledge Base Service with Ollama embedding integration
 */
@Service
public class EnhancedKnowledgeBaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedKnowledgeBaseService.class);
    
    private final RestTemplate restTemplate;
    private final ChromaDBConfig config;
    private final ObjectMapper objectMapper;
    private final OllamaEmbeddingService embeddingService;
    private String collectionId;
    
    @Autowired
    public EnhancedKnowledgeBaseService(@Qualifier("chromaDbRestTemplate") RestTemplate restTemplate,
                                      ChromaDBConfig config,
                                      ObjectMapper objectMapper,
                                      OllamaEmbeddingService embeddingService) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Initialize ChromaDB collection with embedding function
     */
    public void initializeCollection() {
        try {
            // Check if Ollama is available first
            if (!embeddingService.isAvailable()) {
                logger.warn("Ollama embedding service not available. Collection will use default embeddings.");
            }
            
            // Get or create collection
            String listUrl = config.getChromaDbUrl() + "/api/v1/collections";
            ResponseEntity<String> response = restTemplate.getForEntity(listUrl, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode collections = objectMapper.readTree(response.getBody());
                for (JsonNode collection : collections) {
                    if (config.getCollectionName().equals(collection.get("name").asText())) {
                        this.collectionId = collection.get("id").asText();
                        logger.info("Found existing ChromaDB collection '{}' with ID: {}", 
                                   config.getCollectionName(), this.collectionId);
                        return;
                    }
                }
            }
            
            // Create new collection with embedding function
            createCollectionWithEmbeddings();
            
        } catch (Exception e) {
            logger.error("Error initializing enhanced ChromaDB collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize enhanced knowledge base collection", e);
        }
    }
    
    private void createCollectionWithEmbeddings() {
        try {
            String createUrl = config.getChromaDbUrl() + "/api/v1/collections";
            
            Map<String, Object> createRequest = Map.of(
                "name", config.getCollectionName(),
                "metadata", Map.of(
                    "description", "XLBiz Incident Knowledge Base with Ollama Embeddings",
                    "embedding_model", "nomic-embed-text:v1.5",
                    "embedding_service", "ollama"
                )
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(createRequest, headers);
            
            ResponseEntity<String> createResponse = restTemplate.postForEntity(createUrl, entity, String.class);
            
            if (createResponse.getStatusCode().is2xxSuccessful()) {
                JsonNode createdCollection = objectMapper.readTree(createResponse.getBody());
                this.collectionId = createdCollection.get("id").asText();
                logger.info("Successfully created enhanced ChromaDB collection '{}' with ID: {}", 
                           config.getCollectionName(), this.collectionId);
                
                // Initialize with sample knowledge entries
                initializeSampleKnowledge();
                
            } else {
                logger.error("Failed to create enhanced collection. Status: {}, Response: {}", 
                           createResponse.getStatusCode(), createResponse.getBody());
            }
            
        } catch (Exception e) {
            logger.error("Error creating collection with embeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create enhanced collection", e);
        }
    }
    
    /**
     * Add knowledge entry with Ollama-generated embeddings
     */
    public void addKnowledgeEntryWithEmbeddings(KnowledgeEntry entry) {
        try {
            if (this.collectionId == null) {
                logger.error("Collection ID not initialized. Cannot add knowledge entry.");
                throw new RuntimeException("Collection ID not initialized");
            }
            
            String embeddingText = entry.getEmbeddingText();
            String entryId = entry.getId() != null ? entry.getId() : UUID.randomUUID().toString();
            
            // Generate embedding using Ollama
            List<Double> embedding = null;
            if (embeddingService.isAvailable()) {
                try {
                    embedding = embeddingService.generateEmbedding(embeddingText);
                    logger.debug("Generated embedding for entry '{}' with {} dimensions", 
                               entry.getTitle(), embedding.size());
                } catch (Exception e) {
                    logger.warn("Failed to generate embedding for entry '{}': {}", entry.getTitle(), e.getMessage());
                }
            }
            
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/add";
            
            Map<String, Object> request = new HashMap<>();
            request.put("documents", List.of(embeddingText));
            request.put("metadatas", List.of(convertToMetadata(entry)));
            request.put("ids", List.of(entryId));
            
            // Add embeddings if available
            if (embedding != null) {
                request.put("embeddings", List.of(embedding));
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully added knowledge entry with embeddings: {}", entry.getTitle());
            } else {
                logger.error("Failed to add knowledge entry with embeddings. Status: {}, Response: {}", 
                           response.getStatusCode(), response.getBody());
            }
            
        } catch (Exception e) {
            logger.error("Error adding knowledge entry '{}' with embeddings: {}", entry.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Failed to add knowledge entry with embeddings", e);
        }
    }
    
    /**
     * Perform similarity search with embeddings
     */
    public List<SimilarityMatch> findSimilarIncidentsWithEmbeddings(Incident incident, int maxResults) {
        try {
            String searchText = buildSearchQuery(incident);
            return performEmbeddingBasedSearch(searchText, maxResults);
            
        } catch (Exception e) {
            logger.error("Error in embedding-based similarity search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Search by description with embeddings
     */
    public List<SimilarityMatch> searchByDescriptionWithEmbeddings(String description, int maxResults) {
        try {
            return performEmbeddingBasedSearch(description, maxResults);
        } catch (Exception e) {
            logger.error("Error in embedding-based description search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    private List<SimilarityMatch> performEmbeddingBasedSearch(String queryText, int maxResults) {
        try {
            if (this.collectionId == null) {
                logger.error("Collection ID not initialized. Cannot perform embedding search.");
                return Collections.emptyList();
            }
            
            // Generate query embedding
            List<Double> queryEmbedding = null;
            if (embeddingService.isAvailable()) {
                try {
                    queryEmbedding = embeddingService.generateEmbedding(queryText);
                    logger.debug("Generated query embedding with {} dimensions", queryEmbedding.size());
                } catch (Exception e) {
                    logger.warn("Failed to generate query embedding: {}", e.getMessage());
                }
            }
            
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/query";
            
            Map<String, Object> request = new HashMap<>();
            request.put("n_results", Math.min(maxResults, config.getMaxResults()));
            request.put("include", List.of("metadatas", "documents", "distances"));
            
            // Use embedding if available, otherwise use text query
            if (queryEmbedding != null) {
                request.put("query_embeddings", List.of(queryEmbedding));
                logger.debug("Using embedding-based search");
            } else {
                request.put("query_texts", List.of(queryText));
                logger.debug("Falling back to text-based search");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    List<SimilarityMatch> matches = parseSimilarityResults(response.getBody());
                    logger.info("Found {} similar incidents using embedding search", matches.size());
                    return matches;
                } else {
                    logger.error("Failed to perform embedding search. Status: {}, Response: {}", 
                               response.getStatusCode(), response.getBody());
                    return Collections.emptyList();
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                if (e.getStatusCode().value() == 422) {
                    logger.warn("ChromaDB collection not ready for embedding search, falling back to text search");
                    return performTextBasedFallback(queryText, maxResults);
                } else {
                    logger.error("HTTP error during embedding search: {}", e.getMessage());
                    return Collections.emptyList();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error performing embedding-based search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Initialize collection with sample knowledge entries
     */
    private void initializeSampleKnowledge() {
        try {
            logger.info("Initializing sample knowledge entries...");
            
            List<KnowledgeEntry> sampleEntries = createSampleKnowledgeEntries();
            
            for (KnowledgeEntry entry : sampleEntries) {
                try {
                    addKnowledgeEntryWithEmbeddings(entry);
                    Thread.sleep(100); // Small delay to avoid overwhelming Ollama
                } catch (Exception e) {
                    logger.warn("Failed to add sample entry '{}': {}", entry.getTitle(), e.getMessage());
                }
            }
            
            logger.info("Successfully initialized {} sample knowledge entries", sampleEntries.size());
            
        } catch (Exception e) {
            logger.error("Error initializing sample knowledge: {}", e.getMessage(), e);
        }
    }
    
    private List<KnowledgeEntry> createSampleKnowledgeEntries() {
        List<KnowledgeEntry> entries = new ArrayList<>();
        
        // Database-related incidents
        entries.add(createKnowledgeEntry(
            "database-connection-timeout",
            "Database Connection Timeout",
            "DATABASE_CONNECTION_ERROR",
            "HIGH",
            "Connection timeouts, slow queries, application hanging",
            "Database server overloaded or network connectivity issues",
            "1. Check database server CPU/memory usage 2. Restart connection pool 3. Optimize slow queries 4. Scale database if needed",
            List.of("production", "staging"),
            List.of("PostgreSQL", "MySQL", "MongoDB"),
            List.of("database", "timeout", "connection", "performance")
        ));
        
        entries.add(createKnowledgeEntry(
            "database-disk-full",
            "Database Disk Space Full",
            "DATABASE_CONNECTION_ERROR",
            "CRITICAL",
            "Database writes failing, application errors, disk space alerts",
            "Database disk partition reached 100% capacity",
            "1. Clear old logs and temporary files 2. Archive old data 3. Extend disk space 4. Set up disk monitoring",
            List.of("production"),
            List.of("PostgreSQL", "MySQL"),
            List.of("database", "disk", "storage", "critical")
        ));
        
        // Authentication issues
        entries.add(createKnowledgeEntry(
            "auth-service-down",
            "Authentication Service Unavailable",
            "AUTHENTICATION_ERROR",
            "HIGH",
            "Users cannot login, authentication failures, 503 errors",
            "Authentication service crashed or became unresponsive",
            "1. Restart authentication service 2. Check service logs 3. Verify database connectivity 4. Scale service if needed",
            List.of("production", "staging"),
            List.of("OAuth", "JWT", "LDAP"),
            List.of("authentication", "login", "service", "unavailable")
        ));
        
        // Network issues
        entries.add(createKnowledgeEntry(
            "network-connectivity-loss",
            "Network Connectivity Issues",
            "NETWORK_ERROR",
            "MEDIUM",
            "Intermittent connection failures, high latency, packet loss",
            "Network infrastructure problems or ISP issues",
            "1. Check network status with ISP 2. Restart network equipment 3. Switch to backup connection 4. Monitor network metrics",
            List.of("production", "staging", "development"),
            List.of("AWS", "Azure", "GCP"),
            List.of("network", "connectivity", "latency", "infrastructure")
        ));
        
        // Performance issues
        entries.add(createKnowledgeEntry(
            "high-memory-usage",
            "High Memory Usage Alert",
            "PERFORMANCE_ISSUE",
            "MEDIUM",
            "Application slow response, memory alerts, potential OOM errors",
            "Memory leak or increased load causing high memory consumption",
            "1. Identify memory-consuming processes 2. Restart affected services 3. Check for memory leaks 4. Scale resources if needed",
            List.of("production", "staging"),
            List.of("Java", "Node.js", "Python"),
            List.of("memory", "performance", "leak", "resources")
        ));
        
        return entries;
    }
    
    private KnowledgeEntry createKnowledgeEntry(String id, String title, String patternType, 
                                              String severity, String symptoms, String rootCause, 
                                              String solution, List<String> environments, 
                                              List<String> technologies, List<String> tags) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setId(id);
        entry.setTitle(title);
        entry.setPatternType(patternType);
        entry.setSeverity(severity);
        entry.setSymptoms(symptoms);
        entry.setRootCause(rootCause);
        entry.setSolution(solution);
        entry.setEnvironments(environments);
        entry.setTechnologies(technologies);
        entry.setTags(tags);
        entry.setConfidenceScore(0.9);
        entry.setUsageCount(0);
        entry.setResolutionTimeMinutes(30);
        return entry;
    }
    
    // Helper methods (reuse from original service)
    private String buildSearchQuery(Incident incident) {
        StringBuilder query = new StringBuilder();
        
        if (incident.getDescription() != null) {
            query.append("Description: ").append(incident.getDescription()).append(" ");
        }
        
        if (incident.getType() != null) {
            query.append("Type: ").append(incident.getType().name()).append(" ");
        }
        
        if (incident.getSeverity() != null) {
            query.append("Severity: ").append(incident.getSeverity().name()).append(" ");
        }
        
        if (incident.getSource() != null) {
            query.append("Source: ").append(incident.getSource()).append(" ");
        }
        
        return query.toString().trim();
    }
    
    private Map<String, Object> convertToMetadata(KnowledgeEntry entry) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("id", entry.getId());
        metadata.put("title", entry.getTitle());
        metadata.put("pattern_type", entry.getPatternType());
        metadata.put("severity", entry.getSeverity());
        metadata.put("confidence_score", entry.getConfidenceScore());
        metadata.put("usage_count", entry.getUsageCount());
        metadata.put("resolution_time_minutes", entry.getResolutionTimeMinutes());
        
        try {
            if (entry.getEnvironments() != null) {
                metadata.put("environments", objectMapper.writeValueAsString(entry.getEnvironments()));
            }
            if (entry.getTechnologies() != null) {
                metadata.put("technologies", objectMapper.writeValueAsString(entry.getTechnologies()));
            }
            if (entry.getTags() != null) {
                metadata.put("tags", objectMapper.writeValueAsString(entry.getTags()));
            }
        } catch (Exception e) {
            logger.warn("Error serializing metadata for entry '{}': {}", entry.getTitle(), e.getMessage());
        }
        
        return metadata;
    }
    
    private List<SimilarityMatch> parseSimilarityResults(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode ids = root.get("ids").get(0);
            JsonNode documents = root.get("documents").get(0);
            JsonNode metadatas = root.get("metadatas").get(0);
            JsonNode distances = root.get("distances").get(0);
            
            List<SimilarityMatch> matches = new ArrayList<>();
            
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i).asText();
                String document = documents.get(i).asText();
                JsonNode metadata = metadatas.get(i);
                double distance = distances.get(i).asDouble();
                
                double similarity = Math.max(0, 1 - distance);
                
                if (similarity >= config.getSimilarityThreshold()) {
                    KnowledgeEntry entry = parseKnowledgeEntry(id, document, metadata);
                    SimilarityMatch match = new SimilarityMatch(entry, similarity, distance);
                    match.setRank(i + 1);
                    matches.add(match);
                }
            }
            
            return matches;
            
        } catch (Exception e) {
            logger.error("Error parsing similarity results: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    private KnowledgeEntry parseKnowledgeEntry(String id, String document, JsonNode metadata) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setId(id);
        
        try {
            if (metadata.has("title") && !metadata.get("title").isNull()) {
                entry.setTitle(metadata.get("title").asText());
            }
            if (metadata.has("pattern_type") && !metadata.get("pattern_type").isNull()) {
                entry.setPatternType(metadata.get("pattern_type").asText());
            }
            if (metadata.has("severity") && !metadata.get("severity").isNull()) {
                entry.setSeverity(metadata.get("severity").asText());
            }
            if (metadata.has("confidence_score") && !metadata.get("confidence_score").isNull()) {
                entry.setConfidenceScore(metadata.get("confidence_score").asDouble());
            }
            
            parseDocumentText(entry, document);
            
        } catch (Exception e) {
            logger.warn("Error parsing knowledge entry metadata for ID '{}': {}", id, e.getMessage());
        }
        
        return entry;
    }
    
    private void parseDocumentText(KnowledgeEntry entry, String document) {
        String[] parts = document.split(" Root Cause: | Solution: ");
        
        if (parts.length >= 1 && parts[0].startsWith("Symptoms: ")) {
            entry.setSymptoms(parts[0].substring("Symptoms: ".length()));
        }
        if (parts.length >= 2) {
            entry.setRootCause(parts[1]);
        }
        if (parts.length >= 3) {
            entry.setSolution(parts[2]);
        }
    }
    
    /**
     * Fallback to text-based search when embeddings are not available
     */
    private List<SimilarityMatch> performTextBasedFallback(String queryText, int maxResults) {
        try {
            logger.info("Using text-based fallback search for query: {}", queryText);
            
            if (this.collectionId == null) {
                logger.error("Collection ID not initialized for fallback search");
                return Collections.emptyList();
            }
            
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/get";
            
            Map<String, Object> requestBody = new HashMap<>();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<KnowledgeEntry> allEntries = parseKnowledgeEntries(response.getBody());
                
                List<SimilarityMatch> matches = new ArrayList<>();
                String lowerQuery = queryText.toLowerCase();
                
                for (KnowledgeEntry entry : allEntries) {
                    double similarity = calculateTextSimilarity(lowerQuery, entry);
                    
                    if (similarity >= config.getSimilarityThreshold()) {
                        SimilarityMatch match = new SimilarityMatch(entry, similarity);
                        matches.add(match);
                    }
                }
                
                matches.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
                
                return matches.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());
            } else {
                logger.error("Failed to get entries for fallback search. Status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            logger.error("Error in text-based fallback search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Simple text similarity calculation
     */
    private double calculateTextSimilarity(String query, KnowledgeEntry entry) {
        String entryText = (entry.getTitle() + " " + 
                          entry.getSymptoms() + " " + 
                          entry.getRootCause() + " " + 
                          entry.getSolution()).toLowerCase();
        
        String[] queryWords = query.split("\\s+");
        int matches = 0;
        int totalWords = queryWords.length;
        
        for (String word : queryWords) {
            if (word.length() > 2 && entryText.contains(word)) {
                matches++;
            }
        }
        
        double similarity = totalWords > 0 ? (double) matches / totalWords : 0.0;
        
        if (entryText.contains(query)) {
            similarity += 0.3;
        }
        
        if (entry.getPatternType() != null && 
            query.contains(entry.getPatternType().toLowerCase().replace("_", " "))) {
            similarity += 0.2;
        }
        
        return Math.min(similarity, 1.0);
    }
    
    private List<KnowledgeEntry> parseKnowledgeEntries(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            JsonNode ids = root.get("ids");
            JsonNode documents = root.get("documents");
            JsonNode metadatas = root.get("metadatas");
            
            List<KnowledgeEntry> entries = new ArrayList<>();
            
            if (ids != null && ids.isArray() && documents != null && documents.isArray() && 
                metadatas != null && metadatas.isArray()) {
                
                int size = ids.size();
                
                for (int i = 0; i < size; i++) {
                    try {
                        String id = ids.get(i).asText();
                        String document = documents.get(i).asText();
                        JsonNode metadata = metadatas.get(i);
                        
                        KnowledgeEntry entry = parseKnowledgeEntry(id, document, metadata);
                        entries.add(entry);
                        
                    } catch (Exception e) {
                        logger.warn("Error parsing knowledge entry at index {}: {}", i, e.getMessage());
                    }
                }
            }
            
            return entries;
            
        } catch (Exception e) {
            logger.error("Error parsing knowledge entries response: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get service health information
     */
    public Map<String, Object> getServiceHealth() {
        return Map.of(
            "service", "enhanced-knowledge-base",
            "collection_id", collectionId != null ? collectionId : "not_initialized",
            "ollama_embedding", embeddingService.getModelInfo(),
            "chromadb_url", config.getChromaDbUrl(),
            "status", collectionId != null ? "UP" : "INITIALIZING"
        );
    }
}