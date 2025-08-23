package com.xlbiz.incident.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application class for XLBiz.AI Incident Automation Agent.
 * 
 * This application provides intelligent incident management with AI classification,
 * automated Slack/Jira integrations, and vector-based knowledge base search.
 */
@SpringBootApplication
@EnableAsync
public class IncidentAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentAgentApplication.class, args);
    }
}