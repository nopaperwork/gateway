/**
 * @package com.nopaper.work.gateway.utils -> gateway
 * @author saikatbarman
 * @date 2025 27-Oct-2025 1:05:56 am
 * @git 
 */
package com.nopaper.work.gateway.utils;

/**
 * 
 */

import org.springframework.web.server.ServerWebExchange;

/**
 * Utility class for extracting client IP addresses from requests.
 * Handles various proxy headers and edge cases.
 */
public class IpAddressExtractor {
    
    /**
     * Extract client IP address from request.
     * Checks proxy headers in order: X-Forwarded-For, X-Real-IP, remote address.
     */
    public static String extractClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For (standard for proxies/load balancers)
        String xForwardedFor = exchange.getRequest()
            .getHeaders()
            .getFirst("X-Forwarded-For");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
            // Take the first one (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP (alternative proxy header)
        String xRealIp = exchange.getRequest()
            .getHeaders()
            .getFirst("X-Real-IP");
        
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        // Check Forwarded header (RFC 7239)
        String forwarded = exchange.getRequest()
            .getHeaders()
            .getFirst("Forwarded");
        
        if (forwarded != null && !forwarded.isEmpty()) {
            // Parse "for=ip" format
            String[] parts = forwarded.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("for=")) {
                    return part.substring(4).trim().replaceAll("\"", "");
                }
            }
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
}

