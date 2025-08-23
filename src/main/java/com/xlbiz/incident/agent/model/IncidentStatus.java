package com.xlbiz.incident.agent.model;

/**
 * Enumeration of incident processing status.
 * Tracks the lifecycle of an incident through the automation system.
 */
public enum IncidentStatus {
    /**
     * Incident received but not yet processed
     */
    RECEIVED("Received"),
    
    /**
     * AI classification in progress
     */
    CLASSIFYING("Classifying"),
    
    /**
     * AI classification completed, integrations in progress
     */
    PROCESSING("Processing"),
    
    /**
     * All integrations completed successfully
     */
    PROCESSED("Processed"),
    
    /**
     * Incident is being actively worked on
     */
    IN_PROGRESS("In Progress"),
    
    /**
     * Incident has been resolved
     */
    RESOLVED("Resolved"),
    
    /**
     * Incident was closed without resolution
     */
    CLOSED("Closed"),
    
    /**
     * Processing failed due to errors
     */
    FAILED("Failed");

    private final String displayName;

    IncidentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get status from string value (case-insensitive)
     */
    public static IncidentStatus fromString(String value) {
        if (value == null) {
            return RECEIVED;
        }
        
        try {
            return IncidentStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RECEIVED;
        }
    }

    /**
     * Check if this status indicates the incident is still active
     */
    public boolean isActive() {
        return this != RESOLVED && this != CLOSED && this != FAILED;
    }

    /**
     * Check if this status indicates processing is complete
     */
    public boolean isComplete() {
        return this == PROCESSED || this == RESOLVED || this == CLOSED;
    }

    /**
     * Check if this status indicates an error state
     */
    public boolean isError() {
        return this == FAILED;
    }
}