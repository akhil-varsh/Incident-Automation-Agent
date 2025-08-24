package com.xlbiz.incident.agent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * Standalone test for recording download and speech-to-text
 * No Spring dependencies - can run directly with java command
 */
public class StandaloneSpeechTest {
    
    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String RECORDING_SID = System.getenv("TWILIO_TEST_RECORDING_SID");
    
    public static void main(String[] args) {
        System.out.println("=== Standalone Speech-to-Text Test ===");
        System.out.println("Recording SID: " + RECORDING_SID);
        System.out.println();
        
        try {
            // Step 1: Download recording
            System.out.println("Step 1: Downloading recording from Twilio...");
            byte[] audioData = downloadRecording();
            
            if (audioData != null && audioData.length > 0) {
                System.out.println("✅ Recording downloaded successfully: " + audioData.length + " bytes");
                
                // Save audio file
                String fileName = "standalone_test_" + RECORDING_SID + ".wav";
                saveAudioFile(audioData, fileName);
                
                // Step 2: Test Google Cloud Speech-to-Text
                System.out.println("\nStep 2: Testing Google Cloud Speech-to-Text...");
                testSpeechToText(audioData);
                
            } else {
                System.out.println("❌ Failed to download recording");
            }
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static byte[] downloadRecording() throws Exception {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + 
                    "/Recordings/" + RECORDING_SID + ".wav";
        
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
    
    private static void saveAudioFile(byte[] audioData, String fileName) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(audioData);
            System.out.println("Audio saved as: " + fileName);
        }
    }
    
    private static void testSpeechToText(byte[] audioData) {
        try {
            System.out.println("Initializing Google Cloud Speech client...");
            System.out.println("Using Application Default Credentials");
            
            // Create Speech client with Application Default Credentials
            com.google.cloud.speech.v1.SpeechClient speechClient = 
                com.google.cloud.speech.v1.SpeechClient.create();
            
            try {
                System.out.println("Converting audio data for Google Cloud Speech...");
                
                // Convert audio data to ByteString
                com.google.protobuf.ByteString audioBytes = 
                    com.google.protobuf.ByteString.copyFrom(audioData);
                
                // Build the audio object
                com.google.cloud.speech.v1.RecognitionAudio audio = 
                    com.google.cloud.speech.v1.RecognitionAudio.newBuilder()
                        .setContent(audioBytes)
                        .build();
                
                // Configure recognition settings for Twilio recordings
                com.google.cloud.speech.v1.RecognitionConfig config = 
                    com.google.cloud.speech.v1.RecognitionConfig.newBuilder()
                        .setEncoding(com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(8000) // Twilio uses 8kHz for phone calls
                        .setLanguageCode("en-US")
                        .setModel("latest_long")
                        .setEnableAutomaticPunctuation(true)
                        .setUseEnhanced(true)
                        .build();
                
                System.out.println("Sending request to Google Cloud Speech API...");
                System.out.println("Configuration:");
                System.out.println("  Audio size: " + audioData.length + " bytes");
                System.out.println("  Sample rate: 8000 Hz");
                System.out.println("  Language: en-US");
                System.out.println("  Model: latest_long");
                System.out.println("  Enhanced: true");
                System.out.println("  Auto punctuation: true");
                
                // Perform the transcription
                com.google.cloud.speech.v1.RecognizeResponse response = 
                    speechClient.recognize(config, audio);
                
                System.out.println("\nReceived response from Google Cloud Speech API");
                System.out.println("Number of results: " + response.getResultsCount());
                
                // Extract transcript from response
                StringBuilder transcript = new StringBuilder();
                for (int i = 0; i < response.getResultsCount(); i++) {
                    com.google.cloud.speech.v1.SpeechRecognitionResult result = response.getResults(i);
                    System.out.println("\nResult " + (i + 1) + ":");
                    System.out.println("  Alternatives: " + result.getAlternativesCount());
                    
                    if (result.getAlternativesCount() > 0) {
                        com.google.cloud.speech.v1.SpeechRecognitionAlternative alternative = 
                            result.getAlternatives(0);
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
                
                System.out.println("\n" + "=".repeat(50));
                if (!finalTranscript.isEmpty()) {
                    System.out.println("🎉 TRANSCRIPTION SUCCESSFUL!");
                    System.out.println("=".repeat(50));
                    System.out.println("FINAL TRANSCRIPT:");
                    System.out.println("\"" + finalTranscript + "\"");
                    System.out.println();
                    System.out.println("Length: " + finalTranscript.length() + " characters");
                    System.out.println("Word count: " + finalTranscript.split("\\s+").length + " words");
                } else {
                    System.out.println("⚠️  NO TRANSCRIPTION RESULT");
                    System.out.println("=".repeat(50));
                    System.out.println("Possible reasons:");
                    System.out.println("- Audio is silent or very quiet");
                    System.out.println("- Audio quality is too poor");
                    System.out.println("- Audio format incompatibility");
                    System.out.println("- Speech is not in English");
                }
                System.out.println("=".repeat(50));
                
            } finally {
                speechClient.close();
                System.out.println("\nGoogle Cloud Speech client closed");
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ Google Cloud Speech-to-Text failed!");
            System.err.println("Error: " + e.getMessage());
            
            // Provide helpful debugging information
            if (e.getMessage().contains("credentials")) {
                System.err.println("\n🔧 CREDENTIAL ISSUES:");
                System.err.println("- Make sure you ran: gcloud auth application-default login");
                System.err.println("- Check if credentials exist: ~/.config/gcloud/application_default_credentials.json");
            } else if (e.getMessage().contains("quota") || e.getMessage().contains("billing")) {
                System.err.println("\n💳 BILLING/QUOTA ISSUES:");
                System.err.println("- Enable billing in Google Cloud Console");
                System.err.println("- Enable Speech-to-Text API");
                System.err.println("- Check API quotas and limits");
            } else if (e.getMessage().contains("permission") || e.getMessage().contains("403")) {
                System.err.println("\n🔐 PERMISSION ISSUES:");
                System.err.println("- Make sure your account has Speech-to-Text API access");
                System.err.println("- Check IAM permissions for the project");
            } else if (e.getMessage().contains("audio") || e.getMessage().contains("format")) {
                System.err.println("\n🎵 AUDIO FORMAT ISSUES:");
                System.err.println("- Audio format might not be supported");
                System.err.println("- Try different encoding settings");
            }
            
            e.printStackTrace();
        }
    }
}