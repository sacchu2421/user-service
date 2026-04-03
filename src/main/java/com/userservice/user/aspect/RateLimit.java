package com.userservice.user.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Number of tokens to consume for this request
     * Default is 1 token per request
     */
    int tokens() default 1;
    
    /**
     * Key prefix for rate limiting
     * Default is "default"
     */
    String keyPrefix() default "default";
    
    /**
     * Custom message when rate limit is exceeded
     * Default uses the standard message
     */
    String message() default "";
}
