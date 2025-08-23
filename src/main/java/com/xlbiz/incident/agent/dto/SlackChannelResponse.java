package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Slack channel creation response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackChannelResponse extends SlackApiResponse {
    
    @JsonProperty("channel")
    private SlackChannel channel;
    
    public SlackChannelResponse() {}
    
    public SlackChannel getChannel() {
        return channel;
    }
    
    public void setChannel(SlackChannel channel) {
        this.channel = channel;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    
    public static class SlackChannel {
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("is_channel")
        private boolean isChannel;
        
        @JsonProperty("created")
        private long created;
        
        @JsonProperty("creator")
        private String creator;
        
        @JsonProperty("is_archived")
        private boolean isArchived;
        
        @JsonProperty("is_general")
        private boolean isGeneral;
        
        @JsonProperty("is_member")
        private boolean isMember;
        
        @JsonProperty("is_private")
        private boolean isPrivate;
        
        @JsonProperty("purpose")
        private Purpose purpose;
        
        @JsonProperty("topic")
        private Topic topic;
        
        public SlackChannel() {}
        
        // Getters and setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public boolean isChannel() {
            return isChannel;
        }
        
        public void setChannel(boolean channel) {
            isChannel = channel;
        }
        
        public long getCreated() {
            return created;
        }
        
        public void setCreated(long created) {
            this.created = created;
        }
        
        public String getCreator() {
            return creator;
        }
        
        public void setCreator(String creator) {
            this.creator = creator;
        }
        
        public boolean isArchived() {
            return isArchived;
        }
        
        public void setArchived(boolean archived) {
            isArchived = archived;
        }
        
        public boolean isGeneral() {
            return isGeneral;
        }
        
        public void setGeneral(boolean general) {
            isGeneral = general;
        }
        
        public boolean isMember() {
            return isMember;
        }
        
        public void setMember(boolean member) {
            isMember = member;
        }
        
        public boolean isPrivate() {
            return isPrivate;
        }
        
        public void setPrivate(boolean aPrivate) {
            isPrivate = aPrivate;
        }
        
        public Purpose getPurpose() {
            return purpose;
        }
        
        public void setPurpose(Purpose purpose) {
            this.purpose = purpose;
        }
        
        public Topic getTopic() {
            return topic;
        }
        
        public void setTopic(Topic topic) {
            this.topic = topic;
        }
        
        public static class Purpose {
            @JsonProperty("value")
            private String value;
            
            @JsonProperty("creator")
            private String creator;
            
            @JsonProperty("last_set")
            private long lastSet;
            
            public String getValue() {
                return value;
            }
            
            public void setValue(String value) {
                this.value = value;
            }
            
            public String getCreator() {
                return creator;
            }
            
            public void setCreator(String creator) {
                this.creator = creator;
            }
            
            public long getLastSet() {
                return lastSet;
            }
            
            public void setLastSet(long lastSet) {
                this.lastSet = lastSet;
            }
        }
        
        public static class Topic {
            @JsonProperty("value")
            private String value;
            
            @JsonProperty("creator")
            private String creator;
            
            @JsonProperty("last_set")
            private long lastSet;
            
            public String getValue() {
                return value;
            }
            
            public void setValue(String value) {
                this.value = value;
            }
            
            public String getCreator() {
                return creator;
            }
            
            public void setCreator(String creator) {
                this.creator = creator;
            }
            
            public long getLastSet() {
                return lastSet;
            }
            
            public void setLastSet(long lastSet) {
                this.lastSet = lastSet;
            }
        }
    }
}
