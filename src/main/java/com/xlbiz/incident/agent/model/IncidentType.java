package com.xlbiz.incident.agent.model;

/**
 * Enumeration of incident types supported by the automation agent.
 * These types are used for AI classification and knowledge base matching.
 */
public enum IncidentType {
    /**
     * Database connectivity issues, connection pool exhaustion, query timeouts
     */
    DATABASE_CONNECTION_ERROR("Database Connection Error"),
    
    /**
     * High CPU utilization, performance degradation
     */
    HIGH_CPU("High CPU Usage"),
    
    /**
     * Disk space issues, storage full conditions
     */
    DISK_FULL("Disk Full"),
    
    /**
     * Memory-related issues, out of memory errors
     */
    MEMORY_LEAK("Memory Leak"),
    
    /**
     * Network connectivity problems, timeouts, DNS issues
     */
    NETWORK_ISSUE("Network Issue"),
    
    /**
     * Application crashes, service unavailability
     */
    SERVICE_DOWN("Service Down"),
    
    /**
     * Security-related incidents, unauthorized access
     */
    SECURITY_BREACH("Security Breach"),
    
    /**
     * Data corruption, integrity issues
     */
    DATA_CORRUPTION("Data Corruption"),
    
    /**
     * API failures, endpoint unavailability
     */
    API_FAILURE("API Failure"),
    
    /**
     * Deployment-related issues
     */
    DEPLOYMENT_FAILURE("Deployment Failure"),
    
    /**
     * Infrastructure-related failures, hardware issues, server failures
     */
    INFRASTRUCTURE_FAILURE("Infrastructure Failure"),
    
    /**
     * Generic or unclassified incidents
     */
    OTHER("Other");

    private final String displayName;

    IncidentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get incident type from string value (case-insensitive)
     * Supports both enum names and display names
     */
    public static IncidentType fromString(String value) {
        if (value == null) {
            return OTHER;
        }
        
        // First try enum name
        try {
            return IncidentType.valueOf(value.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Then try display name matching
            for (IncidentType type : values()) {
                if (type.getDisplayName().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return OTHER;
        }
    }
}