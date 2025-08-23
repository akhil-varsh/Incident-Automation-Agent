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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vector-based Knowledge Base Service using Ollama embeddings and ChromaDB
 */
@Service
public class VectorSearchKnowledgeBaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorSearchKnowledgeBaseService.class);
    
    private final RestTemplate restTemplate;
    private final ChromaDBConfig config;
    private final ObjectMapper objectMapper;
    private final OllamaEmbeddingService embeddingService;
    
    private String collectionId;
    private boolean isInitialized = false;
    
    @Autowired
    public VectorSearchKnowledgeBaseService(@Qualifier("chromaDbRestTemplate") RestTemplate restTemplate,
                                          ChromaDBConfig config,
                                          ObjectMapper objectMapper,
                                          OllamaEmbeddingService embeddingService) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Initialize the vector knowledge base
     */
    @PostConstruct
    public void initialize() {
        try {
            logger.info("Initializing Vector Search Knowledge Base...");
            initializeVectorCollection();
        } catch (Exception e) {
            logger.error("Failed to initialize vector knowledge base: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Initialize ChromaDB collection for vector search
     */
    public void initializeVectorCollection() {
        try {
            String collectionName = config.getCollectionName() + "_vector";
            
            // Check if collection exists
            if (findExistingCollection(collectionName)) {
                isInitialized = true;
                return;
            }
            
            // Create new collection
            createVectorCollection(collectionName);
            
            // Add sample knowledge with embeddings
            addSampleKnowledgeWithEmbeddings();
            
            isInitialized = true;
            
        } catch (Exception e) {
            logger.error("Error initializing vector collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize vector collection", e);
        }
    }
    
    /**
     * Search using vector embeddings
     */
    public List<SimilarityMatch> searchWithEmbeddings(String query, int maxResults) {
        try {
            if (!isInitialized) {
                initializeVectorCollection();
            }
            
            if (!embeddingService.isAvailable()) {
                throw new RuntimeException("Ollama embedding service not available");
            }
            
            // Generate query embedding
            List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
            
            // Perform vector search
            return performVectorSearch(queryEmbedding, maxResults);
            
        } catch (Exception e) {
            logger.error("Error in vector search: {}", e.getMessage());
            throw new RuntimeException("Vector search failed", e);
        }
    }
    
    /**
     * Search for similar incidents using vector embeddings
     */
    public List<SimilarityMatch> findSimilarIncidentsVector(Incident incident, int maxResults) {
        String searchQuery = buildIncidentSearchQuery(incident);
        return searchWithEmbeddings(searchQuery, maxResults);
    }
    
    /**
     * Add knowledge entry with vector embedding
     */
    public void addKnowledgeWithEmbedding(KnowledgeEntry entry) {
        try {
            if (!isInitialized) {
                logger.warn("Vector knowledge base not initialized");
                return;
            }
            
            String embeddingText = entry.getEmbeddingText();
            String entryId = entry.getId() != null ? entry.getId() : UUID.randomUUID().toString();
            
            // Generate embedding
            List<Double> embedding = embeddingService.generateEmbedding(embeddingText);
            
            // Add to ChromaDB
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/add";
            
            Map<String, Object> request = Map.of(
                "documents", List.of(embeddingText),
                "metadatas", List.of(convertToMetadata(entry)),
                "ids", List.of(entryId),
                "embeddings", List.of(embedding)
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully added knowledge entry with vector embedding: {}", entry.getTitle());
            } else {
                logger.error("Failed to add knowledge entry. Status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error adding knowledge entry with embedding: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get service health and status
     */
    public Map<String, Object> getVectorServiceHealth() {
        return Map.of(
            "service", "vector-search-knowledge-base",
            "initialized", isInitialized,
            "collection_id", collectionId != null ? collectionId : "not_created",
            "ollama_available", embeddingService.isAvailable(),
            "ollama_model", embeddingService.getModelInfo(),
            "chromadb_url", config.getChromaDbUrl(),
            "status", isInitialized ? "UP" : "INITIALIZING"
        );
    }
    
    // Private helper methods
    
    private boolean findExistingCollection(String collectionName) {
        try {
            String listUrl = config.getChromaDbUrl() + "/api/v1/collections";
            ResponseEntity<String> response = restTemplate.getForEntity(listUrl, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode collections = objectMapper.readTree(response.getBody());
                for (JsonNode collection : collections) {
                    if (collectionName.equals(collection.get("name").asText())) {
                        this.collectionId = collection.get("id").asText();
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking for existing collection: {}", e.getMessage());
            return false;
        }
    }
    
    private void createVectorCollection(String collectionName) {
        try {
            String createUrl = config.getChromaDbUrl() + "/api/v1/collections";
            
            Map<String, Object> createRequest = Map.of(
                "name", collectionName,
                "metadata", Map.of(
                    "description", "XLBiz Vector Knowledge Base with Ollama Embeddings",
                    "embedding_model", "nomic-embed-text:v1.5",
                    "embedding_service", "ollama",
                    "created_at", System.currentTimeMillis()
                )
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(createRequest, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(createUrl, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode createdCollection = objectMapper.readTree(response.getBody());
                this.collectionId = createdCollection.get("id").asText();
            } else {
                throw new RuntimeException("Failed to create vector collection: " + response.getBody());
            }
            
        } catch (Exception e) {
            logger.error("Error creating vector collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create vector collection", e);
        }
    }
    
    private void addSampleKnowledgeWithEmbeddings() {
        try {
            List<KnowledgeEntry> sampleEntries = createSampleKnowledgeEntries();
            
            for (KnowledgeEntry entry : sampleEntries) {
                addKnowledgeWithEmbedding(entry);
                Thread.sleep(100); // Small delay for Ollama
            }
            
            logger.info("Vector knowledge base initialized with {} entries", sampleEntries.size());
            
        } catch (Exception e) {
            logger.error("Error initializing sample knowledge: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize sample knowledge", e);
        }
    }
    
    private List<SimilarityMatch> performVectorSearch(List<Double> queryEmbedding, int maxResults) {
        try {
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/query";
            
            Map<String, Object> request = Map.of(
                "query_embeddings", List.of(queryEmbedding),
                "n_results", Math.min(maxResults, config.getMaxResults()),
                "include", List.of("metadatas", "documents", "distances")
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<SimilarityMatch> matches = parseSimilarityResults(response.getBody());
                return matches;
            } else {
                throw new RuntimeException("Vector search failed: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Vector search error: {}", e.getMessage());
            throw new RuntimeException("Vector search failed", e);
        }
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
                
                // Convert distance to similarity (1 - distance for cosine distance)
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
            
            // Parse document text
            parseDocumentText(entry, document);
            
        } catch (Exception e) {
            logger.warn("Error parsing knowledge entry: {}", e.getMessage());
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
    
    private String buildIncidentSearchQuery(Incident incident) {
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
        
        return metadata;
    }
    
    private List<KnowledgeEntry> createSampleKnowledgeEntries() {
        List<KnowledgeEntry> entries = new ArrayList<>();
        
        // Database issues
        entries.add(createKnowledgeEntry(
            "db-connection-timeout-vector",
            "Database Connection Timeout Issues",
            "DATABASE_CONNECTION_ERROR",
            "HIGH",
            "Application unable to connect to database, connection pool exhausted, timeout errors on database queries, users experiencing login failures and data loading issues",
            "Database connection pool has reached its maximum limit due to long-running queries, connection leaks, or insufficient pool configuration",
            "1. Check connection pool configuration (max connections, timeout settings) 2. Identify and terminate long-running queries 3. Restart database connection pool 4. Monitor for connection leaks 5. Scale database resources if needed",
            List.of("production", "staging"),
            List.of("PostgreSQL", "MySQL", "Connection Pool"),
            List.of("database", "timeout", "connection", "pool")
        ));
        
        // Authentication issues
        entries.add(createKnowledgeEntry(
            "auth-service-failure-vector",
            "Authentication Service Failure",
            "AUTHENTICATION_ERROR",
            "HIGH",
            "Users cannot login to the system, authentication requests failing with 401 errors, login page not responding, session timeouts occurring frequently",
            "Authentication service has crashed, database connectivity issues, or authentication server overloaded due to high traffic",
            "1. Restart authentication service 2. Check authentication database connectivity 3. Verify authentication server resources 4. Check for DDoS attacks 5. Scale authentication service if needed",
            List.of("production"),
            List.of("OAuth", "JWT", "LDAP", "Authentication"),
            List.of("authentication", "login", "401", "session")
        ));
        
        // Performance issues
        entries.add(createKnowledgeEntry(
            "high-memory-usage-vector",
            "High Memory Usage Performance Issue",
            "PERFORMANCE_ISSUE",
            "MEDIUM",
            "Application responding slowly, high memory consumption alerts, potential out-of-memory errors, garbage collection taking too long",
            "Memory leak in application code, insufficient memory allocation, or increased load causing memory pressure",
            "1. Identify memory-consuming processes using profiling tools 2. Restart affected services 3. Check for memory leaks in application code 4. Increase memory allocation 5. Optimize memory usage patterns",
            List.of("production", "staging"),
            List.of("Java", "JVM", "Memory Management"),
            List.of("memory", "performance", "gc", "heap")
        ));
        
        // Network issues
        entries.add(createKnowledgeEntry(
            "network-connectivity-vector",
            "Network Connectivity Problems",
            "NETWORK_ERROR",
            "MEDIUM",
            "Intermittent connection failures between services, high network latency, packet loss, timeouts when calling external APIs",
            "Network infrastructure problems, ISP issues, firewall blocking connections, or DNS resolution failures",
            "1. Check network status with ISP 2. Test connectivity between services 3. Verify firewall rules 4. Check DNS resolution 5. Switch to backup network connection if available",
            List.of("production", "staging"),
            List.of("Network", "DNS", "Firewall"),
            List.of("network", "connectivity", "latency", "dns")
        ));
        
        // Disk space issues
        entries.add(createKnowledgeEntry(
            "disk-space-full-vector",
            "Disk Space Full Critical Issue",
            "STORAGE_ERROR",
            "CRITICAL",
            "Disk space at 100% capacity, applications unable to write files, database writes failing, log files cannot be created",
            "Disk partition has reached maximum capacity due to log file accumulation, database growth, or temporary file buildup",
            "1. Clear old log files and temporary files 2. Archive or delete old data 3. Extend disk partition 4. Move files to different partition 5. Set up disk space monitoring and alerts",
            List.of("production"),
            List.of("Storage", "Filesystem", "Database"),
            List.of("disk", "storage", "space", "full")
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
        entry.setConfidenceScore(0.95);
        entry.setUsageCount(0);
        entry.setResolutionTimeMinutes(30);
        return entry;
    }
}