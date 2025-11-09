/**
 * @package com.nopaper.work.gateway.service -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:31:07 pm
 * @git 
 */
package com.nopaper.work.gateway.service;

/**
 * 
 */

import java.time.LocalDateTime;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nopaper.work.gateway.repositories.IpBlacklistRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for IP blacklist management with Redis caching.
 * Implements Azure API Gateway's IP filtering capabilities.
 */
@Slf4j
@Service
// @RequiredArgsConstructor
public class IpBlacklistService {
    
    private final IpBlacklistRepository blacklistRepository;
    
 // No Redis template needed - uses @Cacheable // REPLACING @RequiredArgsConstructor
    public IpBlacklistService(IpBlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }
    
    /**
     * Check if an IP address is blacklisted.
     * Results are cached in Redis for performance.
     * 
     * @param ipAddress The IP address to check
     * @return true if blacklisted and not expired, false otherwise
     */
    @Cacheable(value = "ip-blacklist", key = "#ipAddress")
    public Mono<Boolean> isBlacklisted(String ipAddress) {
        log.debug("Checking if IP {} is blacklisted", ipAddress);
        
        return blacklistRepository.findByIpAddress(ipAddress)
            .map(blacklist -> {
                // Check if blacklist entry is still valid
                if (blacklist.getExpiresAt() == null) {
                    return true; // Permanently blacklisted
                }
                return blacklist.getExpiresAt().isAfter(LocalDateTime.now());
            })
            .defaultIfEmpty(false)
            .doOnNext(isBlacklisted -> {
                if (isBlacklisted) {
                    log.warn("IP {} is blacklisted", ipAddress);
                }
            });
    }
}
