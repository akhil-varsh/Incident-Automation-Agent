package com.xlbiz.incident.agent.controller.exception;

/**
 * Custom exception for incident processing errors.
 * Thrown when business logic validation fails during incident processing.
 */
public class IncidentProcessingException extends RuntimeException {

    private String incidentId;
    private String errorCode;

    public IncidentProcessingException(String message) {
        super(message);
    }

    public IncidentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncidentProcessingException(String message, String incidentId) {
        super(message);
        this.incidentId = incidentId;
    }

    public IncidentProcessingException(String message, String incidentId, String errorCode) {
        super(message);
        this.incidentId = incidentId;
        this.errorCode = errorCode;
    }

    public IncidentProcessingException(String message, String incidentId, Throwable cause) {
        super(message, cause);
        this.incidentId = incidentId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
