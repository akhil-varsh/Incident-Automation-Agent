package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Slack message response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackMessageResponse extends SlackApiResponse {
    
    @JsonProperty("channel")
    private String channel;
    
    @JsonProperty("ts")
    private String timestamp;
    
    @JsonProperty("message")
    private SlackMessage message;
    
    public SlackMessageResponse() {}
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public SlackMessage getMessage() {
        return message;
    }
    
    public void setMessage(SlackMessage message) {
        this.message = message;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlackMessage {
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("bot_id")
        private String botId;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("subtype")
        private String subtype;
        
        @JsonProperty("ts")
        private String timestamp;
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getBotId() {
            return botId;
        }
        
        public void setBotId(String botId) {
            this.botId = botId;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getSubtype() {
            return subtype;
        }
        
        public void setSubtype(String subtype) {
            this.subtype = subtype;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}
