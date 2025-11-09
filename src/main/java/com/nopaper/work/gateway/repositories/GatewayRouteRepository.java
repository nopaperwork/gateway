/**
 * @package com.nopaper.work.gateway.repositories -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:19:35 pm
 * @git 
 */
package com.nopaper.work.gateway.repositories;

/**
 * Reactive repository for gateway routes.
 */

import com.nopaper.work.gateway.models.GatewayRoute;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface GatewayRouteRepository extends ReactiveCrudRepository<GatewayRoute, Long> {
	/**
     * Find all enabled routes for active routing.
     */
    Flux<GatewayRoute> findByEnabledTrue();
}