package com.xlbiz.incident.agent.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.IncidentSeverity;
import com.xlbiz.incident.agent.model.IncidentStatus;
import com.xlbiz.incident.agent.model.IncidentType;

/**
 * Repository interface for Incident entity operations.
 * Provides custom queries for incident filtering, statistics, and analytics.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Find incident by external ID (from source system)
     */
    Optional<Incident> findByExternalId(String externalId);

    /**
     * Check if incident exists by external ID
     */
    boolean existsByExternalId(String externalId);

    /**
     * Find incidents by type with pagination
     */
    Page<Incident> findByType(IncidentType type, Pageable pageable);

    /**
     * Find incidents by severity with pagination
     */
    Page<Incident> findBySeverity(IncidentSeverity severity, Pageable pageable);

    /**
     * Find incidents by status with pagination
     */
    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    /**
     * Find incidents by source system with pagination
     */
    Page<Incident> findBySource(String source, Pageable pageable);

    /**
     * Find incidents created within a date range
     */
    Page<Incident> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find active incidents (not resolved, closed, or failed)
     */
    @Query("SELECT i FROM Incident i WHERE i.status NOT IN ('RESOLVED', 'CLOSED', 'FAILED') ORDER BY i.createdAt DESC")
    List<Incident> findActiveIncidents();

    /**
     * Find recent incidents (last 24 hours)
     */
    @Query("SELECT i FROM Incident i WHERE i.createdAt >= :since ORDER BY i.createdAt DESC")
    List<Incident> findRecentIncidents(@Param("since") LocalDateTime since);

    /**
     * Find incidents by multiple criteria with pagination
     */
       @Query("SELECT i FROM Incident i WHERE " +
                 "(:type IS NULL OR i.type = :type) AND " +
                 "(:severity IS NULL OR i.severity = :severity) AND " +
                 "(:status IS NULL OR i.status = :status) AND " +
                 "(:source IS NULL OR i.source = :source) " +
                 "ORDER BY i.createdAt DESC")
       Page<Incident> findByCriteria(
              @Param("type") IncidentType type,
              @Param("severity") IncidentSeverity severity,
              @Param("status") IncidentStatus status,
              @Param("source") String source,
              Pageable pageable
       );

    /**
     * Count incidents by status
     */
    @Query("SELECT i.status, COUNT(i) FROM Incident i GROUP BY i.status")
    List<Object[]> countByStatus();

    /**
     * Count incidents by severity
     */
    @Query("SELECT i.severity, COUNT(i) FROM Incident i GROUP BY i.severity")
    List<Object[]> countBySeverity();

    /**
     * Count incidents by type
     */
    @Query("SELECT i.type, COUNT(i) FROM Incident i GROUP BY i.type ORDER BY COUNT(i) DESC")
    List<Object[]> countByType();

    /**
     * Count incidents by source system
     */
    @Query("SELECT i.source, COUNT(i) FROM Incident i GROUP BY i.source ORDER BY COUNT(i) DESC")
    List<Object[]> countBySource();

    /**
     * Get incident statistics for dashboard
     */
    @Query("SELECT " +
           "COUNT(i) as total, " +
           "COUNT(CASE WHEN i.status IN ('RECEIVED', 'CLASSIFYING', 'PROCESSING', 'IN_PROGRESS') THEN 1 END) as active, " +
           "COUNT(CASE WHEN i.severity = 'HIGH' THEN 1 END) as high_severity, " +
           "COUNT(CASE WHEN i.status = 'RESOLVED' THEN 1 END) as resolved, " +
           "COUNT(CASE WHEN i.createdAt >= :since THEN 1 END) as recent " +
           "FROM Incident i")
    Object[] getIncidentStatistics(@Param("since") LocalDateTime since);

    /**
     * Find incidents with AI processing completed
     */
    @Query("SELECT i FROM Incident i WHERE i.aiSuggestion IS NOT NULL AND i.aiSuggestion != '' ORDER BY i.createdAt DESC")
    List<Incident> findIncidentsWithAiProcessing();

    /**
     * Find incidents with Slack integration
     */
    @Query("SELECT i FROM Incident i WHERE i.slackChannelId IS NOT NULL AND i.slackChannelId != '' ORDER BY i.createdAt DESC")
    List<Incident> findIncidentsWithSlackIntegration();

    /**
     * Find incidents with Jira integration
     */
    @Query("SELECT i FROM Incident i WHERE i.jiraTicketKey IS NOT NULL AND i.jiraTicketKey != '' ORDER BY i.createdAt DESC")
    List<Incident> findIncidentsWithJiraIntegration();

    /**
     * Find incidents by metadata field value
     */
    @Query(value = "SELECT * FROM incidents WHERE metadata ->> :key = :value ORDER BY created_at DESC", 
           nativeQuery = true)
    List<Incident> findByMetadataField(@Param("key") String key, @Param("value") String value);

    /**
     * Find incidents affecting specific service
     */
    @Query(value = "SELECT * FROM incidents WHERE metadata ->> 'service' = :service ORDER BY created_at DESC", 
           nativeQuery = true)
    List<Incident> findByService(@Param("service") String service);

    /**
     * Find incidents in specific environment
     */
    @Query(value = "SELECT * FROM incidents WHERE metadata ->> 'environment' = :environment ORDER BY created_at DESC", 
           nativeQuery = true)
    List<Incident> findByEnvironment(@Param("environment") String environment);

    /**
     * Get average resolution time for resolved incidents
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at))) FROM incidents WHERE resolved_at IS NOT NULL", 
           nativeQuery = true)
    Double getAverageResolutionTimeInSeconds();

    /**
     * Find similar incidents by type and description keywords
     */
    @Query("SELECT i FROM Incident i WHERE i.type = :type AND " +
           "(LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.aiSuggestion) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY i.createdAt DESC")
    List<Incident> findSimilarIncidents(@Param("type") IncidentType type, @Param("keyword") String keyword);

    /**
     * Find incidents requiring attention (high severity or long processing time)
     */
    @Query("SELECT i FROM Incident i WHERE " +
           "(i.severity = 'HIGH' AND i.status IN ('RECEIVED', 'CLASSIFYING', 'PROCESSING', 'IN_PROGRESS')) OR " +
           "(i.status IN ('RECEIVED', 'CLASSIFYING', 'PROCESSING') AND i.createdAt < :threshold) " +
           "ORDER BY i.severity DESC, i.createdAt ASC")
    List<Incident> findIncidentsRequiringAttention(@Param("threshold") LocalDateTime threshold);

    /**
     * Get incident trends over time (daily counts for the last N days)
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
                   "FROM incidents " +
                   "WHERE created_at >= :startDate " +
                   "GROUP BY DATE(created_at) " +
                   "ORDER BY date DESC", 
           nativeQuery = true)
    List<Object[]> getIncidentTrends(@Param("startDate") LocalDateTime startDate);

    /**
     * Delete old resolved incidents (for cleanup)
     */
    @Query("DELETE FROM Incident i WHERE i.status IN ('RESOLVED', 'CLOSED') AND i.resolvedAt < :cutoffDate")
    void deleteOldResolvedIncidents(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count incidents created today
     */
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.createdAt >= :startOfDay AND i.createdAt < :endOfDay")
    long countTodaysIncidents(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Search incidents by description or external ID
     */
    List<Incident> findByDescriptionContainingIgnoreCaseOrExternalIdContainingIgnoreCase(
        String description, String externalId, Pageable pageable);
}