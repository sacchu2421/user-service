package com.userservice.user.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    
    private final long remainingTokens;
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(String message, long remainingTokens, long retryAfterSeconds) {
        super(message);
        this.remainingTokens = remainingTokens;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitExceededException(String message) {
        this(message, 0, 60);
    }
}
