package com.xlbiz.incident.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Test script for Twilio outbound calling functionality
 * Tests making outbound calls for incident notifications
 */
public class TwilioOutboundCallTest {
    
    // Test configuration
    private static final String BASE_URL = "http://localhost:8080"; // Your application URL
    private static final String TEST_PHONE_NUMBER = "+1234567890"; // Replace with a real test number
    private static final String TEST_INCIDENT_ID = "test-incident-001";
    
    public static void main(String[] args) {
        System.out.println("=== Twilio Outbound Call Test ===");
        System.out.println("Base URL: " + BASE_URL);
        System.out.println("Test Phone Number: " + TEST_PHONE_NUMBER);
        System.out.println();
        
        try {
            // Test 1: Check outbound calling configuration
            System.out.println("Test 1: Checking outbound calling configuration...");
            testConfiguration();
            
            // Test 2: Test incident notification call
            System.out.println("\nTest 2: Testing incident notification call...");
            testIncidentNotificationCall();
            
            // Test 3: Test incident update call
            System.out.println("\nTest 3: Testing incident update call...");
            testIncidentUpdateCall();
            
            // Test 4: Test TwiML generation
            System.out.println("\nTest 4: Testing TwiML generation...");
            testTwiMLGeneration();
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test outbound calling configuration
     */
    private static void testConfiguration() throws Exception {
        String url = BASE_URL + "/api/twilio/outbound/config";
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        
        int responseCode = connection.getResponseCode();
        System.out.println("Configuration Response Code: " + responseCode);
        
        if (responseCode == 200) {
            String response = readResponse(connection);
            System.out.println("✅ Configuration retrieved successfully:");
            System.out.println(response);
        } else {
            System.out.println("❌ Failed to retrieve configuration");
            String errorResponse = readErrorResponse(connection);
            System.out.println("Error: " + errorResponse);
        }
    }
    
    /**
     * Test incident notification call
     */
    private static void testIncidentNotificationCall() throws Exception {
        String url = BASE_URL + "/api/twilio/outbound/call/incident-notification";
        
        String requestBody = String.format("""
            {
                "toPhoneNumber": "%s",
                "incidentId": "%s",
                "severity": "HIGH",
                "message": "Database connection failure detected in production environment. Multiple services affected."
            }
            """, TEST_PHONE_NUMBER, TEST_INCIDENT_ID);
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        // Send request body
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        System.out.println("Incident Notification Call Response Code: " + responseCode);
        
        if (responseCode == 200) {
            String response = readResponse(connection);
            System.out.println("✅ Incident notification call initiated successfully:");
            System.out.println(response);
        } else {
            System.out.println("❌ Failed to initiate incident notification call");
            String errorResponse = readErrorResponse(connection);
            System.out.println("Error: " + errorResponse);
        }
    }
    
    /**
     * Test incident update call
     */
    private static void testIncidentUpdateCall() throws Exception {
        String url = BASE_URL + "/api/twilio/outbound/call/incident-update";
        
        String requestBody = String.format("""
            {
                "toPhoneNumber": "%s",
                "incidentId": "%s",
                "status": "RESOLVED",
                "message": "The database connection issue has been resolved. All services are now operational."
            }
            """, TEST_PHONE_NUMBER, TEST_INCIDENT_ID);
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        // Send request body
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        System.out.println("Incident Update Call Response Code: " + responseCode);
        
        if (responseCode == 200) {
            String response = readResponse(connection);
            System.out.println("✅ Incident update call initiated successfully:");
            System.out.println(response);
        } else {
            System.out.println("❌ Failed to initiate incident update call");
            String errorResponse = readErrorResponse(connection);
            System.out.println("Error: " + errorResponse);
        }
    }
    
    /**
     * Test TwiML generation endpoints
     */
    private static void testTwiMLGeneration() throws Exception {
        // Test incident notification TwiML
        System.out.println("Testing incident notification TwiML...");
        String notificationUrl = BASE_URL + "/api/twilio/outbound/twiml/incident-notification" +
                                "?incidentId=" + TEST_INCIDENT_ID +
                                "&severity=HIGH" +
                                "&message=Test incident message";
        
        testTwiMLEndpoint(notificationUrl, "Incident Notification TwiML");
        
        // Test incident update TwiML
        System.out.println("\nTesting incident update TwiML...");
        String updateUrl = BASE_URL + "/api/twilio/outbound/twiml/incident-update" +
                          "?incidentId=" + TEST_INCIDENT_ID +
                          "&status=RESOLVED" +
                          "&message=Test update message";
        
        testTwiMLEndpoint(updateUrl, "Incident Update TwiML");
        
        // Test default TwiML
        System.out.println("\nTesting default TwiML...");
        String defaultUrl = BASE_URL + "/api/twilio/outbound/twiml/default";
        
        testTwiMLEndpoint(defaultUrl, "Default TwiML");
    }
    
    /**
     * Test a TwiML endpoint
     */
    private static void testTwiMLEndpoint(String url, String description) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/xml");
        
        int responseCode = connection.getResponseCode();
        System.out.println(description + " Response Code: " + responseCode);
        
        if (responseCode == 200) {
            String response = readResponse(connection);
            System.out.println("✅ " + description + " generated successfully:");
            System.out.println(response);
        } else {
            System.out.println("❌ Failed to generate " + description);
            String errorResponse = readErrorResponse(connection);
            System.out.println("Error: " + errorResponse);
        }
    }
    
    /**
     * Read successful response
     */
    private static String readResponse(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }
    
    /**
     * Read error response
     */
    private static String readErrorResponse(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        } catch (Exception e) {
            return "Unable to read error response: " + e.getMessage();
        }
    }
}