package com.xlbiz.incident.agent.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Knowledge entry representing an incident pattern with symptoms and solutions.
 * This model is designed to work with ChromaDB for vector-based similarity search.
 */
public class KnowledgeEntry {
    
    @JsonProperty("id")
    private String id;
    
    @NotBlank
    @JsonProperty("title")
    private String title;
    
    @NotBlank
    @JsonProperty("pattern_type")
    private String patternType; // DB_CONNECTION_ERROR, HIGH_CPU, DISK_FULL, etc.
    
    @NotBlank
    @JsonProperty("symptoms")
    private String symptoms; // What the user observes
    
    @NotBlank
    @JsonProperty("root_cause")
    private String rootCause; // Technical explanation
    
    @NotBlank
    @JsonProperty("solution")
    private String solution; // Step-by-step resolution
    
    @JsonProperty("severity")
    private String severity; // Expected severity level
    
    @JsonProperty("environment")
    private List<String> environments; // production, staging, development
    
    @JsonProperty("technologies")
    private List<String> technologies; // database, api, frontend, etc.
    
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @JsonProperty("confidence_score")
    private Double confidenceScore; // How reliable this solution is
    
    @JsonProperty("success_rate")
    private Double successRate; // Historical success rate
    
    @JsonProperty("resolution_time_minutes")
    private Integer resolutionTimeMinutes; // Typical time to resolve
    
    @JsonProperty("prerequisites")
    private List<String> prerequisites; // What's needed before applying solution
    
    @JsonProperty("verification_steps")
    private List<String> verificationSteps; // How to verify solution worked
    
    @JsonProperty("tags")
    private List<String> tags; // Additional categorization
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional flexible data
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("usage_count")
    private Integer usageCount; // How often this solution has been applied
    
    // Constructors
    public KnowledgeEntry() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.usageCount = 0;
    }
    
    public KnowledgeEntry(String title, String patternType, String symptoms, 
                         String rootCause, String solution, String severity) {
        this();
        this.title = title;
        this.patternType = patternType;
        this.symptoms = symptoms;
        this.rootCause = rootCause;
        this.solution = solution;
        this.severity = severity;
        this.confidenceScore = 0.8; // Default confidence
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getPatternType() {
        return patternType;
    }
    
    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }
    
    public String getSymptoms() {
        return symptoms;
    }
    
    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }
    
    public String getRootCause() {
        return rootCause;
    }
    
    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }
    
    public String getSolution() {
        return solution;
    }
    
    public void setSolution(String solution) {
        this.solution = solution;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public List<String> getEnvironments() {
        return environments;
    }
    
    public void setEnvironments(List<String> environments) {
        this.environments = environments;
    }
    
    public List<String> getTechnologies() {
        return technologies;
    }
    
    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public Double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }
    
    public Integer getResolutionTimeMinutes() {
        return resolutionTimeMinutes;
    }
    
    public void setResolutionTimeMinutes(Integer resolutionTimeMinutes) {
        this.resolutionTimeMinutes = resolutionTimeMinutes;
    }
    
    public List<String> getPrerequisites() {
        return prerequisites;
    }
    
    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }
    
    public List<String> getVerificationSteps() {
        return verificationSteps;
    }
    
    public void setVerificationSteps(List<String> verificationSteps) {
        this.verificationSteps = verificationSteps;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
    
    // Utility methods
    
    /**
     * Increments usage count when this solution is applied
     */
    public void incrementUsage() {
        int currentCount = (this.usageCount != null) ? this.usageCount : 0;
        this.usageCount = currentCount + 1;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Gets combined text for vector embedding (symptoms + root cause + solution)
     */
    public String getEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symptoms: ").append(symptoms);
        sb.append(" Root Cause: ").append(rootCause);
        sb.append(" Solution: ").append(solution);
        return sb.toString();
    }
    
    /**
     * Gets search text combining title and symptoms for similarity matching
     */
    public String getSearchText() {
        return title + " " + symptoms;
    }
    
    @Override
    public String toString() {
        return "KnowledgeEntry{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", patternType='" + patternType + '\'' +
                ", severity='" + severity + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", usageCount=" + usageCount +
                '}';
    }
}
