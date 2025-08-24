package com.xlbiz.incident.agent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Simple test script to verify Twilio recording download functionality
 * Run this as a standalone test to check if Twilio authentication and download works
 */
public class TwilioRecordingDownloadTest {
    
    // Load credentials from environment variables
    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    
    // Use a valid recording SID from TwilioRecordingListTest output
    private static final String TEST_RECORDING_SID = System.getenv("TWILIO_TEST_RECORDING_SID"); // Set this in your .env file
    
    public static void main(String[] args) {
        System.out.println("=== Twilio Recording Download Test ===");
        System.out.println("Account SID: " + ACCOUNT_SID);
        System.out.println("Recording SID: " + TEST_RECORDING_SID);
        System.out.println();
        
        try {
            // Test 1: Check if we can authenticate with Twilio
            System.out.println("Test 1: Testing Twilio Authentication...");
            testTwilioAuth();
            
            // Test 2: Download a specific recording
            System.out.println("\nTest 2: Testing Recording Download...");
            downloadRecording(TEST_RECORDING_SID);
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test basic Twilio authentication by making a simple API call
     */
    private static void testTwilioAuth() throws Exception {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + ".json";
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        
        // Add Basic Authentication
        String credentials = ACCOUNT_SID + ":" + AUTH_TOKEN;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
        
        int responseCode = connection.getResponseCode();
        System.out.println("Authentication Response Code: " + responseCode);
        
        if (responseCode == 200) {
            System.out.println("Authentication successful!");
        } else {
            System.out.println("Authentication failed!");
            
            // Read error response
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String errorLine;
            StringBuilder errorResponse = new StringBuilder();
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            System.out.println("Error Response: " + errorResponse.toString());
        }
        
        connection.disconnect();
    }
    
    /**
     * Download a recording from Twilio
     */
    private static void downloadRecording(String recordingSid) throws Exception {
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
            System.out.println("Recording download successful!");
            
            // Save the audio file
            String fileName = "test_recording_" + recordingSid + ".wav";
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(fileName)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                System.out.println("File saved as: " + fileName);
                System.out.println("File size: " + totalBytes + " bytes");
                
                // Verify file exists and has content
                if (Files.exists(Paths.get(fileName)) && Files.size(Paths.get(fileName)) > 0) {
                    System.out.println("File verification successful!");
                } else {
                    System.out.println("File verification failed!");
                }
            }
            
        } else {
            System.out.println("Recording download failed!");
            
            // Read error response
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String errorLine;
            StringBuilder errorResponse = new StringBuilder();
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            System.out.println("Error Response: " + errorResponse.toString());
        }
        
        connection.disconnect();
    }
}