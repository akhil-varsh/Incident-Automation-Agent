package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VoiceCallRequest {
    
    @JsonProperty("from")
    private String from;
    
    @JsonProperty("to")
    private String to;
    
    @JsonProperty("uuid")
    private String uuid;
    
    @JsonProperty("conversation_uuid")
    private String conversationUuid;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("direction")
    private String direction;
    
    @JsonProperty("timestamp")
    private String timestamp;

    // Constructors
    public VoiceCallRequest() {}

    // Getters and Setters
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getConversationUuid() {
        return conversationUuid;
    }

    public void setConversationUuid(String conversationUuid) {
        this.conversationUuid = conversationUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "VoiceCallRequest{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", uuid='" + uuid + '\'' +
                ", conversationUuid='" + conversationUuid + '\'' +
                ", status='" + status + '\'' +
                ", direction='" + direction + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}