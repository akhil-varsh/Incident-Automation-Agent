package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VoiceRecordingRequest {
    
    @JsonProperty("recording_url")
    private String recordingUrl;
    
    @JsonProperty("recording_uuid")
    private String recordingUuid;
    
    @JsonProperty("conversation_uuid")
    private String conversationUuid;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("size")
    private Long size;
    
    @JsonProperty("duration")
    private Integer duration;
    
    @JsonProperty("end_on_silence_timeout")
    private Integer endOnSilenceTimeout;
    
    @JsonProperty("end_on_key")
    private String endOnKey;

    // Constructors
    public VoiceRecordingRequest() {}

    // Getters and Setters
    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public String getRecordingUuid() {
        return recordingUuid;
    }

    public void setRecordingUuid(String recordingUuid) {
        this.recordingUuid = recordingUuid;
    }

    public String getConversationUuid() {
        return conversationUuid;
    }

    public void setConversationUuid(String conversationUuid) {
        this.conversationUuid = conversationUuid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getEndOnSilenceTimeout() {
        return endOnSilenceTimeout;
    }

    public void setEndOnSilenceTimeout(Integer endOnSilenceTimeout) {
        this.endOnSilenceTimeout = endOnSilenceTimeout;
    }

    public String getEndOnKey() {
        return endOnKey;
    }

    public void setEndOnKey(String endOnKey) {
        this.endOnKey = endOnKey;
    }

    @Override
    public String toString() {
        return "VoiceRecordingRequest{" +
                "recordingUrl='" + recordingUrl + '\'' +
                ", recordingUuid='" + recordingUuid + '\'' +
                ", conversationUuid='" + conversationUuid + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", size=" + size +
                ", duration=" + duration +
                ", endOnSilenceTimeout=" + endOnSilenceTimeout +
                ", endOnKey='" + endOnKey + '\'' +
                '}';
    }
}