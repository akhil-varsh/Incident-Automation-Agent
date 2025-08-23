package com.xlbiz.incident.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing a voice call in the incident automation system.
 * Stores detailed voice call processing information and transcription data.
 */
@Entity
@Table(name = "voice_calls", indexes = {
    @Index(name = "idx_voice_calls_incident_id", columnList = "incident_id"),
    @Index(name = "idx_voice_calls_conversation_uuid", columnList = "conversation_uuid"),
    @Index(name = "idx_voice_calls_caller_number", columnList = "caller_number"),
    @Index(name = "idx_voice_calls_processing_status", columnList = "processing_status"),
    @Index(name = "idx_voice_calls_created_at", columnList = "created_at")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VoiceCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the associated incident
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", foreignKey = @ForeignKey(name = "fk_voice_call_incident"))
    private Incident incident;

    /**
     * Unique conversation identifier from Twilio
     */
    @Column(name = "conversation_uuid", unique = true, nullable = false, length = 100)
    private String conversationUuid;

    /**
     * Caller's phone number
     */
    @Column(name = "caller_number", length = 20)
    private String callerNumber;

    /**
     * URL to the voice recording
     */
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    /**
     * Twilio recording SID for reference
     */
    @Column(name = "recording_sid", length = 100)
    private String recordingSid;

    /**
     * Transcribed text from the voice call
     */
    @Column(name = "transcription", columnDefinition = "TEXT")
    private String transcription;

    /**
     * Call duration in seconds
     */
    @Column(name = "call_duration")
    private Integer callDuration;

    /**
     * Current processing status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private VoiceProcessingStatus processingStatus = VoiceProcessingStatus.RECEIVED;

    /**
     * Speech-to-text service used (google, deepgram, etc.)
     */
    @Column(name = "speech_to_text_service", length = 20)
    private String speechToTextService;

    /**
     * Confidence score from speech-to-text service (0.0 to 1.0)
     */
    @Column(name = "transcription_confidence", precision = 3, scale = 2)
    private java.math.BigDecimal transcriptionConfidence;

    /**
     * Error message if processing failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when the voice call was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the voice call was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp when processing was completed
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Constructors

    public VoiceCall() {
    }

    public VoiceCall(String conversationUuid, String callerNumber, String recordingUrl) {
        this.conversationUuid = conversationUuid;
        this.callerNumber = callerNumber;
        this.recordingUrl = recordingUrl;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getConversationUuid() {
        return conversationUuid;
    }

    public void setConversationUuid(String conversationUuid) {
        this.conversationUuid = conversationUuid;
    }

    public String getCallerNumber() {
        return callerNumber;
    }

    public void setCallerNumber(String callerNumber) {
        this.callerNumber = callerNumber;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public String getRecordingSid() {
        return recordingSid;
    }

    public void setRecordingSid(String recordingSid) {
        this.recordingSid = recordingSid;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    public Integer getCallDuration() {
        return callDuration;
    }

    public void setCallDuration(Integer callDuration) {
        this.callDuration = callDuration;
    }

    public VoiceProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(VoiceProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
        // Automatically set processed timestamp when status changes to PROCESSED
        if (processingStatus == VoiceProcessingStatus.PROCESSED && this.processedAt == null) {
            this.processedAt = LocalDateTime.now();
        }
    }

    public String getSpeechToTextService() {
        return speechToTextService;
    }

    public void setSpeechToTextService(String speechToTextService) {
        this.speechToTextService = speechToTextService;
    }

    public java.math.BigDecimal getTranscriptionConfidence() {
        return transcriptionConfidence;
    }

    public void setTranscriptionConfidence(java.math.BigDecimal transcriptionConfidence) {
        this.transcriptionConfidence = transcriptionConfidence;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    // Additional fields and methods for outbound call support
    
    /**
     * Twilio Call SID for outbound calls
     */
    @Column(name = "call_sid", length = 100)
    private String callSid;
    
    /**
     * Phone number for outbound calls
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    /**
     * Call direction (inbound/outbound)
     */
    @Column(name = "direction", length = 10)
    private String direction;
    
    /**
     * Call type (INCIDENT_NOTIFICATION, etc.)
     */
    @Column(name = "call_type", length = 50)
    private String callType;
    
    /**
     * Twilio call status
     */
    @Column(name = "twilio_status", length = 20)
    private String twilioStatus;
    
    /**
     * Call duration in seconds (for outbound calls)
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    /**
     * Call end timestamp
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public String getCallSid() {
        return callSid;
    }

    public void setCallSid(String callSid) {
        this.callSid = callSid;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getTwilioStatus() {
        return twilioStatus;
    }

    public void setTwilioStatus(String twilioStatus) {
        this.twilioStatus = twilioStatus;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    // Utility methods

    /**
     * Check if the voice call has been successfully processed
     */
    public boolean isProcessed() {
        return processingStatus == VoiceProcessingStatus.PROCESSED;
    }

    /**
     * Check if the voice call processing failed
     */
    public boolean hasFailed() {
        return processingStatus == VoiceProcessingStatus.ERROR;
    }

    /**
     * Check if transcription is available
     */
    public boolean hasTranscription() {
        return transcription != null && !transcription.trim().isEmpty();
    }

    /**
     * Get formatted call duration
     */
    public String getFormattedDuration() {
        if (callDuration == null) {
            return "Unknown";
        }
        
        int minutes = callDuration / 60;
        int seconds = callDuration % 60;
        
        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    // equals, hashCode, toString

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoiceCall voiceCall = (VoiceCall) o;
        return Objects.equals(conversationUuid, voiceCall.conversationUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationUuid);
    }

    @Override
    public String toString() {
        return "VoiceCall{" +
                "id=" + id +
                ", conversationUuid='" + conversationUuid + '\'' +
                ", callerNumber='" + callerNumber + '\'' +
                ", processingStatus=" + processingStatus +
                ", createdAt=" + createdAt +
                '}';
    }
}