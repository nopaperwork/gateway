/**
 * @package com.nopaper.work.gateway.filters -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:31:56 pm
 * @git 
 */
package com.nopaper.work.gateway.filters;

/**
 * 
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nopaper.work.gateway.service.IpBlacklistService;
import com.nopaper.work.gateway.utils.IpAddressExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global filter for IP blacklisting.
 * 
 * Implements Azure API Gateway's IP filtering capabilities with:
 * - Database-driven blacklist
 * - Redis caching for performance
 * - Expiration support for temporary bans
 * 
 * Filter Order: -100 (Execute early in the filter chain)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlacklistGlobalFilter implements GlobalFilter, Ordered {
    
    private final IpBlacklistService blacklistService;
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract client IP address
        String clientIp = IpAddressExtractor.extractClientIp(exchange);
        
        log.debug("Checking IP blacklist for: {}", clientIp);
        
        // Check if IP is blacklisted (cached in Redis)
        return blacklistService.isBlacklisted(clientIp)
            .flatMap(isBlacklisted -> {
                if (isBlacklisted) {
                    log.warn("Blocked request from blacklisted IP: {}", clientIp);
                    return rejectRequest(exchange, clientIp);
                }
                
                // IP is not blacklisted, continue with filter chain
                return chain.filter(exchange);
            })
            .onErrorResume(error -> {
                // On error checking blacklist, allow request (fail open)
                log.error("Error checking IP blacklist, allowing request", error);
                return chain.filter(exchange);
            });
    }
    
    /**
     * Reject request with 403 Forbidden response.
     */
    private Mono<Void> rejectRequest(ServerWebExchange exchange, String clientIp) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // Build error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.FORBIDDEN.value());
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", "Access denied: IP address is blacklisted");
        errorResponse.put("path", exchange.getRequest().getPath().value());
        errorResponse.put("clientIp", clientIp);
        
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating error response", e);
            return exchange.getResponse().setComplete();
        }
    }
    
    @Override
    public int getOrder() {
        return -100; // Execute early, before routing
    }
}
