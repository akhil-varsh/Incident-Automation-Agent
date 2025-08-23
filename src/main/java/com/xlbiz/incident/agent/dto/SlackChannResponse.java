package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Slack message posting response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackChannResponse extends SlackApiResponse {
    
    @JsonProperty("channel")
    private String channel;
    
    @JsonProperty("ts")
    private String ts;
    
    @JsonProperty("message")
    private SlackMessage message;
    
    public SlackChannResponse() {}
    
    // Getters and setters
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public String getTs() {
        return ts;
    }
    
    public void setTs(String ts) {
        this.ts = ts;
    }
    
    public SlackMessage getMessage() {
        return message;
    }
    
    public void setMessage(SlackMessage message) {
        this.message = message;
    }
    
    // THIS IS THE CLASS FOR SLACK MESSAGE - RENAMED FROM SlackChannel
    @JsonIgnoreProperties(ignoreUnknown = true) // CRITICAL: This must be here
    public static class SlackMessage {

        @JsonProperty("bot_id")
        private String botId;
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("ts")
        private String ts;
        
        @JsonProperty("subtype")
        private String subtype;
        
        // ADD THESE MISSING FIELDS TO FIX THE ERROR:
        @JsonProperty("icons")
        private Object icons; // This was missing and causing your error
        
        @JsonProperty("app_id")
        private String appId;
        
        @JsonProperty("bot_profile")
        private Object botProfile; // Use Object for complex nested structures
        
        @JsonProperty("team")
        private String team;
        
        @JsonProperty("user")
        private String user;
        
        @JsonProperty("thread_ts")
        private String threadTs;
        
        @JsonProperty("reply_count")
        private Integer replyCount;
        
        @JsonProperty("replies")
        private Object replies;
        
        @JsonProperty("blocks")
        private Object blocks;
        
        @JsonProperty("edited")
        private Object edited;
        
        @JsonProperty("client_msg_id")
        private String clientMsgId;
        
        public SlackMessage() {}
        
        // Getters and setters
        public String getBotId() {
            return botId;
        }
        
        public void setBotId(String botId) {
            this.botId = botId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getTs() {
            return ts;
        }
        
        public void setTs(String ts) {
            this.ts = ts;
        }
        
        public String getSubtype() {
            return subtype;
        }
        
        public void setSubtype(String subtype) {
            this.subtype = subtype;
        }
        
        public Object getIcons() {
            return icons;
        }
        
        public void setIcons(Object icons) {
            this.icons = icons;
        }
        
        public String getAppId() {
            return appId;
        }
        
        public void setAppId(String appId) {
            this.appId = appId;
        }
        
        public Object getBotProfile() {
            return botProfile;
        }
        
        public void setBotProfile(Object botProfile) {
            this.botProfile = botProfile;
        }
        
        public String getTeam() {
            return team;
        }
        
        public void setTeam(String team) {
            this.team = team;
        }
        
        public String getUser() {
            return user;
        }
        
        public void setUser(String user) {
            this.user = user;
        }
        
        public String getThreadTs() {
            return threadTs;
        }
        
        public void setThreadTs(String threadTs) {
            this.threadTs = threadTs;
        }
        
        public Integer getReplyCount() {
            return replyCount;
        }
        
        public void setReplyCount(Integer replyCount) {
            this.replyCount = replyCount;
        }
        
        public Object getReplies() {
            return replies;
        }
        
        public void setReplies(Object replies) {
            this.replies = replies;
        }
        
        public Object getBlocks() {
            return blocks;
        }
        
        public void setBlocks(Object blocks) {
            this.blocks = blocks;
        }
        
        public Object getEdited() {
            return edited;
        }
        
        public void setEdited(Object edited) {
            this.edited = edited;
        }
        
        public String getClientMsgId() {
            return clientMsgId;
        }
        
        public void setClientMsgId(String clientMsgId) {
            this.clientMsgId = clientMsgId;
        }
    }
}