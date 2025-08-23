package com.xlbiz.incident.agent.dto;

import java.time.LocalDateTime;

public class OutboundCallResponse {
    private String callSid;
    private String toPhoneNumber;
    private String callType;
    private String status; // INITIATED, RINGING, ANSWERED, COMPLETED, FAILED
    private String twimlUrl;
    private boolean success;
    private String message;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;

    // Constructors
    public OutboundCallResponse() {}

    public OutboundCallResponse(String toPhoneNumber, String callType) {
        this.toPhoneNumber = toPhoneNumber;
        this.callType = callType;
        this.requestedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getCallSid() {
        return callSid;
    }

    public void setCallSid(String callSid) {
        this.callSid = callSid;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTwimlUrl() {
        return twimlUrl;
    }

    public void setTwimlUrl(String twimlUrl) {
        this.twimlUrl = twimlUrl;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "OutboundCallResponse{" +
                "callSid='" + callSid + '\'' +
                ", toPhoneNumber='" + toPhoneNumber + '\'' +
                ", callType='" + callType + '\'' +
                ", status='" + status + '\'' +
                ", twimlUrl='" + twimlUrl + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", requestedAt=" + requestedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}