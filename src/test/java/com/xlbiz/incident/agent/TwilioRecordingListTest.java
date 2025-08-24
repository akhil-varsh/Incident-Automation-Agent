package com.xlbiz.incident.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * Helper script to list recent Twilio recordings and get their SIDs
 * Run this first to get actual recording SIDs for testing
 */
public class TwilioRecordingListTest {
    
    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    
    public static void main(String[] args) {
        System.out.println("=== Twilio Recording List Test ===");
        System.out.println("Fetching recent recordings from your Twilio account...");
        System.out.println();
        
        try {
            listRecentRecordings();
        } catch (Exception e) {
            System.err.println("Failed to list recordings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void listRecentRecordings() throws Exception {
        // Get recent recordings (last 20)
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + 
                    "/Recordings.json?PageSize=20";
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        
        // Add Basic Authentication
        String credentials = ACCOUNT_SID + ":" + AUTH_TOKEN;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
        
        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);
        
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String jsonResponse = response.toString();
            System.out.println("✅ Successfully retrieved recordings list!");
            System.out.println();
            
            // Parse and display recording SIDs (simple string parsing)
            parseAndDisplayRecordings(jsonResponse);
            
        } else {
            System.out.println("❌ Failed to retrieve recordings!");
            
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
    
    private static void parseAndDisplayRecordings(String jsonResponse) {
        System.out.println("Recent Recordings:");
        System.out.println("==================");
        
        // Simple parsing to extract recording SIDs and basic info
        String[] recordings = jsonResponse.split("\"sid\":");
        
        if (recordings.length <= 1) {
            System.out.println("No recordings found in your account.");
            System.out.println("Make a test call to generate some recordings first.");
            return;
        }
        
        for (int i = 1; i < recordings.length; i++) {
            String recordingData = recordings[i];
            
            // Extract SID
            String sid = extractValue(recordingData, "\"", "\"");
            
            // Extract duration if available
            String duration = extractValue(recordingData, "\"duration\":", ",");
            if (duration.startsWith("\"")) {
                duration = duration.substring(1, duration.length() - 1);
            }
            
            // Extract date created if available
            String dateCreated = extractValue(recordingData, "\"date_created\":", ",");
            if (dateCreated.startsWith("\"")) {
                dateCreated = dateCreated.substring(1, dateCreated.length() - 1);
            }
            
            System.out.println("Recording #" + i + ":");
            System.out.println("  SID: " + sid);
            System.out.println("  Duration: " + duration + " seconds");
            System.out.println("  Created: " + dateCreated);
            System.out.println();
            
            if (i == 1) {
                System.out.println("💡 Copy this SID to use in TwilioRecordingDownloadTest:");
                System.out.println("   TEST_RECORDING_SID = \"" + sid + "\";");
                System.out.println();
            }
        }
    }
    
    private static String extractValue(String text, String startDelim, String endDelim) {
        try {
            int start = text.indexOf(startDelim);
            if (start == -1) return "N/A";
            start += startDelim.length();
            
            int end = text.indexOf(endDelim, start);
            if (end == -1) return "N/A";
            
            return text.substring(start, end).trim();
        } catch (Exception e) {
            return "N/A";
        }
    }
}