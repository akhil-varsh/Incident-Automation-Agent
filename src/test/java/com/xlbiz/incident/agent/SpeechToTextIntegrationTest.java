package com.xlbiz.incident.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * Integration test for speech-to-text functionality using Spring Boot Test
 * This will work with Maven and all dependencies
 */
@SpringBootTest
@TestPropertySource(properties = {
    "integrations.twilio.account-sid=AC08854d517d4c0ba1775cec4e96b47fa0",
    "integrations.twilio.auth-token=5db62c4294b8f20b94f8357d913b26fd",
    "integrations.twilio.phone-number=+18723501845",
    "integrations.twilio.webhook-base-url=https://8159504a71bc.ngrok-free.app",
    "integrations.twilio.enabled=true",
    "google.cloud.project-id=akhil-stt",
    "google.application.credentials=/home/akhil/.config/gcloud/application_default_credentials.json",
    "google.speech.language-code=en-US",
    "google.speech.model=latest_long",
    "app.voice.speech-to-text.enabled=true",
    "app.voice.speech-to-text.service=google"
})
public class SpeechToTextIntegrationTest {
    
    private static final String ACCOUNT_SID = "AC08854d517d4c0ba1775cec4e96b47fa0";
    private static final String AUTH_TOKEN = "5db62c4294b8f20b94f8357d913b26fd";
    private static final String RECORDING_SID = "REed5dea99b50be6b30ec9836e7af1f281";
    
    @Test
    public void testRecordingDownloadAndSpeechToText() {
        System.out.println("=== Speech-to-Text Integration Test ===");
        
        try {
            // Step 1: Download recording
            System.out.println("Step 1: Downloading recording...");
            byte[] audioData = downloadRecording();
            
            if (audioData != null && audioData.length > 0) {
                System.out.println("✅ Recording downloaded: " + audioData.length + " bytes");
                
                // Save for verification
                saveAudioFile(audioData, "integration_test_" + RECORDING_SID + ".wav");
                
                // Step 2: Test Google Cloud Speech-to-Text
                System.out.println("\nStep 2: Testing Google Cloud Speech-to-Text...");
                testGoogleSpeechToText(audioData);
                
            } else {
                System.out.println("❌ Failed to download recording");
            }
            
        } catch (Exception e) {
            System.err.println("Integration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private byte[] downloadRecording() throws Exception {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + 
                    "/Recordings/" + RECORDING_SID + ".wav";
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        
        String credentials = ACCOUNT_SID + ":" + AUTH_TOKEN;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
        
        int responseCode = connection.getResponseCode();
        System.out.println("Download Response Code: " + responseCode);
        
        if (responseCode == 200) {
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                return outputStream.toByteArray();
            }
        } else {
            throw new RuntimeException("Download failed with code: " + responseCode);
        }
    }
    
    private void saveAudioFile(byte[] audioData, String fileName) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(audioData);
            System.out.println("Audio saved as: " + fileName);
        }
    }
    
    private void testGoogleSpeechToText(byte[] audioData) {
        try {
            System.out.println("Setting up Google Cloud Speech client...");
            System.out.println("Using Application Default Credentials from: /home/akhil/.config/gcloud/application_default_credentials.json");
            
            // Initialize Google Cloud Speech client with Application Default Credentials
            com.google.cloud.speech.v1.SpeechClient speechClient = 
                com.google.cloud.speech.v1.SpeechClient.create();
            
            try {
                // Convert audio data
                com.google.protobuf.ByteString audioBytes = 
                    com.google.protobuf.ByteString.copyFrom(audioData);
                
                // Build audio object
                com.google.cloud.speech.v1.RecognitionAudio audio = 
                    com.google.cloud.speech.v1.RecognitionAudio.newBuilder()
                        .setContent(audioBytes)
                        .build();
                
                // Configure recognition
                com.google.cloud.speech.v1.RecognitionConfig config = 
                    com.google.cloud.speech.v1.RecognitionConfig.newBuilder()
                        .setEncoding(com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(8000) // Twilio phone calls
                        .setLanguageCode("en-US")
                        .setModel("latest_long")
                        .setEnableAutomaticPunctuation(true)
                        .setUseEnhanced(true)
                        .build();
                
                System.out.println("Sending request to Google Cloud Speech API...");
                System.out.println("Audio size: " + audioData.length + " bytes");
                
                // Perform transcription
                com.google.cloud.speech.v1.RecognizeResponse response = 
                    speechClient.recognize(config, audio);
                
                System.out.println("Received response with " + response.getResultsCount() + " results");
                
                // Extract transcript
                StringBuilder transcript = new StringBuilder();
                for (int i = 0; i < response.getResultsCount(); i++) {
                    com.google.cloud.speech.v1.SpeechRecognitionResult result = response.getResults(i);
                    
                    if (result.getAlternativesCount() > 0) {
                        com.google.cloud.speech.v1.SpeechRecognitionAlternative alternative = 
                            result.getAlternatives(0);
                        String text = alternative.getTranscript();
                        float confidence = alternative.getConfidence();
                        
                        System.out.println("Result " + (i + 1) + ": \"" + text + "\" (confidence: " + confidence + ")");
                        
                        if (transcript.length() > 0) {
                            transcript.append(" ");
                        }
                        transcript.append(text);
                    }
                }
                
                String finalTranscript = transcript.toString().trim();
                
                if (!finalTranscript.isEmpty()) {
                    System.out.println("\n=== FINAL TRANSCRIPTION ===");
                    System.out.println("\"" + finalTranscript + "\"");
                    System.out.println("Length: " + finalTranscript.length() + " characters");
                    System.out.println("===========================");
                    System.out.println("✅ Speech-to-text successful!");
                } else {
                    System.out.println("⚠️  No transcription result - audio may be silent or unclear");
                }
                
            } finally {
                speechClient.close();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Google Cloud Speech-to-Text failed: " + e.getMessage());
            
            if (e.getMessage().contains("credentials")) {
                System.err.println("💡 Check Google Cloud credentials configuration");
            } else if (e.getMessage().contains("quota") || e.getMessage().contains("billing")) {
                System.err.println("💡 Check Google Cloud billing and API quotas");
            }
            
            e.printStackTrace();
        }
    }
}