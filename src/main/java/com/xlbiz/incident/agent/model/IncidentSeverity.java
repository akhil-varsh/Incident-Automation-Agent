package com.xlbiz.incident.agent.model;

/**
 * Enumeration of incident severity levels.
 * Used by AI classification service to determine incident priority.
 */
public enum IncidentSeverity {
    /**
     * Low severity - Minor issues with minimal impact
     * - No service disruption
     * - Affects few users or non-critical functionality
     * - Can be resolved during normal business hours
     */
    LOW("Low", 1),
    
    /**
     * Medium severity - Moderate issues with noticeable impact
     * - Partial service disruption
     * - Affects multiple users or important functionality
     * - Should be resolved within business hours
     */
    MEDIUM("Medium", 2),
    
    /**
     * High severity - Critical issues requiring immediate attention
     * - Complete service disruption
     * - Affects many users or critical functionality
     * - Requires immediate response and resolution
     */
    HIGH("High", 3),
    
    /**
     * Unknown severity - Used when AI classification is not available
     * Will be classified by AI service or fallback logic
     */
    UNKNOWN("Unknown", 0);

    private final String displayName;
    private final int priority;

    IncidentSeverity(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Get severity from string value (case-insensitive)
     */
    public static IncidentSeverity fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        try {
            return IncidentSeverity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * Check if this severity requires immediate attention
     */
    public boolean isUrgent() {
        return this == HIGH;
    }

    /**
     * Check if this severity requires Slack channel creation
     */
    public boolean requiresSlackChannel() {
        return this == MEDIUM || this == HIGH;
    }
}