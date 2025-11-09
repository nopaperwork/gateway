/**
 * @package com.nopaper.work.gateway.service -> gateway
 * @author saikatbarman
 * @date 2025 26-Oct-2025 11:58:05 pm
 * @git 
 */
package com.nopaper.work.gateway.service;

/**
 * 
 */

import org.springframework.stereotype.Service;

import com.nopaper.work.gateway.dto.RouteDefinitionDTO;
import com.nopaper.work.gateway.models.GatewayRoute;
import com.nopaper.work.gateway.repositories.GatewayRouteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Service for managing gateway routes with Redis caching.
 * 
 * IMPORTANT: We cache DTOs (not RouteDefinition objects) to avoid serialization issues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    
    private final GatewayRouteRepository routeRepository;
    
    /**
     * Get all enabled routes from database with Redis caching.
     * Returns DTOs which are Serializable, avoiding NotSerializableException.
     */
    public Flux<RouteDefinitionDTO> getEnabledRoutes() {
        log.debug("Fetching enabled routes from database");
        return routeRepository.findByEnabledTrue()
            .map(this::toDTO);
    }
    
    /**
     * Convert database entity to cacheable DTO.
     */
    private RouteDefinitionDTO toDTO(GatewayRoute entity) {
        return new RouteDefinitionDTO(
            entity.getRouteId(),
            entity.getUri(),
            entity.getPath(),
            entity.getMethod(),
            entity.getRateLimitRequests(),
            entity.getRateLimitPeriodSeconds()
        );
    }
}
