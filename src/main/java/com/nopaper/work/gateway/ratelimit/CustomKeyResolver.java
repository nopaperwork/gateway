/**
 * @package com.nopaper.work.gateway.ratelimit -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:25:26 pm
 * @git 
 */
package com.nopaper.work.gateway.ratelimit;

/**
 * 
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom KeyResolver for rate limiting.
 * 
 * Implements Azure API Gateway's rate-limit-by-key pattern with composite keys:
 * - Client IP address
 * - Route/API path
 * - User identifier (if authenticated)
 * 
 * This allows for flexible rate limiting strategies:
 * 1. Per-client rate limiting (prevent single client abuse)
 * 2. Per-route rate limiting (protect specific APIs)
 * 3. Combination of both for granular control
 */
@Slf4j
@Component("customKeyResolver")
public class CustomKeyResolver implements KeyResolver {
    
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        // Extract client IP
        String clientIp = extractClientIp(exchange);
        
        // Extract route ID (API path)
        String routeId = exchange.getAttribute(
            org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR
        );
        if (routeId == null) {
            routeId = "unknown-route";
        }
        
        // Extract user identifier if authenticated (for future enhancement)
        String userId = extractUserId(exchange);
        
        // Build composite key for rate limiting
        // Format: ip:route:user
        String key = String.format("%s:%s:%s", 
            clientIp, 
            routeId, 
            userId != null ? userId : "anonymous"
        );
        
        log.debug("Rate limit key resolved: {}", key);
        
        return Mono.just(key);
    }
    
    /**
     * Extract client IP from request headers or remote address.
     * Handles proxy headers like X-Forwarded-For.
     */
    private String extractClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For header first (for proxied requests)
        String xForwardedFor = exchange.getRequest()
            .getHeaders()
            .getFirst("X-Forwarded-For");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take first IP in chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = exchange.getRequest()
            .getHeaders()
            .getFirst("X-Real-IP");
        
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fall back to remote address
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
        }
        
        return "unknown";
    }
    
    /**
     * Extract user ID from authentication context.
     * This is a placeholder for future authentication integration.
     */
    private String extractUserId(ServerWebExchange exchange) {
        // TODO: Extract from JWT token or session when authentication is implemented
        return null;
    }
}
