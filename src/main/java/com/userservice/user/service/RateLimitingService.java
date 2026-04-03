package com.userservice.user.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final ProxyManager<String> buckets;

    @Value("${rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${rate-limit.requests-per-hour:1000}")
    private int requestsPerHour;

    @Value("${rate-limit.requests-per-day:10000}")
    private int requestsPerDay;

    public boolean tryConsume(String key) {
        return tryConsume(key, 1);
    }

    public boolean tryConsume(String key, int tokens) {
        try {
            Bucket bucket = buckets.builder().build(key, getConfigSupplier());
            return bucket.tryConsume(tokens);
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Fail open - allow request if rate limiting fails
            return true;
        }
    }

    public boolean tryConsume(HttpServletRequest request) {
        return tryConsume(getClientIdentifier(request));
    }

    public boolean tryConsume(HttpServletRequest request, int tokens) {
        return tryConsume(getClientIdentifier(request), tokens);
    }

    public Bucket getBucket(String key) {
        return buckets.builder().build(key, getConfigSupplier());
    }

    public Bucket getBucket(HttpServletRequest request) {
        return getBucket(getClientIdentifier(request));
    }

    public long getRemainingTokens(String key) {
        try {
            Bucket bucket = getBucket(key);
            return bucket.getAvailableTokens();
        } catch (Exception e) {
            log.error("Error getting remaining tokens for key: {}", key, e);
            return 0;
        }
    }

    public long getRemainingTokens(HttpServletRequest request) {
        return getRemainingTokens(getClientIdentifier(request));
    }

    public Duration getTimeToRefill(String key) {
        try {
            Bucket bucket = getBucket(key);
            return bucket.getAvailableTokens() > 0 ? Duration.ZERO : bucket.getAvailableTokens();
        } catch (Exception e) {
            log.error("Error getting time to refill for key: {}", key, e);
            return Duration.ofMinutes(1);
        }
    }

    public Duration getTimeToRefill(HttpServletRequest request) {
        return getTimeToRefill(getClientIdentifier(request));
    }

    public void resetBucket(String key) {
        try {
            Bucket bucket = getBucket(key);
            bucket.reset();
            log.debug("Reset rate limit bucket for key: {}", key);
        } catch (Exception e) {
            log.error("Error resetting rate limit bucket for key: {}", key, e);
        }
    }

    private Supplier<BucketConfiguration> getConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        requestsPerMinute,
                        Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
                ))
                .addLimit(Bandwidth.classic(
                        requestsPerHour,
                        Refill.intervally(requestsPerHour, Duration.ofHours(1))
                ))
                .addLimit(Bandwidth.classic(
                        requestsPerDay,
                        Refill.intervally(requestsPerDay, Duration.ofDays(1))
                ))
                .build();
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get client IP from various headers
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_FORWARDED");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_CLIENT_IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_FORWARDED");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_VIA");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("REMOTE_ADDR");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        // If multiple IPs, take the first one
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        // Add user agent to make the key more specific
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            userAgent = userAgent.replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(50, userAgent.length()));
            return clientIp + ":" + userAgent;
        }

        return clientIp;
    }

    public boolean isRateLimitingEnabled() {
        return buckets != null;
    }

    public void logRateLimitStats(String key) {
        try {
            Bucket bucket = getBucket(key);
            log.info("Rate limit stats for {}: Available tokens: {}, Refill rate: {}/min", 
                    key, bucket.getAvailableTokens(), requestsPerMinute);
        } catch (Exception e) {
            log.error("Error logging rate limit stats for key: {}", key, e);
        }
    }
}
