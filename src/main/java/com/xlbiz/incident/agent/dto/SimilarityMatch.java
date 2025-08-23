package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xlbiz.incident.agent.model.KnowledgeEntry;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * DTO representing a knowledge entry match with similarity score.
 * Used for vector-based similarity search results from ChromaDB.
 */
public class SimilarityMatch {
    
    @NotNull
    @JsonProperty("knowledge_entry")
    private KnowledgeEntry knowledgeEntry;
    
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @JsonProperty("similarity_score")
    private Double similarityScore;
    
    @JsonProperty("distance")
    private Double distance; // Vector distance (lower is more similar)
    
    @JsonProperty("rank")
    private Integer rank; // Position in the result set
    
    @JsonProperty("relevance_reason")
    private String relevanceReason; // Why this match is relevant
    
    // Constructors
    public SimilarityMatch() {
    }
    
    public SimilarityMatch(KnowledgeEntry knowledgeEntry, Double similarityScore) {
        this.knowledgeEntry = knowledgeEntry;
        this.similarityScore = similarityScore;
    }
    
    public SimilarityMatch(KnowledgeEntry knowledgeEntry, Double similarityScore, Double distance) {
        this.knowledgeEntry = knowledgeEntry;
        this.similarityScore = similarityScore;
        this.distance = distance;
    }
    
    // Getters and Setters
    public KnowledgeEntry getKnowledgeEntry() {
        return knowledgeEntry;
    }
    
    public void setKnowledgeEntry(KnowledgeEntry knowledgeEntry) {
        this.knowledgeEntry = knowledgeEntry;
    }
    
    public Double getSimilarityScore() {
        return similarityScore;
    }
    
    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }
    
    public Double getDistance() {
        return distance;
    }
    
    public void setDistance(Double distance) {
        this.distance = distance;
    }
    
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
    public String getRelevanceReason() {
        return relevanceReason;
    }
    
    public void setRelevanceReason(String relevanceReason) {
        this.relevanceReason = relevanceReason;
    }
    
    // Utility methods
    
    /**
     * Checks if this is a high-confidence match (similarity > 0.8)
     */
    public boolean isHighConfidence() {
        return similarityScore != null && similarityScore > 0.8;
    }
    
    /**
     * Checks if this is a medium-confidence match (similarity 0.6-0.8)
     */
    public boolean isMediumConfidence() {
        return similarityScore != null && similarityScore >= 0.6 && similarityScore <= 0.8;
    }
    
    /**
     * Gets the confidence level as a string
     */
    public String getConfidenceLevel() {
        if (similarityScore == null) return "UNKNOWN";
        if (similarityScore > 0.8) return "HIGH";
        if (similarityScore >= 0.6) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Gets formatted similarity score as percentage
     */
    public String getFormattedSimilarity() {
        if (similarityScore == null) return "N/A";
        return String.format("%.1f%%", similarityScore * 100);
    }
    
    @Override
    public String toString() {
        return "SimilarityMatch{" +
                "title='" + (knowledgeEntry != null ? knowledgeEntry.getTitle() : "null") + '\'' +
                ", similarityScore=" + getFormattedSimilarity() +
                ", rank=" + rank +
                ", confidence=" + getConfidenceLevel() +
                '}';
    }
}
