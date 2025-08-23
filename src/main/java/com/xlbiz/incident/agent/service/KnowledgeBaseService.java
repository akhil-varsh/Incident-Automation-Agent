package com.xlbiz.incident.agent.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xlbiz.incident.agent.config.ChromaDBConfig;
import com.xlbiz.incident.agent.dto.SimilarityMatch;
import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.KnowledgeEntry;

/**
 * Service for managing knowledge base interactions with ChromaDB.
 * Handles vector storage, similarity search, and knowledge entry management.
 */
@Service
public class KnowledgeBaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);
    
    private final RestTemplate restTemplate;
    private final ChromaDBConfig config;
    private final ObjectMapper objectMapper;
    private String collectionId; // Store the actual collection ID from ChromaDB
    
    @Autowired
    public KnowledgeBaseService(@Qualifier("chromaDbRestTemplate") RestTemplate restTemplate,
                                ChromaDBConfig config,
                                ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Initialize the ChromaDB collection if it doesn't exist
     */
    public void initializeCollection() {
        try {
            // Get all collections to find our collection ID
            String listUrl = config.getChromaDbUrl() + "/api/v1/collections";
            
            ResponseEntity<String> response = restTemplate.getForEntity(listUrl, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                // Parse response to find our collection
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
            
            // Collection doesn't exist, create it
            logger.info("Collection doesn't exist, creating new one");
            
            String createUrl = config.getChromaDbUrl() + "/api/v1/collections";
            Map<String, Object> createRequest = Map.of(
                "name", config.getCollectionName(),
                "metadata", Map.of(
                    "description", "XLBiz Incident Knowledge Base",
                    "embedding_model", config.getEmbeddingModel()
                )
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(createRequest, headers);
            
            ResponseEntity<String> createResponse = restTemplate.postForEntity(createUrl, entity, String.class);
            if (createResponse.getStatusCode().is2xxSuccessful()) {
                // Parse the created collection response to get the ID
                JsonNode createdCollection = objectMapper.readTree(createResponse.getBody());
                this.collectionId = createdCollection.get("id").asText();
                logger.info("Successfully created ChromaDB collection '{}' with ID: {}", 
                           config.getCollectionName(), this.collectionId);
            } else {
                logger.error("Failed to create collection. Status: {}, Response: {}", 
                           createResponse.getStatusCode(), createResponse.getBody());
            }
            
        } catch (Exception e) {
            logger.error("Error initializing ChromaDB collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize knowledge base collection", e);
        }
    }
    
    /**
     * Add a knowledge entry to the vector database
     */
    public void addKnowledgeEntry(KnowledgeEntry entry) {
        try {
            if (this.collectionId == null) {
                logger.error("Collection ID not initialized. Cannot add knowledge entry.");
                throw new RuntimeException("Collection ID not initialized");
            }
            
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/add";
            
            // Prepare the request for ChromaDB
            Map<String, Object> request = Map.of(
                "documents", List.of(entry.getEmbeddingText()),
                "metadatas", List.of(convertToMetadata(entry)),
                "ids", List.of(entry.getId() != null ? entry.getId() : UUID.randomUUID().toString())
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully added knowledge entry: {}", entry.getTitle());
            } else {
                logger.error("Failed to add knowledge entry. Status: {}, Response: {}", 
                           response.getStatusCode(), response.getBody());
            }
            
        } catch (Exception e) {
            logger.error("Error adding knowledge entry '{}': {}", entry.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Failed to add knowledge entry to vector database", e);
        }
    }
    
    /**
     * Search for similar incidents and return potential solutions
     */
    public List<SimilarityMatch> findSimilarIncidents(Incident incident, int maxResults) {
        try {
            // Create search query from incident details
            String searchText = buildSearchQuery(incident);
            return performSimilaritySearch(searchText, maxResults);
            
        } catch (Exception e) {
            logger.error("Error searching for similar incidents: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Search for similar incidents by description text
     */
    public List<SimilarityMatch> searchByDescription(String description, int maxResults) {
        try {
            return performSimilaritySearch(description, maxResults);
        } catch (Exception e) {
            logger.error("Error searching by description '{}': {}", description, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all knowledge entries (for management purposes)
     */
    public List<KnowledgeEntry> getAllKnowledgeEntries() {
        try {
            if (this.collectionId == null) {
                logger.error("Collection ID not initialized. Cannot get knowledge entries.");
                return Collections.emptyList();
            }
            
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/get";
            
            // ChromaDB get endpoint - send empty request to get all entries
            Map<String, Object> requestBody = new HashMap<>();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<KnowledgeEntry> entries = parseKnowledgeEntries(response.getBody());
                logger.info("Retrieved {} knowledge entries from ChromaDB", entries.size());
                return entries;
            } else {
                logger.error("Failed to get all knowledge entries. Status: {}, Response: {}", 
                           response.getStatusCode(), response.getBody());
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            logger.error("Error getting all knowledge entries: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Delete a knowledge entry by ID
     */
    public boolean deleteKnowledgeEntry(String entryId) {
        try {
            if (this.collectionId == null) {
                logger.error("Collection ID not initialized. Cannot delete knowledge entry.");
                return false;
            }
            
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/delete";
            
            Map<String, Object> request = Map.of("ids", List.of(entryId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted knowledge entry: {}", entryId);
                return true;
            } else {
                logger.error("Failed to delete knowledge entry. Status: {}", response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error deleting knowledge entry '{}': {}", entryId, e.getMessage(), e);
            return false;
        }
    }
    
    // Private helper methods
    
    private List<SimilarityMatch> performSimilaritySearch(String queryText, int maxResults) {
        try {
            if (this.collectionId == null) {
                logger.error("Collection ID not initialized. Cannot perform similarity search.");
                return Collections.emptyList();
            }
            
            String url = config.getChromaDbUrl() + "/api/v1/collections/" + this.collectionId + "/query";
            
            Map<String, Object> request = Map.of(
                "query_texts", List.of(queryText),
                "n_results", Math.min(maxResults, config.getMaxResults()),
                "include", List.of("metadatas", "documents", "distances")
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    return parseSimilarityResults(response.getBody());
                } else {
                    logger.error("Failed to perform similarity search. Status: {}, Response: {}", 
                               response.getStatusCode(), response.getBody());
                    return Collections.emptyList();
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 422) {
                    // ChromaDB requires embeddings - let's implement a workaround
                    logger.warn("ChromaDB requires pre-computed embeddings. Using text-based fallback search.");
                    return performTextBasedSearch(queryText, maxResults);
                } else {
                    logger.error("HTTP error during similarity search: {}", e.getMessage());
                    return Collections.emptyList();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error performing similarity search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
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
        
        // Add serialized JSON for complex fields
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
            JsonNode ids = root.get("ids").get(0); // First query results
            JsonNode documents = root.get("documents").get(0);
            JsonNode metadatas = root.get("metadatas").get(0);
            JsonNode distances = root.get("distances").get(0);
            
            List<SimilarityMatch> matches = new ArrayList<>();
            
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i).asText();
                String document = documents.get(i).asText();
                JsonNode metadata = metadatas.get(i);
                double distance = distances.get(i).asDouble();
                
                // Convert distance to similarity (1 - distance, assuming cosine distance)
                double similarity = Math.max(0, 1 - distance);
                
                // Only include results above threshold
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
            // Extract from metadata with null checks
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
            if (metadata.has("usage_count") && !metadata.get("usage_count").isNull()) {
                entry.setUsageCount(metadata.get("usage_count").asInt());
            }
            if (metadata.has("resolution_time_minutes") && !metadata.get("resolution_time_minutes").isNull()) {
                entry.setResolutionTimeMinutes(metadata.get("resolution_time_minutes").asInt());
            }
            
            // Parse JSON arrays from metadata
            if (metadata.has("environments") && !metadata.get("environments").isNull()) {
                try {
                    JsonNode envArray = objectMapper.readTree(metadata.get("environments").asText());
                    List<String> environments = new ArrayList<>();
                    envArray.forEach(env -> environments.add(env.asText()));
                    entry.setEnvironments(environments);
                } catch (Exception e) {
                    logger.debug("Error parsing environments for entry '{}': {}", entry.getTitle(), e.getMessage());
                }
            }
            
            if (metadata.has("technologies") && !metadata.get("technologies").isNull()) {
                try {
                    JsonNode techArray = objectMapper.readTree(metadata.get("technologies").asText());
                    List<String> technologies = new ArrayList<>();
                    techArray.forEach(tech -> technologies.add(tech.asText()));
                    entry.setTechnologies(technologies);
                } catch (Exception e) {
                    logger.debug("Error parsing technologies for entry '{}': {}", entry.getTitle(), e.getMessage());
                }
            }
            
            if (metadata.has("tags") && !metadata.get("tags").isNull()) {
                try {
                    JsonNode tagArray = objectMapper.readTree(metadata.get("tags").asText());
                    List<String> tags = new ArrayList<>();
                    tagArray.forEach(tag -> tags.add(tag.asText()));
                    entry.setTags(tags);
                } catch (Exception e) {
                    logger.debug("Error parsing tags for entry '{}': {}", entry.getTitle(), e.getMessage());
                }
            }
            
            // Parse the document text to extract symptoms, root cause, and solution
            parseDocumentText(entry, document);
            
            logger.debug("Successfully parsed knowledge entry: {} ({})", entry.getTitle(), entry.getId());
            
        } catch (Exception e) {
            logger.warn("Error parsing knowledge entry metadata for ID '{}': {}", id, e.getMessage());
        }
        
        return entry;
    }
    
    private void parseDocumentText(KnowledgeEntry entry, String document) {
        // Simple parsing of the embedded text format
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
    
    private List<KnowledgeEntry> parseKnowledgeEntries(String responseBody) {
        try {
            logger.debug("Parsing ChromaDB response: {}", responseBody);
            JsonNode root = objectMapper.readTree(responseBody);
            
            JsonNode ids = root.get("ids");
            JsonNode documents = root.get("documents");
            JsonNode metadatas = root.get("metadatas");
            
            List<KnowledgeEntry> entries = new ArrayList<>();
            
            if (ids != null && ids.isArray() && documents != null && documents.isArray() && 
                metadatas != null && metadatas.isArray()) {
                
                int size = ids.size();
                logger.debug("Found {} knowledge entries in response", size);
                
                for (int i = 0; i < size; i++) {
                    try {
                        String id = ids.get(i).asText();
                        String document = documents.get(i).asText();
                        JsonNode metadata = metadatas.get(i);
                        
                        KnowledgeEntry entry = parseKnowledgeEntry(id, document, metadata);
                        entries.add(entry);
                        
                        logger.debug("Parsed entry {}: {}", i + 1, entry.getTitle());
                        
                    } catch (Exception e) {
                        logger.warn("Error parsing knowledge entry at index {}: {}", i, e.getMessage());
                    }
                }
            } else {
                logger.warn("Invalid or missing data arrays in ChromaDB response. ids: {}, documents: {}, metadatas: {}", 
                           ids != null, documents != null, metadatas != null);
            }
            
            logger.info("Successfully parsed {} knowledge entries from ChromaDB", entries.size());
            return entries;
            
        } catch (Exception e) {
            logger.error("Error parsing knowledge entries response: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Fallback text-based search when embeddings are not available
     */
    private List<SimilarityMatch> performTextBasedSearch(String queryText, int maxResults) {
        try {
            logger.info("Using text-based fallback search for query: {}", queryText);
            
            // Get all entries and perform simple text matching
            List<KnowledgeEntry> allEntries = getAllKnowledgeEntries();
            
            List<SimilarityMatch> matches = new ArrayList<>();
            String lowerQuery = queryText.toLowerCase();
            
            for (KnowledgeEntry entry : allEntries) {
                double similarity = calculateTextSimilarity(lowerQuery, entry);
                
                if (similarity >= config.getSimilarityThreshold()) {
                    SimilarityMatch match = new SimilarityMatch(entry, similarity);
                    matches.add(match);
                }
            }
            
            // Sort by similarity score (highest first) and limit results
            matches.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
            
            return matches.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error in text-based fallback search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Simple text similarity calculation based on keyword matching
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
        
        // Calculate similarity as percentage of matched words
        double similarity = totalWords > 0 ? (double) matches / totalWords : 0.0;
        
        // Bonus for exact phrase matches
        if (entryText.contains(query)) {
            similarity += 0.3;
        }
        
        // Bonus for pattern type matches
        if (entry.getPatternType() != null && 
            query.contains(entry.getPatternType().toLowerCase().replace("_", " "))) {
            similarity += 0.2;
        }
        
        return Math.min(similarity, 1.0); // Cap at 1.0
    }
}
