package com.xlbiz.incident.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for incident statistics and dashboard metrics.
 * Provides aggregated data for monitoring and reporting purposes.
 */
public class IncidentStats {

    @JsonProperty("summary")
    private Summary summary;

    @JsonProperty("by_status")
    private Map<String, Long> byStatus = new HashMap<>();

    @JsonProperty("by_severity")
    private Map<String, Long> bySeverity = new HashMap<>();

    @JsonProperty("by_type")
    private Map<String, Long> byType = new HashMap<>();

    @JsonProperty("by_source")
    private Map<String, Long> bySource = new HashMap<>();

    @JsonProperty("trends")
    private List<TrendData> trends;

    @JsonProperty("performance")
    private Performance performance;

    // Constructors

    public IncidentStats() {
        this.summary = new Summary();
        this.performance = new Performance();
    }

    // Getters and Setters

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }

    public void setByStatus(Map<String, Long> byStatus) {
        this.byStatus = byStatus;
    }

    public Map<String, Long> getBySeverity() {
        return bySeverity;
    }

    public void setBySeverity(Map<String, Long> bySeverity) {
        this.bySeverity = bySeverity;
    }

    public Map<String, Long> getByType() {
        return byType;
    }

    public void setByType(Map<String, Long> byType) {
        this.byType = byType;
    }

    public Map<String, Long> getBySource() {
        return bySource;
    }

    public void setBySource(Map<String, Long> bySource) {
        this.bySource = bySource;
    }

    public List<TrendData> getTrends() {
        return trends;
    }

    public void setTrends(List<TrendData> trends) {
        this.trends = trends;
    }

    public Performance getPerformance() {
        return performance;
    }

    public void setPerformance(Performance performance) {
        this.performance = performance;
    }

    /**
     * Summary statistics
     */
    public static class Summary {
        
        @JsonProperty("total_incidents")
        private long totalIncidents;
        
        @JsonProperty("active_incidents")
        private long activeIncidents;
        
        @JsonProperty("high_severity_incidents")
        private long highSeverityIncidents;
        
        @JsonProperty("resolved_incidents")
        private long resolvedIncidents;
        
        @JsonProperty("recent_incidents_24h")
        private long recentIncidents24h;
        
        @JsonProperty("incidents_with_ai_processing")
        private long incidentsWithAiProcessing;
        
        @JsonProperty("incidents_with_slack_integration")
        private long incidentsWithSlackIntegration;
        
        @JsonProperty("incidents_with_jira_integration")
        private long incidentsWithJiraIntegration;

        // Getters and Setters
        public long getTotalIncidents() {
            return totalIncidents;
        }

        public void setTotalIncidents(long totalIncidents) {
            this.totalIncidents = totalIncidents;
        }

        public long getActiveIncidents() {
            return activeIncidents;
        }

        public void setActiveIncidents(long activeIncidents) {
            this.activeIncidents = activeIncidents;
        }

        public long getHighSeverityIncidents() {
            return highSeverityIncidents;
        }

        public void setHighSeverityIncidents(long highSeverityIncidents) {
            this.highSeverityIncidents = highSeverityIncidents;
        }

        public long getResolvedIncidents() {
            return resolvedIncidents;
        }

        public void setResolvedIncidents(long resolvedIncidents) {
            this.resolvedIncidents = resolvedIncidents;
        }

        public long getRecentIncidents24h() {
            return recentIncidents24h;
        }

        public void setRecentIncidents24h(long recentIncidents24h) {
            this.recentIncidents24h = recentIncidents24h;
        }

        public long getIncidentsWithAiProcessing() {
            return incidentsWithAiProcessing;
        }

        public void setIncidentsWithAiProcessing(long incidentsWithAiProcessing) {
            this.incidentsWithAiProcessing = incidentsWithAiProcessing;
        }

        public long getIncidentsWithSlackIntegration() {
            return incidentsWithSlackIntegration;
        }

        public void setIncidentsWithSlackIntegration(long incidentsWithSlackIntegration) {
            this.incidentsWithSlackIntegration = incidentsWithSlackIntegration;
        }

        public long getIncidentsWithJiraIntegration() {
            return incidentsWithJiraIntegration;
        }

        public void setIncidentsWithJiraIntegration(long incidentsWithJiraIntegration) {
            this.incidentsWithJiraIntegration = incidentsWithJiraIntegration;
        }
    }

    /**
     * Trend data for time-series analysis
     */
    public static class TrendData {
        
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("count")
        private long count;

        public TrendData() {
        }

        public TrendData(String date, long count) {
            this.date = date;
            this.count = count;
        }

        // Getters and Setters
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    /**
     * Performance metrics
     */
    public static class Performance {
        
        @JsonProperty("average_resolution_time_seconds")
        private Double averageResolutionTimeSeconds;
        
        @JsonProperty("average_resolution_time_human")
        private String averageResolutionTimeHuman;
        
        @JsonProperty("ai_processing_success_rate")
        private Double aiProcessingSuccessRate;
        
        @JsonProperty("slack_integration_success_rate")
        private Double slackIntegrationSuccessRate;
        
        @JsonProperty("jira_integration_success_rate")
        private Double jiraIntegrationSuccessRate;

        // Getters and Setters
        public Double getAverageResolutionTimeSeconds() {
            return averageResolutionTimeSeconds;
        }

        public void setAverageResolutionTimeSeconds(Double averageResolutionTimeSeconds) {
            this.averageResolutionTimeSeconds = averageResolutionTimeSeconds;
            // Convert to human-readable format
            if (averageResolutionTimeSeconds != null) {
                this.averageResolutionTimeHuman = formatDuration(averageResolutionTimeSeconds);
            }
        }

        public String getAverageResolutionTimeHuman() {
            return averageResolutionTimeHuman;
        }

        public void setAverageResolutionTimeHuman(String averageResolutionTimeHuman) {
            this.averageResolutionTimeHuman = averageResolutionTimeHuman;
        }

        public Double getAiProcessingSuccessRate() {
            return aiProcessingSuccessRate;
        }

        public void setAiProcessingSuccessRate(Double aiProcessingSuccessRate) {
            this.aiProcessingSuccessRate = aiProcessingSuccessRate;
        }

        public Double getSlackIntegrationSuccessRate() {
            return slackIntegrationSuccessRate;
        }

        public void setSlackIntegrationSuccessRate(Double slackIntegrationSuccessRate) {
            this.slackIntegrationSuccessRate = slackIntegrationSuccessRate;
        }

        public Double getJiraIntegrationSuccessRate() {
            return jiraIntegrationSuccessRate;
        }

        public void setJiraIntegrationSuccessRate(Double jiraIntegrationSuccessRate) {
            this.jiraIntegrationSuccessRate = jiraIntegrationSuccessRate;
        }

        /**
         * Format duration in seconds to human-readable format
         */
        private String formatDuration(double seconds) {
            if (seconds < 60) {
                return String.format("%.1f seconds", seconds);
            } else if (seconds < 3600) {
                return String.format("%.1f minutes", seconds / 60);
            } else if (seconds < 86400) {
                return String.format("%.1f hours", seconds / 3600);
            } else {
                return String.format("%.1f days", seconds / 86400);
            }
        }
    }
}