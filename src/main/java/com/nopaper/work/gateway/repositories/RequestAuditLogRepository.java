/**
 * @package com.nopaper.work.gateway.repositories -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:21:02 pm
 * @git 
 */
package com.nopaper.work.gateway.repositories;

/**
 * Reactive repository for request audit logging.
 */

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.nopaper.work.gateway.models.RequestAuditLog;

public interface RequestAuditLogRepository extends ReactiveCrudRepository<RequestAuditLog, Long> {
}