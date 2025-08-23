package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Slack API responses
 */
@JsonIgnoreProperties(ignoreUnknown = true)

public class SlackApiResponse {
    
    @JsonProperty("ok")
    private boolean ok;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("warning")
    private String warning;
    
    public SlackApiResponse() {}
    
    public boolean isOk() {
        return ok;
    }
    
    public void setOk(boolean ok) {
        this.ok = ok;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getWarning() {
        return warning;
    }
    
    public void setWarning(String warning) {
        this.warning = warning;
    }
}
