package com.xlbiz.incident.agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Home controller to handle root path requests.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return """
            <html>
            <head>
                <title>XLBiz.AI Incident Automation Agent</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }
                    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
                    .endpoint { background: #f8f9fa; padding: 15px; margin: 10px 0; border-left: 4px solid #007bff; }
                    .method { color: #28a745; font-weight: bold; }
                    .path { color: #007bff; font-family: monospace; }
                    .status { color: #28a745; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🚨 XLBiz.AI Incident Automation Agent</h1>
                    <p><span class="status">✅ Service is running</span></p>
                    
                    <h2>Available API Endpoints:</h2>
                    
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/api/health/status</span><br>
                        Health check endpoint
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/api/v1/incidents</span><br>
                        List all incidents
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">POST</span> <span class="path">/api/v1/incidents</span><br>
                        Create new incident
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/api/v1/knowledge/search</span><br>
                        Search knowledge base (text-based)
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/api/v1/knowledge/search-enhanced</span><br>
                        Search knowledge base (with Ollama embeddings)
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/api/v1/knowledge/health-enhanced</span><br>
                        Enhanced knowledge base health check
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">POST</span> <span class="path">/api/v1/knowledge/initialize-enhanced</span><br>
                        Initialize enhanced knowledge base with embeddings
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/api/v1/knowledge/search-vector</span><br>
                        Search knowledge base (Vector Search with Ollama)
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/api/v1/knowledge/health-vector</span><br>
                        Vector search service health check
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">POST</span> <span class="path">/api/v1/knowledge/initialize-vector</span><br>
                        Initialize vector knowledge base
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">POST</span> <span class="path">/api/voice/webhook</span><br>
                        Vonage voice webhook
                    </div>
                    
                    <div class="endpoint">
                        <span class="method">POST</span> <span class="path">/api/twilio/voice/webhook</span><br>
                        Twilio voice webhook
                    </div>
                    
                    <h2>Administration:</h2>
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path"><a href="/admin/database" style="color: #007bff; text-decoration: none;">/admin/database</a></span><br>
                        Database viewer - View incidents and voice calls tables
                    </div>
                    
                    <h2>Monitoring:</h2>
                    <div class="endpoint">
                        <span class="method">GET</span> <span class="path">/actuator/health</span><br>
                        Spring Boot health endpoint
                    </div>
                    
                    <p><em>Incident Automation Agent with AI classification and multi-platform integration</em></p>
                </div>
            </body>
            </html>
            """;
    }
}