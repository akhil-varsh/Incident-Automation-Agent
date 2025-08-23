package com.xlbiz.incident.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Slack integration
 */
@Configuration
public class SlackConfig {
    
    @Value("${integrations.slack.bot-token:}")
    private String botToken;
    
    @Value("${integrations.slack.signing-secret:}")
    private String signingSecret;
    
    @Value("${integrations.slack.enabled:false}")
    private boolean enabled;
    
    @Value("${app.slack.api-timeout:30000}")
    private int apiTimeout;
    
    @Value("${app.slack.default-channel:#incidents}")
    private String defaultChannel;
    
    @Value("${app.slack.workspace-url:}")
    private String workspaceUrl;
    
    /**
     * RestTemplate configured for Slack API calls
     */
    @Bean("slackRestTemplate")
    public RestTemplate slackRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure timeout
        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        ((SimpleClientHttpRequestFactory) factory).setConnectTimeout(apiTimeout);
        ((SimpleClientHttpRequestFactory) factory).setReadTimeout(apiTimeout);
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }
    
    // Getters
    public String getBotToken() {
        return botToken;
    }
    
    public String getSigningSecret() {
        return signingSecret;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getApiTimeout() {
        return apiTimeout;
    }
    
    public String getDefaultChannel() {
        return defaultChannel;
    }
    
    public String getWorkspaceUrl() {
        return workspaceUrl;
    }
    
    public String getSlackApiUrl() {
        return "https://slack.com/api";
    }
}
