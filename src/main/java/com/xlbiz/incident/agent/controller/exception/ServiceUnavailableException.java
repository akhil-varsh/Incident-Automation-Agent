package com.xlbiz.incident.agent.controller.exception;

/**
 * Custom exception for service unavailability scenarios.
 * Thrown when external services (AI, Slack, Jira) are temporarily unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {

    private String serviceName;
    private String errorCode;

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceUnavailableException(String message, String serviceName) {
        super(message);
        this.serviceName = serviceName;
    }

    public ServiceUnavailableException(String message, String serviceName, String errorCode) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public ServiceUnavailableException(String message, String serviceName, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
