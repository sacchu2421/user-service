package com.userservice.user.config;

import com.userservice.user.exception.RateLimitExceededException;
import com.userservice.user.service.RateLimitingService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Apply rate limiting to all requests
            if (!rateLimitingService.tryConsume(request)) {
                handleRateLimitExceeded(request, response);
                return;
            }
            
            // Add rate limit headers
            addRateLimitHeaders(request, response);
            
        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            // Continue processing if rate limiting fails
        }
        
        filterChain.doFilter(request, response);
    }

    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("Rate limit exceeded for request: {} {}", request.getMethod(), request.getRequestURI());
        
        String clientKey = getClientKey(request);
        long remainingTokens = rateLimitingService.getRemainingTokens(clientKey);
        Duration timeToRefill = rateLimitingService.getTimeToRefill(clientKey);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
        response.setHeader("X-Rate-Limit-Retry-After", String.valueOf(timeToRefill.getSeconds()));
        response.setHeader("Retry-After", String.valueOf(timeToRefill.getSeconds()));
        
        String errorResponse = String.format("""
            {
                "timestamp": "%s",
                "status": 429,
                "error": "Too Many Requests",
                "message": "Rate limit exceeded. Please try again in %d seconds.",
                "path": "%s",
                "remainingTokens": %d,
                "retryAfter": %d
            }
            """, 
            java.time.LocalDateTime.now(),
            timeToRefill.getSeconds(),
            request.getRequestURI(),
            remainingTokens,
            timeToRefill.getSeconds());
        
        response.getWriter().write(errorResponse);
    }

    private void addRateLimitHeaders(HttpServletRequest request, HttpServletResponse response) {
        try {
            String clientKey = getClientKey(request);
            long remainingTokens = rateLimitingService.getRemainingTokens(clientKey);
            Duration timeToRefill = rateLimitingService.getTimeToRefill(clientKey);
            
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
            response.setHeader("X-Rate-Limit-Retry-After", String.valueOf(timeToRefill.getSeconds()));
            
        } catch (Exception e) {
            log.debug("Error adding rate limit headers", e);
        }
    }

    private String getClientKey(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            userAgent = userAgent.replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(50, userAgent.length()));
            return clientIp + ":" + userAgent;
        }
        return clientIp;
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        
        // If multiple IPs, take the first one
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        
        return clientIp;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for health checks and metrics
        return path.startsWith("/actuator/health") || 
               path.startsWith("/actuator/prometheus") ||
               path.startsWith("/actuator/info");
    }
}
