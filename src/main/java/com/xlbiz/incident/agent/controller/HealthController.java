package com.xlbiz.incident.agent.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring application and dependency status.
 * Provides basic health endpoints for load balancers and monitoring systems.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check endpoint that validates core application components.
     * 
     * @return ResponseEntity with health status and component details
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "incident-automation-agent");
        health.put("version", "0.0.1-SNAPSHOT");
        
        // Check database connectivity
        Map<String, String> database = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            database.put("status", "UP");
            database.put("database", connection.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
        }
        health.put("database", database);
        
        return ResponseEntity.ok(health);
    }

    /**
     * Readiness probe endpoint for Kubernetes deployments.
     * 
     * @return ResponseEntity indicating if the application is ready to serve traffic
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "READY");
        status.put("message", "Application is ready to serve requests");
        return ResponseEntity.ok(status);
    }

    /**
     * Liveness probe endpoint for Kubernetes deployments.
     * 
     * @return ResponseEntity indicating if the application is alive
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> live() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ALIVE");
        status.put("message", "Application is running");
        return ResponseEntity.ok(status);
    }
}