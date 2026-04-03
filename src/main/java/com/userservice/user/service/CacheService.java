package com.userservice.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.default-ttl:3600}")
    private long defaultTtl;

    public void evictCache(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("Evicted cache entry: {} from cache: {}", key, cacheName);
            }
        } catch (Exception e) {
            log.error("Error evicting cache entry: {} from cache: {}", key, cacheName, e);
        }
    }

    public void evictAllCache(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared all entries from cache: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Error clearing cache: {}", cacheName, e);
        }
    }

    public void putInCache(String cacheName, Object key, Object value, Duration ttl) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
                
                // Set TTL in Redis if available
                if (redisTemplate != null) {
                    String redisKey = cacheName + "::" + key.toString();
                    redisTemplate.expire(redisKey, ttl);
                }
                
                log.debug("Added entry to cache: {} with key: {} and TTL: {}", cacheName, key, ttl);
            }
        } catch (Exception e) {
            log.error("Error adding entry to cache: {} with key: {}", cacheName, key, e);
        }
    }

    public Object getFromCache(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Object value = cache.get(key);
                if (value != null) {
                    log.debug("Cache hit for key: {} in cache: {}", key, cacheName);
                } else {
                    log.debug("Cache miss for key: {} in cache: {}", key, cacheName);
                }
                return value;
            }
        } catch (Exception e) {
            log.error("Error getting entry from cache: {} with key: {}", cacheName, key, e);
        }
        return null;
    }

    public boolean existsInCache(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                return cache.get(key) != null;
            }
        } catch (Exception e) {
            log.error("Error checking cache existence for key: {} in cache: {}", key, cacheName, e);
        }
        return false;
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredCacheEntries() {
        try {
            log.debug("Starting cache cleanup task");
            
            if (redisTemplate != null) {
                Set<String> keys = redisTemplate.keys("*");
                if (keys != null) {
                    long expiredCount = 0;
                    for (String key : keys) {
                        Long ttl = redisTemplate.getExpire(key);
                        if (ttl != null && ttl <= 0) {
                            redisTemplate.delete(key);
                            expiredCount++;
                        }
                    }
                    if (expiredCount > 0) {
                        log.info("Cleaned up {} expired cache entries", expiredCount);
                    }
                }
            }
            
            log.debug("Cache cleanup task completed");
        } catch (Exception e) {
            log.error("Error during cache cleanup", e);
        }
    }

    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void logCacheStatistics() {
        try {
            log.info("=== Cache Statistics ===");
            
            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                    com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = 
                        (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
                    
                    var stats = nativeCache.stats();
                    log.info("Cache: {} | Size: {} | Hit Rate: {:.2f}% | Miss Rate: {:.2f}% | Eviction Count: {}",
                            cacheName,
                            nativeCache.estimatedSize(),
                            stats.hitRate() * 100,
                            stats.missRate() * 100,
                            stats.evictionCount());
                }
            }
            
            log.info("=== End Cache Statistics ===");
        } catch (Exception e) {
            log.error("Error logging cache statistics", e);
        }
    }

    public void warmupCache(List<String> cacheNames) {
        log.info("Starting cache warmup for caches: {}", cacheNames);
        
        cacheNames.forEach(cacheName -> {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    log.debug("Warmed up cache: {}", cacheName);
                }
            } catch (Exception e) {
                log.error("Error warming up cache: {}", cacheName, e);
            }
        });
        
        log.info("Cache warmup completed");
    }

    public Duration getCacheTtl(String cacheName) {
        // Return different TTL based on cache type
        return switch (cacheName) {
            case "users" -> Duration.ofMinutes(30);
            case "userStats" -> Duration.ofMinutes(5);
            case "searchResults" -> Duration.ofMinutes(15);
            default -> Duration.ofSeconds(defaultTtl);
        };
    }
}
