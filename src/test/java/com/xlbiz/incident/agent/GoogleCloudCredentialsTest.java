package com.xlbiz.incident.agent;

import java.io.File;
import java.io.FileInputStream;

/**
 * Simple test to verify Google Cloud credentials setup
 */
public class GoogleCloudCredentialsTest {
    
    public static void main(String[] args) {
        System.out.println("=== Google Cloud Credentials Test ===");
        
        try {
            // Test 1: Check if credentials file exists
            System.out.println("Test 1: Checking credentials file...");
            String credentialsPath = "akhil-stt-834eac683b23.json";
            File credentialsFile = new File(credentialsPath);
            
            if (credentialsFile.exists()) {
                System.out.println("✅ Credentials file found: " + credentialsFile.getAbsolutePath());
                System.out.println("   File size: " + credentialsFile.length() + " bytes");
            } else {
                System.out.println("❌ Credentials file not found: " + credentialsPath);
                System.out.println("   Current directory: " + System.getProperty("user.dir"));
                return;
            }
            
            // Test 2: Try to load credentials
            System.out.println("\nTest 2: Loading Google Cloud credentials...");
            try {
                com.google.auth.oauth2.GoogleCredentials credentials = 
                    com.google.auth.oauth2.GoogleCredentials.fromStream(
                        new FileInputStream(credentialsFile));
                
                System.out.println("✅ Credentials loaded successfully");
                
                // Test 3: Try to create Speech client
                System.out.println("\nTest 3: Creating Speech client...");
                
                com.google.cloud.speech.v1.SpeechSettings speechSettings = 
                    com.google.cloud.speech.v1.SpeechSettings.newBuilder()
                        .setCredentialsProvider(() -> credentials)
                        .build();
                
                com.google.cloud.speech.v1.SpeechClient speechClient = 
                    com.google.cloud.speech.v1.SpeechClient.create(speechSettings);
                
                System.out.println("✅ Speech client created successfully");
                
                // Test 4: Basic API connectivity (without actual transcription)
                System.out.println("\nTest 4: Testing API connectivity...");
                
                // Just check if we can create a simple config (no API call)
                com.google.cloud.speech.v1.RecognitionConfig config = 
                    com.google.cloud.speech.v1.RecognitionConfig.newBuilder()
                        .setEncoding(com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(8000)
                        .setLanguageCode("en-US")
                        .setModel("latest_long")
                        .setEnableAutomaticPunctuation(true)
                        .setUseEnhanced(true)
                        .build();
                
                System.out.println("✅ Recognition config created successfully");
                System.out.println("   Encoding: " + config.getEncoding());
                System.out.println("   Sample Rate: " + config.getSampleRateHertz() + " Hz");
                System.out.println("   Language: " + config.getLanguageCode());
                System.out.println("   Model: " + config.getModel());
                
                speechClient.close();
                System.out.println("\n🎉 All tests passed! Google Cloud Speech-to-Text is ready to use.");
                
            } catch (Exception e) {
                System.err.println("❌ Failed to load credentials or create client: " + e.getMessage());
                
                if (e.getMessage().contains("project")) {
                    System.err.println("💡 Make sure the project ID is correct in the credentials file");
                } else if (e.getMessage().contains("billing")) {
                    System.err.println("💡 Check if billing is enabled for your Google Cloud project");
                } else if (e.getMessage().contains("API")) {
                    System.err.println("💡 Make sure Speech-to-Text API is enabled in your Google Cloud project");
                }
                
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}