package com.xlbiz.incident.agent.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limiting configuration using Redis or in-memory fallback.
 * Implements rate limiting for the incident trigger endpoint to prevent abuse.
 */
@Configuration
public class RateLimitingConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.requests-per-hour:1000}")
    private int requestsPerHour;

    private final RedisTemplate<String, Object> redisTemplate;
    
    // Fallback in-memory rate limiter if Redis is unavailable
    private final ConcurrentHashMap<String, RateLimitInfo> inMemoryLimits = new ConcurrentHashMap<>();

    public RateLimitingConfig(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitEnabled) {
            registry.addInterceptor(new RateLimitInterceptor())
                    .addPathPatterns("/api/incidents/trigger");
        }
    }

    /**
     * Rate limiting interceptor that checks request limits before processing
     */
    public class RateLimitInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (!rateLimitEnabled) {
                return true;
            }

            String clientIdentifier = getClientIdentifier(request);
            
            if (isRateLimited(clientIdentifier)) {
                logger.warn("Rate limit exceeded for client: {}", clientIdentifier);
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}");
                return false;
            }

            return true;
        }

        private String getClientIdentifier(HttpServletRequest request) {
            // Try to get API key first, then fall back to IP address
            String apiKey = request.getHeader("X-API-Key");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                return "api:" + apiKey;
            }
            
            // Get real IP address considering proxy headers
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                return "ip:" + xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.trim().isEmpty()) {
                return "ip:" + xRealIp;
            }
            
            return "ip:" + request.getRemoteAddr();
        }

        private boolean isRateLimited(String clientIdentifier) {
            try {
                // Try Redis first if available
                if (redisTemplate != null) {
                    return isRateLimitedRedis(clientIdentifier);
                } else {
                    // Fall back to in-memory rate limiting
                    return isRateLimitedInMemory(clientIdentifier);
                }
            } catch (Exception e) {
                logger.warn("Redis rate limiting failed, falling back to in-memory: {}", e.getMessage());
                // Fall back to in-memory rate limiting
                return isRateLimitedInMemory(clientIdentifier);
            }
        }

        private boolean isRateLimitedRedis(String clientIdentifier) {
            String minuteKey = "rate_limit:minute:" + clientIdentifier;
            String hourKey = "rate_limit:hour:" + clientIdentifier;

            // Check minute limit
            Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
            if (minuteCount == 1) {
                redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
            }
            
            if (minuteCount > requestsPerMinute) {
                return true;
            }

            // Check hour limit
            Long hourCount = redisTemplate.opsForValue().increment(hourKey);
            if (hourCount == 1) {
                redisTemplate.expire(hourKey, Duration.ofHours(1));
            }
            
            return hourCount > requestsPerHour;
        }

        private boolean isRateLimitedInMemory(String clientIdentifier) {
            long currentTime = System.currentTimeMillis();
            
            RateLimitInfo limitInfo = inMemoryLimits.computeIfAbsent(clientIdentifier, 
                k -> new RateLimitInfo());

            synchronized (limitInfo) {
                // Reset counters if time windows have expired
                if (currentTime - limitInfo.minuteWindowStart > 60000) {
                    limitInfo.minuteCount = 0;
                    limitInfo.minuteWindowStart = currentTime;
                }
                
                if (currentTime - limitInfo.hourWindowStart > 3600000) {
                    limitInfo.hourCount = 0;
                    limitInfo.hourWindowStart = currentTime;
                }

                // Check limits
                if (limitInfo.minuteCount >= requestsPerMinute || limitInfo.hourCount >= requestsPerHour) {
                    return true;
                }

                // Increment counters
                limitInfo.minuteCount++;
                limitInfo.hourCount++;
                
                return false;
            }
        }
    }

    /**
     * In-memory rate limit tracking information
     */
    private static class RateLimitInfo {
        volatile int minuteCount = 0;
        volatile int hourCount = 0;
        volatile long minuteWindowStart = System.currentTimeMillis();
        volatile long hourWindowStart = System.currentTimeMillis();
    }
}
