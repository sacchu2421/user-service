package com.userservice.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@EnableJpaAuditing
public class UserServiceApplication {

    public static void main(String[] args) {
        log.info("Starting User Management Service...");
        
        try {
            SpringApplication.run(UserServiceApplication.class, args);
            log.info("User Management Service started successfully!");
        } catch (Exception e) {
            log.error("Failed to start User Management Service", e);
            System.exit(1);
        }
    }
}
