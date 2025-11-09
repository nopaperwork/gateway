/**
 * @package com.nopaper.work.gateway.repositories -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:20:31 pm
 * @git 
 */
package com.nopaper.work.gateway.repositories;

import java.time.LocalDateTime;

/**
 * Reactive repository for IP blacklist management.
 */

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.nopaper.work.gateway.models.IpBlacklist;

import reactor.core.publisher.Mono;

public interface IpBlacklistRepository extends ReactiveCrudRepository<IpBlacklist, Long> {
	/**
     * Find a blacklist entry by IP address.
     */
    Mono<IpBlacklist> findByIpAddress(String ipAddress);
    Mono<Void> deleteByExpiresAtBefore(LocalDateTime dateTime);
}