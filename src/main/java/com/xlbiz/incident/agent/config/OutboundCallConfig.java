package com.xlbiz.incident.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for outbound call settings
 */
@Configuration
@ConfigurationProperties(prefix = "integrations.twilio.outbound")
public class OutboundCallConfig {

    private boolean enabled = true;
    private String developerPhoneNumber;
    private String escalationPhoneNumber;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDeveloperPhoneNumber() {
        return developerPhoneNumber;
    }

    public void setDeveloperPhoneNumber(String developerPhoneNumber) {
        this.developerPhoneNumber = developerPhoneNumber;
    }

    public String getEscalationPhoneNumber() {
        return escalationPhoneNumber;
    }

    public void setEscalationPhoneNumber(String escalationPhoneNumber) {
        this.escalationPhoneNumber = escalationPhoneNumber;
    }

    /**
     * Get the appropriate phone number based on incident severity
     */
    public String getPhoneNumberForSeverity(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity) && escalationPhoneNumber != null) {
            return escalationPhoneNumber;
        }
        return developerPhoneNumber;
    }

    /**
     * Check if outbound calling is properly configured
     */
    public boolean isConfigured() {
        return enabled && developerPhoneNumber != null && !developerPhoneNumber.trim().isEmpty();
    }
}