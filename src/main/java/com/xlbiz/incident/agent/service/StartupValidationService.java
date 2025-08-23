package com.xlbiz.incident.agent.service;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service to validate application startup and dependency configuration.
 * Performs health checks on critical dependencies during application startup.
 */
@Service
public class StartupValidationService {

    private static final Logger logger = LoggerFactory.getLogger(StartupValidationService.class);

    private final DataSource dataSource;

    @Value("${spring.ai.openai.api-key}")
    private String groqApiKey;

    @Value("${integrations.slack.bot-token}")
    private String slackBotToken;

    @Value("${integrations.jira.base-url}")
    private String jiraBaseUrl;

    public StartupValidationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Validates application configuration and dependencies after startup.
     * This method runs after the application context is fully initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateStartup() {
        logger.info("Starting application validation...");

        boolean allValid = true;

        // Validate database connectivity
        if (!validateDatabase()) {
            allValid = false;
        }

        // Validate AI service configuration
        if (!validateAiConfiguration()) {
            allValid = false;
        }

        // Validate integration configurations
        if (!validateIntegrationConfiguration()) {
            allValid = false;
        }

        if (allValid) {
            logger.info("✅ Application startup validation completed successfully");
            logger.info("🚀 XLBiz.AI Incident Automation Agent is ready to process incidents");
        } else {
            logger.error("❌ Application startup validation failed - some components may not work correctly");
        }
    }

    private boolean validateDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            logger.info("✅ Database connection validated: {}", 
                connection.getMetaData().getDatabaseProductName());
            return true;
        } catch (Exception e) {
            logger.error("❌ Database connection failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean validateAiConfiguration() {
        if (groqApiKey == null || groqApiKey.equals("your_groq_key_here") || groqApiKey.trim().isEmpty()) {
            logger.warn("⚠️ Groq API key not configured - AI classification will use fallback logic");
            return false;
        }
        logger.info("✅ Groq API key configured for gpt-oss-120b model");
        return true;
    }

    private boolean validateIntegrationConfiguration() {
        boolean valid = true;

        // Validate Slack configuration
        if (slackBotToken == null || slackBotToken.trim().isEmpty() || slackBotToken.equals("your_slack_token_here")) {
            logger.warn("⚠️ Slack bot token not configured - Slack integration will be disabled");
            valid = false;
        } else {
            logger.info("✅ Slack integration configured");
        }

        // Validate Jira configuration
        if (jiraBaseUrl == null || jiraBaseUrl.equals("https://company.atlassian.net") || jiraBaseUrl.trim().isEmpty()) {
            logger.warn("⚠️ Jira base URL not configured - Jira integration will be disabled");
            valid = false;
        } else {
            logger.info("✅ Jira integration configured");
        }

        return valid;
    }
}