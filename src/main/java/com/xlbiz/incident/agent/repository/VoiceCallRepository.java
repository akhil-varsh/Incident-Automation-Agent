package com.xlbiz.incident.agent.repository;

import com.xlbiz.incident.agent.model.VoiceCall;
import com.xlbiz.incident.agent.model.VoiceProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for VoiceCall entity operations
 */
@Repository
public interface VoiceCallRepository extends JpaRepository<VoiceCall, Long> {

    /**
     * Find voice call by conversation UUID
     */
    Optional<VoiceCall> findByConversationUuid(String conversationUuid);

    /**
     * Find voice calls by caller number
     */
    List<VoiceCall> findByCallerNumberOrderByCreatedAtDesc(String callerNumber);

    /**
     * Find voice calls by processing status
     */
    List<VoiceCall> findByProcessingStatusOrderByCreatedAtDesc(VoiceProcessingStatus status);

    /**
     * Find voice calls by incident ID
     */
    List<VoiceCall> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    /**
     * Find voice calls created within a date range
     */
    @Query("SELECT v FROM VoiceCall v WHERE v.createdAt BETWEEN :startDate AND :endDate ORDER BY v.createdAt DESC")
    List<VoiceCall> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find voice calls with pagination
     */
    Page<VoiceCall> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find voice calls by processing status with pagination
     */
    Page<VoiceCall> findByProcessingStatusOrderByCreatedAtDesc(VoiceProcessingStatus status, Pageable pageable);

    /**
     * Count voice calls by processing status
     */
    long countByProcessingStatus(VoiceProcessingStatus status);

    /**
     * Count voice calls created today
     */
    @Query("SELECT COUNT(v) FROM VoiceCall v WHERE v.createdAt >= :startOfDay AND v.createdAt < :endOfDay")
    long countTodaysCalls(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Find voice calls that failed processing
     */
    @Query("SELECT v FROM VoiceCall v WHERE v.processingStatus = 'ERROR' ORDER BY v.createdAt DESC")
    List<VoiceCall> findFailedCalls();

    /**
     * Find voice calls without transcription
     */
    @Query("SELECT v FROM VoiceCall v WHERE v.transcription IS NULL OR v.transcription = '' ORDER BY v.createdAt DESC")
    List<VoiceCall> findCallsWithoutTranscription();

    /**
     * Find recent voice calls (last 24 hours)
     */
    @Query("SELECT v FROM VoiceCall v WHERE v.createdAt >= :since ORDER BY v.createdAt DESC")
    List<VoiceCall> findRecentCalls(@Param("since") LocalDateTime since);

    /**
     * Get voice call statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as totalCalls,
            COUNT(CASE WHEN v.processingStatus = 'PROCESSED' THEN 1 END) as processedCalls,
            COUNT(CASE WHEN v.processingStatus = 'ERROR' THEN 1 END) as errorCalls,
            AVG(v.callDuration) as avgDuration
        FROM VoiceCall v 
        WHERE v.createdAt >= :since
        """)
    Object[] getCallStatistics(@Param("since") LocalDateTime since);

    /**
     * Check if conversation UUID exists
     */
    boolean existsByConversationUuid(String conversationUuid);

    /**
     * Find voice call by Twilio Call SID (for outbound calls)
     */
    Optional<VoiceCall> findByCallSid(String callSid);
}