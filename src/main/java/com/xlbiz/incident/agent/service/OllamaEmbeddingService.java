package com.xlbiz.incident.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service for generating embeddings using local Ollama with nomic-embed-text model
 */
@Service
public class OllamaEmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaEmbeddingService.class);
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${ollama.embedding-model:nomic-embed-text:v1.5}")
    private String embeddingModel;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OllamaEmbeddingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generate embeddings for a single text using Ollama
     */
    public List<Double> generateEmbedding(String text) {
        try {
            logger.debug("Generating embedding for text: {}", text.substring(0, Math.min(text.length(), 100)) + "...");
            
            String url = ollamaBaseUrl + "/api/embeddings";
            
            Map<String, Object> request = Map.of(
                "model", embeddingModel,
                "prompt", text
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode embeddingArray = responseJson.get("embedding");
                
                if (embeddingArray != null && embeddingArray.isArray()) {
                    List<Double> embedding = new java.util.ArrayList<>();
                    for (JsonNode value : embeddingArray) {
                        embedding.add(value.asDouble());
                    }
                    
                    logger.debug("Generated embedding with {} dimensions", embedding.size());
                    return embedding;
                } else {
                    logger.error("Invalid embedding response format from Ollama");
                    throw new RuntimeException("Invalid embedding response format");
                }
            } else {
                logger.error("Failed to generate embedding. Status: {}, Response: {}", 
                           response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to generate embedding from Ollama");
            }
            
        } catch (Exception e) {
            logger.error("Error generating embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    /**
     * Generate embeddings for multiple texts in batch
     */
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        List<List<Double>> embeddings = new java.util.ArrayList<>();
        
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        
        return embeddings;
    }
    
    /**
     * Check if Ollama service is available
     */
    public boolean isAvailable() {
        try {
            // Test with a simple embedding request
            String url = ollamaBaseUrl + "/api/embeddings";
            
            Map<String, Object> testRequest = Map.of(
                "model", embeddingModel,
                "prompt", "test"
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(testRequest, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Ollama embedding model '{}' is available", embeddingModel);
                return true;
            } else {
                logger.warn("Ollama embedding model not responding. Status: {}", response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error checking Ollama availability: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get embedding model information
     */
    public Map<String, Object> getModelInfo() {
        return Map.of(
            "service", "ollama",
            "base_url", ollamaBaseUrl,
            "model", embeddingModel,
            "available", isAvailable()
        );
    }
}