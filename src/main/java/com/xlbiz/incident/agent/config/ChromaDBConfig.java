package com.xlbiz.incident.agent.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for ChromaDB vector database integration.
 */
@Configuration
public class ChromaDBConfig {
    
    @Value("${knowledge.chromadb.url:http://localhost:8000}")
    private String chromaDbUrl;
    
    @Value("${knowledge.chromadb.collection:incident_knowledge}")
    private String collectionName;
    
    @Value("${knowledge.chromadb.timeout:30}")
    private int timeoutSeconds;
    
    @Value("${knowledge.chromadb.embedding-model:all-MiniLM-L6-v2}")
    private String embeddingModel;
    
    @Value("${knowledge.chromadb.max-results:10}")
    private int maxResults;
    
    @Value("${knowledge.chromadb.similarity-threshold:0.5}")
    private double similarityThreshold;
    
    /**
     * RestTemplate configured for ChromaDB communication
     * Marked as @Primary to resolve bean conflicts with Spring AI auto-configuration
     */
    @Bean(name = "chromaDbRestTemplate")
    @Primary
    public RestTemplate chromaDbRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
    
    // Getters for configuration values
    public String getChromaDbUrl() {
        return chromaDbUrl;
    }
    
    public String getCollectionName() {
        return collectionName;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public String getEmbeddingModel() {
        return embeddingModel;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
