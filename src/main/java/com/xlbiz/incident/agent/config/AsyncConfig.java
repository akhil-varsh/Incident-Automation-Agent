package com.xlbiz.incident.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing in the incident automation agent.
 * Configures thread pools for non-blocking integration operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Task executor for incident processing operations.
     * Used for Slack and Jira integrations to prevent blocking the main request thread.
     * 
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "incidentTaskExecutor")
    public Executor incidentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("incident-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for AI processing operations.
     * Separate pool for AI classification to isolate AI service latency.
     * 
     * @return Configured ThreadPoolTaskExecutor for AI operations
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}