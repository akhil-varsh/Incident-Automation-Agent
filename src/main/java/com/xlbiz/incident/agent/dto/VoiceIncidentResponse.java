package com.xlbiz.incident.agent.dto;

import java.time.LocalDateTime;

public class VoiceIncidentResponse {
    
    private String incidentId;
    private String callUuid;
    private String callerNumber;
    private String transcription;
    private String aiClassification;
    private String processingStatus;
    private LocalDateTime processedAt;
    private String message;

    // Constructors
    public VoiceIncidentResponse() {}

    public VoiceIncidentResponse(String incidentId, String callUuid, String message) {
        this.incidentId = incidentId;
        this.callUuid = callUuid;
        this.message = message;
        this.processedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getCallUuid() {
        return callUuid;
    }

    public void setCallUuid(String callUuid) {
        this.callUuid = callUuid;
    }

    public String getCallerNumber() {
        return callerNumber;
    }

    public void setCallerNumber(String callerNumber) {
        this.callerNumber = callerNumber;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    public String getAiClassification() {
        return aiClassification;
    }

    public void setAiClassification(String aiClassification) {
        this.aiClassification = aiClassification;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "VoiceIncidentResponse{" +
                "incidentId='" + incidentId + '\'' +
                ", callUuid='" + callUuid + '\'' +
                ", callerNumber='" + callerNumber + '\'' +
                ", transcription='" + transcription + '\'' +
                ", aiClassification='" + aiClassification + '\'' +
                ", processingStatus='" + processingStatus + '\'' +
                ", processedAt=" + processedAt +
                ", message='" + message + '\'' +
                '}';
    }
}