package com.nopaper.work.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for Production API Gateway.
 * 
 * This gateway implements Azure API Management features:
 * - Dynamic routing from database
 * - Redis-based caching
 * - Per-route and per-client rate limiting
 * - IP blacklisting
 * - Response templating
 * - Global error handling
 * - Asynchronous request auditing
 * - Non-blocking logging
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class GatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
