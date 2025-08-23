package com.xlbiz.incident.agent.service;

import com.xlbiz.incident.agent.config.TwilioConfig;
import com.xlbiz.incident.agent.dto.IncidentRequest;
import com.xlbiz.incident.agent.dto.VoiceIncidentResponse;
import com.xlbiz.incident.agent.model.IncidentType;
import com.xlbiz.incident.agent.model.VoiceCall;
import com.xlbiz.incident.agent.model.VoiceProcessingStatus;
import com.xlbiz.incident.agent.repository.VoiceCallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VoiceIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceIntegrationService.class);

    private final TwilioConfig twilioConfig;
    private final IncidentService incidentService;
    private final VoiceCallRepository voiceCallRepository;
    private final WebClient webClient;
    
    @Value("${app.voice.speech-to-text.enabled:true}")
    private boolean speechToTextEnabled;
    
    @Value("${app.voice.speech-to-text.service:google}")
    private String speechToTextService;
    
    @Value("${google.cloud.project-id:}")
    private String googleCloudProjectId;
    
    @Value("${google.cloud.credentials:}")
    private String googleCloudCredentials;
    
    @Value("${google.speech.language-code:en-US}")
    private String googleSpeechLanguageCode;
    
    @Value("${google.speech.model:latest_long}")
    private String googleSpeechModel;
    
    @Value("${deepgram.api-key:}")
    private String deepgramApiKey;
    
    @Value("${deepgram.api-url:https://api.deepgram.com/v1/listen}")
    private String deepgramApiUrl;

    @Autowired
    public VoiceIntegrationService(TwilioConfig twilioConfig, 
                                 IncidentService incidentService,
                                 VoiceCallRepository voiceCallRepository,
                                 WebClient.Builder webClientBuilder) {
        this.twilioConfig = twilioConfig;
        this.incidentService = incidentService;
        this.voiceCallRepository = voiceCallRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Generate TwiML for incoming calls (Twilio's XML format)
     */
    public String generateAnswerTwiML() {
        String recordingUrl = twilioConfig.getWebhookBaseUrl() + "/api/twilio/voice/recording";
        
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Say voice="alice">Hello, you have reached the XLBiz Incident Automation Hotline. Please describe your incident clearly after the beep. You have up to %d seconds.</Say>
                <Record action="%s" method="POST" timeout="%d" maxLength="%d" finishOnKey="*" />
                <Say voice="alice">Thank you. Your incident has been recorded and will be processed immediately. You will receive updates via Slack and Jira. Goodbye.</Say>
            </Response>
            """, 
            twilioConfig.getRecording().getMaxDurationSeconds(),
            recordingUrl,
            twilioConfig.getRecording().getTimeoutSeconds(),
            twilioConfig.getRecording().getMaxDurationSeconds()
        );
    }

    /**
     * Process voice recording and create incident
     */
    public VoiceIncidentResponse processVoiceRecording(String recordingUrl, String conversationUuid, String callerNumber) {
        logger.info("Processing voice recording from URL: {} for conversation: {}", recordingUrl, conversationUuid);
        
        // Check if this conversation has already been processed to prevent duplicates
        if (voiceCallRepository.existsByConversationUuid(conversationUuid)) {
            logger.warn("Conversation {} has already been processed, skipping duplicate", conversationUuid);
            return createDuplicateResponse(conversationUuid, callerNumber);
        }
        
        // Create voice call record
        VoiceCall voiceCall = new VoiceCall(conversationUuid, callerNumber, recordingUrl);
        voiceCall.setProcessingStatus(VoiceProcessingStatus.RECEIVED);
        voiceCall = voiceCallRepository.save(voiceCall);
        
        try {
            // Step 1: Update status to downloading
            voiceCall.setProcessingStatus(VoiceProcessingStatus.DOWNLOADING);
            voiceCallRepository.save(voiceCall);
            
            // Step 2: Download and transcribe the recording
            voiceCall.setProcessingStatus(VoiceProcessingStatus.TRANSCRIBING);
            voiceCallRepository.save(voiceCall);
            
            String transcription = transcribeRecording(recordingUrl);
            logger.info("Transcription completed: {}", transcription);
            
            // Update voice call with transcription
            voiceCall.setTranscription(transcription);
            voiceCall.setSpeechToTextService(speechToTextService);
            
            // Step 3: Extract incident details from transcription
            IncidentRequest incidentRequest = extractIncidentFromTranscription(transcription, callerNumber, conversationUuid);
            
            // Step 4: Process incident through existing workflow
            var incidentResponse = incidentService.processIncident(incidentRequest);
            
            // Step 5: Update the incident with voice-specific data
            var incident = incidentService.getIncidentById(incidentResponse.getIncidentId());
            if (incident.isPresent()) {
                var inc = incident.get();
                inc.setTranscription(transcription);
                inc.setRecordingUrl(recordingUrl);
                inc.setConversationUuid(conversationUuid);
                // Call duration will be set when available from Twilio webhook
                incidentService.updateIncident(inc);
                
                // Link voice call to incident
                voiceCall.setIncident(inc);
            }
            
            // Step 6: Update voice call with processing completion
            voiceCall.setProcessingStatus(VoiceProcessingStatus.PROCESSED);
            voiceCall.setProcessedAt(LocalDateTime.now());
            voiceCallRepository.save(voiceCall);
            
            // Step 6: Create voice-specific response
            VoiceIncidentResponse voiceResponse = new VoiceIncidentResponse();
            voiceResponse.setIncidentId(incidentResponse.getIncidentId().toString());
            voiceResponse.setCallUuid(conversationUuid);
            voiceResponse.setCallerNumber(callerNumber);
            voiceResponse.setTranscription(transcription);
            voiceResponse.setAiClassification(incidentResponse.getAiClassifiedSeverity().toString());
            voiceResponse.setProcessingStatus("PROCESSED");
            voiceResponse.setProcessedAt(LocalDateTime.now());
            voiceResponse.setMessage("Incident successfully created from voice call");
            
            logger.info("Voice incident processed successfully: {}", voiceResponse.getIncidentId());
            return voiceResponse;
            
        } catch (Exception e) {
            logger.error("Error processing voice recording: {}", e.getMessage(), e);
            
            // Update voice call with error
            voiceCall.setProcessingStatus(VoiceProcessingStatus.ERROR);
            voiceCall.setErrorMessage(e.getMessage());
            voiceCallRepository.save(voiceCall);
            
            VoiceIncidentResponse errorResponse = new VoiceIncidentResponse();
            errorResponse.setCallUuid(conversationUuid);
            errorResponse.setCallerNumber(callerNumber);
            errorResponse.setProcessingStatus("ERROR");
            errorResponse.setProcessedAt(LocalDateTime.now());
            errorResponse.setMessage("Failed to process voice incident: " + e.getMessage());
            
            return errorResponse;
        }
    }

    /**
     * Transcribe recording using actual speech-to-text service
     */
    private String transcribeRecording(String recordingUrl) {
        logger.info("Transcribing recording from URL: {}", recordingUrl);
        
        try {
            // Download the audio file from the recording URL
            byte[] audioData = downloadAudioFile(recordingUrl);
            
            if (audioData == null || audioData.length == 0) {
                throw new RuntimeException("Failed to download audio file or file is empty");
            }
            
            // Use speech-to-text service to transcribe
            String transcription = performSpeechToText(audioData);
            
            if (transcription == null || transcription.trim().isEmpty()) {
                throw new RuntimeException("Speech-to-text service returned empty transcription");
            }
            
            logger.info("Transcription completed successfully, length: {} characters", transcription.length());
            return transcription.trim();
            
        } catch (Exception e) {
            logger.error("Failed to transcribe recording from URL {}: {}", recordingUrl, e.getMessage(), e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download audio file from recording URL with Twilio authentication
     */
    private byte[] downloadAudioFile(String recordingUrl) {
        try {
            logger.info("Downloading audio file from: {}", recordingUrl);
            
            // First try with WebClient
            try {
                return downloadWithWebClient(recordingUrl);
            } catch (Exception webClientError) {
                logger.warn("WebClient download failed, trying alternative method: {}", webClientError.getMessage());
                return downloadWithRestTemplate(recordingUrl);
            }
                
        } catch (Exception e) {
            logger.error("All download methods failed for {}: {}", recordingUrl, e.getMessage(), e);
            throw new RuntimeException("Audio download failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download using WebClient (primary method)
     */
    private byte[] downloadWithWebClient(String recordingUrl) {
        // Twilio requires Basic Authentication with Account SID and Auth Token
        String credentials = twilioConfig.getAccountSid() + ":" + twilioConfig.getAuthToken();
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
        
        logger.debug("Using WebClient with Twilio credentials for authentication");
        
        byte[] audioData = webClient.get()
            .uri(recordingUrl)
            .header("Authorization", "Basic " + encodedCredentials)
            .header("Accept", "audio/*")
            .retrieve()
            .bodyToMono(byte[].class)
            .timeout(Duration.ofSeconds(30))
            .block();
        
        if (audioData == null || audioData.length == 0) {
            throw new RuntimeException("Received empty audio data from Twilio");
        }
        
        logger.info("WebClient: Successfully downloaded audio file, size: {} bytes", audioData.length);
        return audioData;
    }
    
    /**
     * Download using RestTemplate (fallback method)
     */
    private byte[] downloadWithRestTemplate(String recordingUrl) {
        try {
            logger.info("Attempting download with RestTemplate as fallback");
            
            // Create RestTemplate with authentication
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            // Set up Basic Authentication
            String credentials = twilioConfig.getAccountSid() + ":" + twilioConfig.getAuthToken();
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Basic " + encodedCredentials);
            headers.set("Accept", "audio/*");
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
                recordingUrl, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                byte[].class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("RestTemplate: Successfully downloaded audio file, size: {} bytes", response.getBody().length);
                return response.getBody();
            } else {
                throw new RuntimeException("RestTemplate failed: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("RestTemplate download failed: {}", e.getMessage());
            throw new RuntimeException("RestTemplate download failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform speech-to-text conversion using configured service
     */
    private String performSpeechToText(byte[] audioData) {
        if (!speechToTextEnabled) {
            throw new RuntimeException("Speech-to-text service is disabled. Enable it in configuration: app.voice.speech-to-text.enabled=true");
        }
        
        switch (speechToTextService.toLowerCase()) {
            case "google":
                return performGoogleSpeechToText(audioData);
            case "deepgram":
                return performDeepgramSpeechToText(audioData);
            default:
                throw new RuntimeException("Unsupported speech-to-text service: " + speechToTextService + 
                    ". Supported services: google, deepgram");
        }
    }
    
    /**
     * Google Cloud Speech-to-Text implementation
     */
    private String performGoogleSpeechToText(byte[] audioData) {
        try {
            logger.info("Sending audio to Google Cloud Speech-to-Text, size: {} bytes", audioData.length);
            
            logger.debug("Google Cloud configuration - Project ID: {}, Service: {}", googleCloudProjectId, speechToTextService);
            
            if (googleCloudProjectId == null || googleCloudProjectId.trim().isEmpty()) {
                logger.error("Google Cloud Project ID not configured. Current value: '{}'. Please set GOOGLE_CLOUD_PROJECT_ID environment variable.", googleCloudProjectId);
                throw new RuntimeException("Google Cloud Project ID not configured. Please set GOOGLE_CLOUD_PROJECT_ID environment variable to 'akhil-stt'");
            }
            
            // Initialize Google Cloud Speech client
            com.google.cloud.speech.v1.SpeechClient speechClient = com.google.cloud.speech.v1.SpeechClient.create();
            
            try {
                // Convert audio data to ByteString
                com.google.protobuf.ByteString audioBytes = com.google.protobuf.ByteString.copyFrom(audioData);
                
                // Build the audio object
                com.google.cloud.speech.v1.RecognitionAudio audio = com.google.cloud.speech.v1.RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();
                
                // Configure recognition settings
                com.google.cloud.speech.v1.RecognitionConfig config = com.google.cloud.speech.v1.RecognitionConfig.newBuilder()
                    .setEncoding(com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(8000) // Twilio uses 8kHz for phone calls
                    .setLanguageCode(googleSpeechLanguageCode)
                    .setModel(googleSpeechModel)
                    .setEnableAutomaticPunctuation(true)
                    .setUseEnhanced(true)
                    .build();
                
                // Perform the transcription
                com.google.cloud.speech.v1.RecognizeResponse response = speechClient.recognize(config, audio);
                
                // Extract transcript from response
                StringBuilder transcript = new StringBuilder();
                for (com.google.cloud.speech.v1.SpeechRecognitionResult result : response.getResultsList()) {
                    if (!result.getAlternativesList().isEmpty()) {
                        com.google.cloud.speech.v1.SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                        transcript.append(alternative.getTranscript()).append(" ");
                    }
                }
                
                String finalTranscript = transcript.toString().trim();
                
                if (finalTranscript.isEmpty()) {
                    throw new RuntimeException("Google Speech-to-Text returned empty transcript");
                }
                
                logger.info("Google Speech-to-Text transcription successful, transcript length: {} characters", finalTranscript.length());
                return finalTranscript;
                
            } finally {
                speechClient.close();
            }
            
        } catch (Exception e) {
            logger.error("Google Speech-to-Text transcription failed: {}", e.getMessage(), e);
            
            // Fallback to Deepgram if Google fails and Deepgram is configured
            if (deepgramApiKey != null && !deepgramApiKey.trim().isEmpty()) {
                logger.info("Falling back to Deepgram for transcription");
                return performDeepgramSpeechToText(audioData);
            }
            
            throw new RuntimeException("Google Speech-to-Text failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deepgram Speech-to-Text implementation (fallback)
     */
    private String performDeepgramSpeechToText(byte[] audioData) {
        try {
            logger.info("Sending audio to Deepgram for transcription, size: {} bytes", audioData.length);
            
            if (deepgramApiKey == null || deepgramApiKey.trim().isEmpty()) {
                throw new RuntimeException("Deepgram API key not configured. Please set deepgram.api-key");
            }
            
            // Deepgram API call
            String response = webClient.post()
                .uri(deepgramApiUrl + "?model=nova-2&smart_format=true&punctuate=true&diarize=false")
                .header("Authorization", "Token " + deepgramApiKey)
                .header("Content-Type", "audio/wav")
                .bodyValue(audioData)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("Deepgram returned empty response");
            }
            
            // Parse Deepgram response to extract transcript
            String transcript = parseDeepgramResponse(response);
            
            if (transcript == null || transcript.trim().isEmpty()) {
                throw new RuntimeException("No transcript found in Deepgram response");
            }
            
            logger.info("Deepgram transcription successful, transcript length: {} characters", transcript.length());
            return transcript;
            
        } catch (Exception e) {
            logger.error("Deepgram transcription failed: {}", e.getMessage(), e);
            throw new RuntimeException("Deepgram transcription failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse Deepgram JSON response to extract transcript
     */
    private String parseDeepgramResponse(String jsonResponse) {
        try {
            // Simple JSON parsing for Deepgram response
            Pattern transcriptPattern = Pattern.compile("\"transcript\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = transcriptPattern.matcher(jsonResponse);
            
            StringBuilder fullTranscript = new StringBuilder();
            while (matcher.find()) {
                String transcript = matcher.group(1);
                if (transcript != null && !transcript.trim().isEmpty()) {
                    if (fullTranscript.length() > 0) {
                        fullTranscript.append(" ");
                    }
                    fullTranscript.append(transcript.trim());
                }
            }
            
            String result = fullTranscript.toString().trim();
            logger.debug("Extracted transcript from Deepgram response: {}", result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to parse Deepgram response: {}", e.getMessage());
            logger.debug("Deepgram response was: {}", jsonResponse);
            throw new RuntimeException("Failed to parse Deepgram response: " + e.getMessage(), e);
        }
    }

    /**
     * Extract incident details from transcription using pattern matching and AI
     */
    private IncidentRequest extractIncidentFromTranscription(String transcription, String callerNumber, String conversationUuid) {
        logger.info("Extracting incident details from transcription");
        
        IncidentRequest request = new IncidentRequest();
        
        // Generate unique incident ID
        String incidentId = "voice-" + UUID.randomUUID().toString().substring(0, 8);
        request.setId(incidentId);
        
        // Set basic fields
        request.setDescription(transcription);
        request.setSource("voice-hotline");
        request.setTimestamp(LocalDateTime.now());
        
        // Extract incident type using keyword matching
        request.setType(extractIncidentType(transcription).name());
        
        // Add voice-specific metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("caller_number", callerNumber);
        metadata.put("conversation_uuid", conversationUuid);
        metadata.put("input_method", "voice");
        
        // Extract environment if mentioned
        String environment = extractEnvironment(transcription);
        if (environment != null) {
            metadata.put("environment", environment);
        }
        
        // Extract affected users count if mentioned
        Integer affectedUsers = extractAffectedUsers(transcription);
        if (affectedUsers != null) {
            metadata.put("affected_users", affectedUsers);
        }
        
        request.setMetadata(metadata);
        
        logger.info("Extracted incident request: ID={}, Type={}, Environment={}", 
                   incidentId, request.getType(), environment);
        
        return request;
    }

    /**
     * Extract incident type from transcription using keyword matching
     */
    private IncidentType extractIncidentType(String transcription) {
        String lowerText = transcription.toLowerCase();
        
        // Database-related keywords
        if (lowerText.contains("database") || lowerText.contains("db") || 
            lowerText.contains("connection") || lowerText.contains("sql")) {
            return IncidentType.DATABASE_CONNECTION_ERROR;
        }
        
        // Network-related keywords
        if (lowerText.contains("network") || lowerText.contains("connectivity") || 
            lowerText.contains("timeout") || lowerText.contains("unreachable")) {
            return IncidentType.NETWORK_ISSUE;
        }
        
        // Performance-related keywords (CPU)
        if (lowerText.contains("cpu") || lowerText.contains("processor") || 
            lowerText.contains("high cpu")) {
            return IncidentType.HIGH_CPU;
        }
        
        // Memory-related keywords
        if (lowerText.contains("memory") || lowerText.contains("ram") || 
            lowerText.contains("out of memory") || lowerText.contains("leak")) {
            return IncidentType.MEMORY_LEAK;
        }
        
        // Service/Application down keywords
        if (lowerText.contains("down") || lowerText.contains("outage") || 
            lowerText.contains("unavailable") || lowerText.contains("crash")) {
            return IncidentType.SERVICE_DOWN;
        }
        
        // API-related keywords
        if (lowerText.contains("api") || lowerText.contains("endpoint") || 
            lowerText.contains("rest") || lowerText.contains("web service")) {
            return IncidentType.API_FAILURE;
        }
        
        // Security-related keywords
        if (lowerText.contains("security") || lowerText.contains("breach") || 
            lowerText.contains("unauthorized") || lowerText.contains("hack")) {
            return IncidentType.SECURITY_BREACH;
        }
        
        // Disk-related keywords
        if (lowerText.contains("disk") || lowerText.contains("storage") || 
            lowerText.contains("space") || lowerText.contains("full")) {
            return IncidentType.DISK_FULL;
        }
        
        // Deployment-related keywords
        if (lowerText.contains("deployment") || lowerText.contains("deploy") || 
            lowerText.contains("release") || lowerText.contains("build")) {
            return IncidentType.DEPLOYMENT_FAILURE;
        }
        
        // Data-related keywords
        if (lowerText.contains("data corruption") || lowerText.contains("corrupt") || 
            lowerText.contains("data loss") || lowerText.contains("integrity")) {
            return IncidentType.DATA_CORRUPTION;
        }
        
        // Default to other
        return IncidentType.OTHER;
    }

    /**
     * Extract environment from transcription
     */
    private String extractEnvironment(String transcription) {
        String lowerText = transcription.toLowerCase();
        
        if (lowerText.contains("production") || lowerText.contains("prod")) {
            return "production";
        } else if (lowerText.contains("staging") || lowerText.contains("stage")) {
            return "staging";
        } else if (lowerText.contains("development") || lowerText.contains("dev")) {
            return "development";
        } else if (lowerText.contains("test") || lowerText.contains("testing")) {
            return "test";
        }
        
        return null; // Don't assume environment if not explicitly mentioned
    }

    /**
     * Extract affected users count from transcription
     */
    private Integer extractAffectedUsers(String transcription) {
        // Look for explicit numbers like "100 users", "50 customers", etc.
        Pattern numberPattern = Pattern.compile("(\\d+)\\s*(users?|customers?|people)");
        Matcher matcher = numberPattern.matcher(transcription.toLowerCase());
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        // Look for scale indicators like "thousands", "hundreds"
        String lowerText = transcription.toLowerCase();
        if (lowerText.contains("thousands")) {
            return 1000;
        } else if (lowerText.contains("hundreds")) {
            return 100;
        } else if (lowerText.contains("dozens")) {
            return 12;
        }
        
        return null; // Unable to determine specific number
    }

    /**
     * Create response for duplicate processing attempt
     */
    private VoiceIncidentResponse createDuplicateResponse(String conversationUuid, String callerNumber) {
        VoiceIncidentResponse response = new VoiceIncidentResponse();
        response.setCallUuid(conversationUuid);
        response.setCallerNumber(callerNumber);
        response.setProcessingStatus("DUPLICATE");
        response.setProcessedAt(LocalDateTime.now());
        response.setMessage("This conversation has already been processed recently");
        return response;
    }

    /**
     * Check if voice integration is fully enabled and configured
     */
    public boolean isEnabled() {
        return twilioConfig.isEnabled() && speechToTextEnabled && isSpeechServiceConfigured();
    }
    
    /**
     * Check if speech-to-text service is properly configured
     */
    private boolean isSpeechServiceConfigured() {
        switch (speechToTextService.toLowerCase()) {
            case "google":
                return googleCloudProjectId != null && !googleCloudProjectId.trim().isEmpty();
            case "deepgram":
                return deepgramApiKey != null && !deepgramApiKey.trim().isEmpty();
            default:
                return false;
        }
    }
    
    /**
     * Check if Twilio integration is enabled (without speech-to-text requirement)
     */
    public boolean isTwilioEnabled() {
        return twilioConfig.isEnabled();
    }
    
    /**
     * Get configuration status for debugging
     */
    public Map<String, Object> getConfigurationStatus() {
        return Map.of(
            "twilio_enabled", twilioConfig.isEnabled(),
            "speech_to_text_enabled", speechToTextEnabled,
            "speech_to_text_service", speechToTextService,
            "google_cloud_project_configured", googleCloudProjectId != null && !googleCloudProjectId.trim().isEmpty(),
            "deepgram_api_key_configured", deepgramApiKey != null && !deepgramApiKey.trim().isEmpty(),
            "speech_service_configured", isSpeechServiceConfigured(),
            "fully_enabled", isEnabled()
        );
    }
}