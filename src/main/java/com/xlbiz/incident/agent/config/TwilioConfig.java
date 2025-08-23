package com.xlbiz.incident.agent.config;

import com.twilio.Twilio;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "integrations.twilio")
@Validated
public class TwilioConfig {

    @NotBlank
    private String accountSid;
    
    @NotBlank
    private String authToken;
    
    @NotBlank
    private String phoneNumber;
    
    private boolean enabled = true;
    
    @NotBlank
    private String webhookBaseUrl;
    
    private String twimlAppSid; // Optional: TwiML App SID for outbound calls
    
    private Recording recording = new Recording();

    @PostConstruct
    public void initTwilio() {
        if (enabled) {
            Twilio.init(accountSid, authToken);
        }
    }

    // Getters and Setters
    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookBaseUrl() {
        return webhookBaseUrl;
    }

    public void setWebhookBaseUrl(String webhookBaseUrl) {
        this.webhookBaseUrl = webhookBaseUrl;
    }

    public Recording getRecording() {
        return recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public String getTwimlAppSid() {
        return twimlAppSid;
    }

    public void setTwimlAppSid(String twimlAppSid) {
        this.twimlAppSid = twimlAppSid;
    }

    public static class Recording {
        private int timeoutSeconds = 30;
        private int maxDurationSeconds = 300;

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxDurationSeconds() {
            return maxDurationSeconds;
        }

        public void setMaxDurationSeconds(int maxDurationSeconds) {
            this.maxDurationSeconds = maxDurationSeconds;
        }
    }
}