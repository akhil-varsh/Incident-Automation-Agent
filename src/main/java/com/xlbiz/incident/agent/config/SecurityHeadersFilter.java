package com.xlbiz.incident.agent.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Security headers filter to add security-related HTTP headers to all responses.
 * Helps protect against common web vulnerabilities.
 */
@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        if (response instanceof HttpServletResponse httpResponse) {
            // Prevent clickjacking
            httpResponse.setHeader("X-Frame-Options", "DENY");
            
            // Prevent MIME type sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            
            // Enable XSS protection
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Strict transport security (HTTPS only)
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            // Content Security Policy
            httpResponse.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self'; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'"
            );
            
            // Referrer Policy
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Feature Policy / Permissions Policy
            httpResponse.setHeader("Permissions-Policy", 
                "geolocation=(), microphone=(), camera=(), fullscreen=(self)"
            );
            
            // Cache control for API responses
            if (request instanceof jakarta.servlet.http.HttpServletRequest httpRequest) {
                String path = httpRequest.getRequestURI();
                if (path.startsWith("/api/")) {
                    httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    httpResponse.setHeader("Pragma", "no-cache");
                    httpResponse.setHeader("Expires", "0");
                }
            }
        }
        
        chain.doFilter(request, response);
    }
}
