package com.xlbiz.incident.agent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Complete test script to download Twilio recording and perform speech-to-text
 * This tests the full workflow: Download → Speech-to-Text → Result
 */
public class TwilioSpeechToTextTest {
    
    // Twilio Configuration
    private static final String ACCOUNT_SID = "AC08854d517d4c0ba1775cec4e96b47fa0";
    private static final String AUTH_TOKEN = "5db62c4294b8f20b94f8357d913b26fd";
    private static final String RECORDING_SID = "REed5dea99b50be6b30ec9836e7af1f281"; // 12 seconds recording
    
    // Google Cloud Configuration
    private static final String GOOGLE_PROJECT_ID = "akhil-stt";
    private static final String GOOGLE_CREDENTIALS_PATH = "/home/akhil/.config/gcloud/application_default_credentials.json";
    private static final String LANGUAGE_CODE = "en-US";
    private static final String SPEECH_MODEL = "latest_long";
    
    public static void main(String[] args) {
        System.out.println("=== Twilio Recording Download + Speech-to-Text Test ===");
        System.out.println("Recording SID: " + RECORDING_SID);
        System.out.println("Google Project: " + GOOGLE_PROJECT_ID);
        System.out.println();
        
        try {
            // Step 1: Download the recording
            System.out.println("Step 1: Downloading recording from Twilio...");
            byte[] audioData = downloadRecording(RECORDING_SID);
            
            if (audioData != null && audioData.length > 0) {
                System.out.println("✅ Recording downloaded successfully: " + audioData.length + " bytes");
                
                // Step 2: Save audio file for verification
                String audioFileName = "test_audio_" + RECORDING_SID + ".wav";
                saveAudioFile(audioData, audioFileName);
                
                // Step 3: Perform speech-to-text
                System.out.println("\nStep 2: Performing speech-to-text conversion...");
                String transcription = performSpeechToText(audioData);
                
                if (transcription != null && !transcription.trim().isEmpty()) {
                    System.out.println("✅ Speech-to-text successful!");
                    System.out.println("\n=== TRANSCRIPTION RESULT ===");
                    System.out.println("\"" + transcription + "\"");
                    System.out.println("Length: " + transcription.length() + " characters");
                    System.out.println("===========================");
                } else {
                    System.out.println("❌ Speech-to-text returned empty result");
                }
                
            } else {
                System.out.println("❌ Failed to download recording");
            }
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Download recording from Twilio
     */
    private static byte[] downloadRecording(String recordingSid) throws Exception {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + 
                    "/Recordings/" + recordingSid + ".wav";
        
        System.out.println("Downloading from: " + url);
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        
        // Add Basic Authentication
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
            // Read error response
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String errorLine;
            StringBuilder errorResponse = new StringBuilder();
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            throw new RuntimeException("Download failed: " + errorResponse.toString());
        }
    }
    
    /**
     * Save audio data to file for verification
     */
    private static void saveAudioFile(byte[] audioData, String fileName) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(audioData);
            System.out.println("Audio saved as: " + fileName);
        }
    }
    
    /**
     * Perform speech-to-text using Google Cloud Speech API
     */
    private static String performSpeechToText(byte[] audioData) throws Exception {
        System.out.println("Initializing Google Cloud Speech client...");
        
        // Set Google Cloud credentials environment variable
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", GOOGLE_CREDENTIALS_PATH);
        
        try {
            // Initialize Google Cloud Speech client
            com.google.cloud.speech.v1.SpeechClient speechClient = com.google.cloud.speech.v1.SpeechClient.create();
            
            try {
                System.out.println("Converting audio data for Google Cloud Speech...");
                
                // Convert audio data to ByteString
                com.google.protobuf.ByteString audioBytes = com.google.protobuf.ByteString.copyFrom(audioData);
                
                // Build the audio object
                com.google.cloud.speech.v1.RecognitionAudio audio = com.google.cloud.speech.v1.RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();
                
                // Configure recognition settings for Twilio recordings
                com.google.cloud.speech.v1.RecognitionConfig config = com.google.cloud.speech.v1.RecognitionConfig.newBuilder()
                    .setEncoding(com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(8000) // Twilio uses 8kHz for phone calls
                    .setLanguageCode(LANGUAGE_CODE)
                    .setModel(SPEECH_MODEL)
                    .setEnableAutomaticPunctuation(true)
                    .setUseEnhanced(true)
                    .build();
                
                System.out.println("Sending request to Google Cloud Speech API...");
                System.out.println("Audio size: " + audioData.length + " bytes");
                System.out.println("Sample rate: 8000 Hz");
                System.out.println("Language: " + LANGUAGE_CODE);
                System.out.println("Model: " + SPEECH_MODEL);
                
                // Perform the transcription
                com.google.cloud.speech.v1.RecognizeResponse response = speechClient.recognize(config, audio);
                
                System.out.println("Received response from Google Cloud Speech API");
                System.out.println("Number of results: " + response.getResultsCount());
                
                // Extract transcript from response
                StringBuilder transcript = new StringBuilder();
                for (int i = 0; i < response.getResultsCount(); i++) {
                    com.google.cloud.speech.v1.SpeechRecognitionResult result = response.getResults(i);
                    System.out.println("Result " + (i + 1) + " alternatives: " + result.getAlternativesCount());
                    
                    if (result.getAlternativesCount() > 0) {
                        com.google.cloud.speech.v1.SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                        String text = alternative.getTranscript();
                        float confidence = alternative.getConfidence();
                        
                        System.out.println("  Text: \"" + text + "\"");
                        System.out.println("  Confidence: " + confidence);
                        
                        if (transcript.length() > 0) {
                            transcript.append(" ");
                        }
                        transcript.append(text);
                    }
                }
                
                String finalTranscript = transcript.toString().trim();
                
                if (finalTranscript.isEmpty()) {
                    System.out.println("⚠️  Google Speech API returned empty transcript");
                    System.out.println("This could mean:");
                    System.out.println("  - The audio is silent or very quiet");
                    System.out.println("  - The audio format is not compatible");
                    System.out.println("  - The speech is not clear enough");
                    return null;
                }
                
                return finalTranscript;
                
            } finally {
                speechClient.close();
                System.out.println("Google Cloud Speech client closed");
            }
            
        } catch (Exception e) {
            System.err.println("Google Cloud Speech API error: " + e.getMessage());
            
            // Print detailed error information
            if (e.getMessage().contains("credentials")) {
                System.err.println("\n🔧 Credential Issues:");
                System.err.println("  - Check if credentials file exists: " + GOOGLE_CREDENTIALS_PATH);
                System.err.println("  - Verify GOOGLE_APPLICATION_CREDENTIALS environment variable");
                System.err.println("  - Ensure the service account has Speech API permissions");
            } else if (e.getMessage().contains("quota") || e.getMessage().contains("billing")) {
                System.err.println("\n💳 Billing/Quota Issues:");
                System.err.println("  - Check if Google Cloud billing is enabled");
                System.err.println("  - Verify Speech-to-Text API is enabled");
                System.err.println("  - Check API quotas and limits");
            } else if (e.getMessage().contains("audio")) {
                System.err.println("\n🎵 Audio Format Issues:");
                System.err.println("  - Audio might be in unsupported format");
                System.err.println("  - Try different encoding settings");
                System.err.println("  - Check if audio file is corrupted");
            }
            
            throw e;
        }
    }
}