package com.userservice.user.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.ZsetScoreCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration redisTimeout;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        try {
            LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, redisPort);
            factory.setShareNativeConnection(false);
            factory.setValidateConnection(true);
            
            // Configure timeout
            factory.setTimeout(redisTimeout.toMillis());
            
            log.info("Redis connection factory configured for {}:{}", redisHost, redisPort);
            return factory;
        } catch (Exception e) {
            log.error("Error configuring Redis connection factory", e);
            throw e;
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        
        log.info("Redis template configured with JSON serialization");
        return template;
    }

    @Bean
    public ProxyManager<String> bucket4jProxyManager(RedisConnectionFactory connectionFactory) {
        try {
            // Create Redis client for Bucket4j
            RedisClient redisClient = RedisClient.create(
                RedisURI.builder()
                    .withHost(redisHost)
                    .withPort(redisPort)
                    .withPassword(redisPassword.isEmpty() ? null : redisPassword)
                    .build()
            );

            StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                new ZsetScoreCodec<>(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
            );

            ProxyManager<String> proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();

            log.info("Bucket4j proxy manager configured for Redis");
            return proxyManager;
        } catch (Exception e) {
            log.error("Error configuring Bucket4j proxy manager", e);
            throw e;
        }
    }
}
