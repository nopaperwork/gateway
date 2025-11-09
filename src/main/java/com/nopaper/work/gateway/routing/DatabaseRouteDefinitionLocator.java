/**
 * @package com.nopaper.work.gateway.routing -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:23:05 pm
 * @git 
 */
package com.nopaper.work.gateway.routing;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function; // Import Function

import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Component;

import com.nopaper.work.gateway.dto.RouteDefinitionDTO;
import com.nopaper.work.gateway.service.RouteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Custom RouteDefinitionLocator that loads routes dynamically from PostgreSQL.
 * Routes are cached in the service layer using Redis for high performance.
 * 
 * This implements Azure API Gateway's dynamic routing capabilities where routes
 * can be modified without restarting the gateway.
 */

/**
 * Custom RouteDefinitionLocator that loads routes dynamically.
 * Routes are loaded from the RouteService, which implements a "Redis-First"
 * caching strategy.
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseRouteDefinitionLocator implements RouteDefinitionLocator {
    
    private final RouteService routeService;
    
    /**
     * Load route definitions from database.
     * This method is called by Spring Cloud Gateway to discover routes.
     * It fetches the routes from the RouteService, which serves from the Redis cache.
     * @return Flux of RouteDefinition objects
     */
    
    /*
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        log.info("Loading route definitions from database");
        
        return routeService.getEnabledRoutes()
            .map(this::convertToRouteDefinition)
            .doOnNext(route -> log.debug("Loaded route: {}", route.getId()))
            .doOnComplete(() -> log.info("Finished loading route definitions"))
            .doOnError(error -> log.error("Error loading route definitions", error));
    }
    */
    
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        log.info("Loading route definitions from service (Redis-First)");
        
        return routeService.getEnabledRoutes()
            .flatMapIterable(Function.identity()) // Unpack the Mono<List<DTO>> into a Flux<DTO>
            .map(this::convertToRouteDefinition)
            .doOnNext(route -> log.debug("Loaded route: {}", route.getId()))
            .doOnComplete(() -> log.info("Finished loading route definitions"))
            .doOnError(error -> log.error("Error loading route definitions", error));
    }
    
    /**
     * Convert DTO to Spring Cloud Gateway RouteDefinition.
     * Adds predicates (path, method) and filters (rate limiting).
     */
    private RouteDefinition convertToRouteDefinition(RouteDefinitionDTO dto) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(dto.getRouteId());
        definition.setUri(URI.create(dto.getUri()));
        
        // Build predicates
        List<PredicateDefinition> predicates = new ArrayList<>();
        
        // Path predicate (required)
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", dto.getPath());
        predicates.add(pathPredicate);
        
        // Method predicate (optional)
        if (dto.getMethod() != null && !dto.getMethod().isEmpty()) {
            PredicateDefinition methodPredicate = new PredicateDefinition();
            methodPredicate.setName("Method");
            methodPredicate.addArg("methods", dto.getMethod());
            predicates.add(methodPredicate);
        }
        
        definition.setPredicates(predicates);
        
        // Build filters
        List<FilterDefinition> filters = new ArrayList<>();
        
        // Add rate limiting filter with route-specific configuration
        FilterDefinition rateLimitFilter = createRateLimitFilter(dto);
        filters.add(rateLimitFilter);
        
        // Add StripPrefix filter to remove /api prefix if needed
        FilterDefinition stripPrefixFilter = new FilterDefinition();
        stripPrefixFilter.setName("StripPrefix");
        stripPrefixFilter.addArg("parts", "0");
        filters.add(stripPrefixFilter);
        
        definition.setFilters(filters);
        definition.setOrder(1);
        
        return definition;
    }
    
    /**
     * Create rate limiting filter with dynamic configuration from database.
     * This implements Azure API Gateway's rate limiting by key pattern.
     */
    private FilterDefinition createRateLimitFilter(RouteDefinitionDTO dto) {
        FilterDefinition rateLimitFilter = new FilterDefinition();
        rateLimitFilter.setName("RequestRateLimiter");
        
        Map<String, String> args = new HashMap<>();
        
        // Calculate replenish rate (requests per second)
        int replenishRate = dto.getRateLimitRequests() / dto.getRateLimitPeriodSeconds();
        if (replenishRate < 1) replenishRate = 1;
        
        args.put("redis-rate-limiter.replenishRate", String.valueOf(replenishRate));
        args.put("redis-rate-limiter.burstCapacity", String.valueOf(dto.getRateLimitRequests()));
        args.put("redis-rate-limiter.requestedTokens", "1");
        
        // Use custom key resolver bean
        args.put("key-resolver", "#{@customKeyResolver}");
        
        rateLimitFilter.setArgs(args);
        
        return rateLimitFilter;
    }
    
    /**
     * Convert DTO to Spring Cloud Gateway RouteDefinition.
     * Adds predicates (path, method) and filters (rate limiting).
     */
    
    /* OLD
     * 
     */
    /*
     * private RouteDefinition convertToRouteDefinition(RouteDefinitionDTO dto) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(dto.getRouteId());
        definition.setUri(URI.create(dto.getUri()));
        
        // Build predicates
        List<PredicateDefinition> predicates = new ArrayList<>();
        
        // Path predicate (required)
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        pathPredicate.addArg("pattern", dto.getPath());
        predicates.add(pathPredicate);
        
        // Method predicate (optional)
        if (dto.getMethod() != null && !dto.getMethod().isEmpty()) {
            PredicateDefinition methodPredicate = new PredicateDefinition();
            methodPredicate.setName("Method");
            methodPredicate.addArg("methods", dto.getMethod());
            predicates.add(methodPredicate);
        }
        
        definition.setPredicates(predicates);
        
        // Build filters
        List<FilterDefinition> filters = new ArrayList<>();
        
        // Add rate limiting filter with route-specific configuration
        FilterDefinition rateLimitFilter = createRateLimitFilter(dto);
        filters.add(rateLimitFilter);
        
        // Add StripPrefix filter to remove /api prefix if needed
        FilterDefinition stripPrefixFilter = new FilterDefinition();
        stripPrefixFilter.setName("StripPrefix");
        stripPrefixFilter.addArg("parts", "0");
        filters.add(stripPrefixFilter);
        
        definition.setFilters(filters);
        definition.setOrder(1);
        
        return definition;
    } */
    
    /**
     * Create rate limiting filter with dynamic configuration from database.
     * This implements Azure API Gateway's rate limiting by key pattern.
     */
    /*
    private FilterDefinition createRateLimitFilter(RouteDefinitionDTO dto) {
        FilterDefinition rateLimitFilter = new FilterDefinition();
        rateLimitFilter.setName("RequestRateLimiter");
        
        Map<String, String> args = new HashMap<>();
        
        // Calculate replenish rate (requests per second)
        int replenishRate = dto.getRateLimitRequests() / dto.getRateLimitPeriodSeconds();
        if (replenishRate < 1) replenishRate = 1;
        
        args.put("redis-rate-limiter.replenishRate", String.valueOf(replenishRate));
        args.put("redis-rate-limiter.burstCapacity", String.valueOf(dto.getRateLimitRequests()));
        args.put("redis-rate-limiter.requestedTokens", "1");
        
        // Use custom key resolver bean
        args.put("key-resolver", "#{@customKeyResolver}");
        
        rateLimitFilter.setArgs(args);
        
        return rateLimitFilter;
    }
    */
}
