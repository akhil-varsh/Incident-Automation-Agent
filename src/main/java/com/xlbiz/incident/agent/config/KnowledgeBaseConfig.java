package com.xlbiz.incident.agent.config;

import com.xlbiz.incident.agent.service.EnhancedKnowledgeBaseService;
import com.xlbiz.incident.agent.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuration for Knowledge Base services initialization
 */
@Configuration
public class KnowledgeBaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseConfig.class);
    
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    
    @Autowired
    private EnhancedKnowledgeBaseService enhancedKnowledgeBaseService;
    
    /**
     * Initialize knowledge base services after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeKnowledgeBase() {
        try {
            logger.info("Initializing knowledge base services...");
            
            // Initialize original knowledge base service
            knowledgeBaseService.initializeCollection();
            
            // Initialize enhanced knowledge base service with Ollama embeddings
            enhancedKnowledgeBaseService.initializeCollection();
            
            logger.info("Knowledge base services initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize knowledge base services: {}", e.getMessage(), e);
            // Don't fail application startup if knowledge base initialization fails
        }
    }
}