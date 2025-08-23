package com.xlbiz.incident.agent.controller;

import com.xlbiz.incident.agent.model.Incident;
import com.xlbiz.incident.agent.model.VoiceCall;
import com.xlbiz.incident.agent.repository.IncidentRepository;
import com.xlbiz.incident.agent.repository.VoiceCallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for database table viewing interface
 */
@Controller
@RequestMapping("/admin/database")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class DatabaseViewController {

    private final IncidentRepository incidentRepository;
    private final VoiceCallRepository voiceCallRepository;

    @Autowired
    public DatabaseViewController(IncidentRepository incidentRepository, 
                                VoiceCallRepository voiceCallRepository) {
        this.incidentRepository = incidentRepository;
        this.voiceCallRepository = voiceCallRepository;
    }

    /**
     * Main database viewer page - redirects to React frontend
     */
    @GetMapping
    public String databaseViewer() {
        // Redirect to the React frontend
        return "redirect:/";
    }

    /**
     * Get incidents with pagination
     */
    @GetMapping("/api/incidents")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Incident> incidents = incidentRepository.findAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", incidents.getContent());
        response.put("totalElements", incidents.getTotalElements());
        response.put("totalPages", incidents.getTotalPages());
        response.put("currentPage", incidents.getNumber());
        response.put("size", incidents.getSize());
        response.put("hasNext", incidents.hasNext());
        response.put("hasPrevious", incidents.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * Get voice calls with pagination
     */
    @GetMapping("/api/voice-calls")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVoiceCalls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<VoiceCall> voiceCalls = voiceCallRepository.findAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", voiceCalls.getContent());
        response.put("totalElements", voiceCalls.getTotalElements());
        response.put("totalPages", voiceCalls.getTotalPages());
        response.put("currentPage", voiceCalls.getNumber());
        response.put("size", voiceCalls.getSize());
        response.put("hasNext", voiceCalls.hasNext());
        response.put("hasPrevious", voiceCalls.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * Get incident by ID
     */
    @GetMapping("/api/incidents/{id}")
    @ResponseBody
    public ResponseEntity<Incident> getIncident(@PathVariable Long id) {
        Optional<Incident> incident = incidentRepository.findById(id);
        return incident.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get voice call by ID
     */
    @GetMapping("/api/voice-calls/{id}")
    @ResponseBody
    public ResponseEntity<VoiceCall> getVoiceCall(@PathVariable Long id) {
        Optional<VoiceCall> voiceCall = voiceCallRepository.findById(id);
        return voiceCall.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get database statistics
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Calculate today's date range
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // Incident statistics
        long totalIncidents = incidentRepository.count();
        long todaysIncidents = incidentRepository.countTodaysIncidents(startOfDay, endOfDay);
        
        // Voice call statistics
        long totalVoiceCalls = voiceCallRepository.count();
        long todaysVoiceCalls = voiceCallRepository.countTodaysCalls(startOfDay, endOfDay);
        
        // Recent activity (last 24 hours)
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Incident> recentIncidents = incidentRepository.findRecentIncidents(since);
        List<VoiceCall> recentVoiceCalls = voiceCallRepository.findRecentCalls(since);

        stats.put("incidents", Map.of(
            "total", totalIncidents,
            "today", todaysIncidents,
            "recent24h", recentIncidents.size()
        ));

        stats.put("voiceCalls", Map.of(
            "total", totalVoiceCalls,
            "today", todaysVoiceCalls,
            "recent24h", recentVoiceCalls.size()
        ));

        return ResponseEntity.ok(stats);
    }

    /**
     * Search incidents
     */
    @GetMapping("/api/incidents/search")
    @ResponseBody
    public ResponseEntity<List<Incident>> searchIncidents(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        
        // Simple search implementation - can be enhanced with full-text search
        List<Incident> results = incidentRepository.findByDescriptionContainingIgnoreCaseOrExternalIdContainingIgnoreCase(
            query, query, PageRequest.of(0, limit));
        
        return ResponseEntity.ok(results);
    }

    /**
     * Search voice calls
     */
    @GetMapping("/api/voice-calls/search")
    @ResponseBody
    public ResponseEntity<List<VoiceCall>> searchVoiceCalls(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        
        // Search by caller number or conversation UUID
        List<VoiceCall> results = voiceCallRepository.findByCallerNumberOrderByCreatedAtDesc(query);
        
        if (results.isEmpty()) {
            // Try searching by conversation UUID
            voiceCallRepository.findByConversationUuid(query)
                .ifPresent(results::add);
        }
        
        return ResponseEntity.ok(results.stream().limit(limit).toList());
    }
}