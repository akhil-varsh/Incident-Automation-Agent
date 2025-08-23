package com.xlbiz.incident.agent.dto;

import java.util.Map;

public class OutboundCallRequest {
    private String toPhoneNumber;
    private String callType; // INCIDENT_NOTIFICATION, INCIDENT_UPDATE, CUSTOM
    private String incidentId;
    private String severity;
    private String status;
    private String message;
    private String twimlUrl;
    private Map<String, String> parameters;

    // Constructors
    public OutboundCallRequest() {}

    public OutboundCallRequest(String toPhoneNumber, String callType) {
        this.toPhoneNumber = toPhoneNumber;
        this.callType = callType;
    }

    // Getters and Setters
    public String getToPhoneNumber() {
        return toPhoneNumber;
    }

    public void setToPhoneNumber(String toPhoneNumber) {
        this.toPhoneNumber = toPhoneNumber;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTwimlUrl() {
        return twimlUrl;
    }

    public void setTwimlUrl(String twimlUrl) {
        this.twimlUrl = twimlUrl;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "OutboundCallRequest{" +
                "toPhoneNumber='" + toPhoneNumber + '\'' +
                ", callType='" + callType + '\'' +
                ", incidentId='" + incidentId + '\'' +
                ", severity='" + severity + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", twimlUrl='" + twimlUrl + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}