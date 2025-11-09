/**
 * @package com.nopaper.work.gateway.service -> gateway
 * @author saikatbarman
 * @date 2025 27-Oct-2025 1:02:43 am
 * @git 
 */
package com.nopaper.work.gateway.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for managing rate limiting with Redis.
 * Provides additional rate limiting capabilities beyond built-in gateway features.
 */
@Slf4j
@Service
public class RateLimitService {
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    public RateLimitService(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check and increment rate limit counter for a given key.
     * 
     * @param key Rate limit key (e.g., "ip:route:user")
     * @param maxRequests Maximum requests allowed in the period
     * @param period Time period for rate limit
     * @return true if request is allowed, false if rate limit exceeded
     */
    public Mono<Boolean> checkRateLimit(String key, int maxRequests, Duration period) {
        String redisKey = "ratelimit:" + key;
        
        return redisTemplate.opsForValue()
            .increment(redisKey)
            .flatMap(count -> {
                if (count == 1) {
                    // First request, set expiration
                    return redisTemplate.expire(redisKey, period)
                        .thenReturn(true);
                }
                
                boolean allowed = count <= maxRequests;
                
                if (!allowed) {
                    log.warn("Rate limit exceeded for key: {}, count: {}, max: {}", 
                        key, count, maxRequests);
                }
                
                return Mono.just(allowed);
            })
            .onErrorResume(error -> {
                log.error("Error checking rate limit", error);
                // On error, allow request (fail open)
                return Mono.just(true);
            });
    }
    
    /**
     * Get current rate limit count for a key.
     */
    public Mono<Long> getCurrentCount(String key) {
        return redisTemplate.opsForValue()
            .get("ratelimit:" + key)
            .map(Long::parseLong)
            .defaultIfEmpty(0L);
    }
    
    /**
     * Reset rate limit for a key (useful for testing or manual intervention).
     */
    public Mono<Boolean> resetRateLimit(String key) {
        return redisTemplate.delete("ratelimit:" + key)
            .map(deleted -> deleted > 0);
    }
}
