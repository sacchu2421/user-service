package com.userservice.user.aspect;

import com.userservice.user.exception.RateLimitExceededException;
import com.userservice.user.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitingAspect {

    private final RateLimitingService rateLimitingService;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.warn("No HTTP request found, skipping rate limiting");
            return joinPoint.proceed();
        }

        String key = generateKey(request, rateLimit.keyPrefix());
        int tokens = rateLimit.tokens();

        if (!rateLimitingService.tryConsume(key, tokens)) {
            long remainingTokens = rateLimitingService.getRemainingTokens(key);
            Duration timeToRefill = rateLimitingService.getTimeToRefill(key);
            
            log.warn("Rate limit exceeded for key: {}. Remaining tokens: {}, Time to refill: {} seconds", 
                    key, remainingTokens, timeToRefill.getSeconds());
            
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please try again in " + timeToRefill.getSeconds() + " seconds.",
                    remainingTokens,
                    timeToRefill.getSeconds()
            );
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("Error getting current request", e);
            return null;
        }
    }

    private String generateKey(HttpServletRequest request, String prefix) {
        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();
        String method = request.getMethod();
        
        return String.format("%s:%s:%s:%s", prefix, clientIp, method, endpoint);
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
}
