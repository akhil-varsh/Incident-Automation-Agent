package com.xlbiz.incident.agent.model;

/**
 * Enumeration of voice call processing statuses
 */
public enum VoiceProcessingStatus {
    /**
     * Voice call received but not yet processed
     */
    RECEIVED,
    
    /**
     * Currently downloading the recording from Twilio
     */
    DOWNLOADING,
    
    /**
     * Currently transcribing the audio using speech-to-text service
     */
    TRANSCRIBING,
    
    /**
     * Voice call successfully processed and incident created
     */
    PROCESSED,
    
    /**
     * Processing failed due to an error
     */
    ERROR,
    
    /**
     * Duplicate processing attempt detected
     */
    DUPLICATE,
    
    /**
     * Call is pending (for outbound calls)
     */
    PENDING,
    
    /**
     * Call is being processed (for outbound calls)
     */
    PROCESSING,
    
    /**
     * Call completed successfully (for outbound calls)
     */
    COMPLETED,
    
    /**
     * Call failed (for outbound calls)
     */
    FAILED
}