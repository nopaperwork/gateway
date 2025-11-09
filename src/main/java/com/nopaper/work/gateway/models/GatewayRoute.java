/**
 * @package com.nopaper.work.gateway.models -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 1:13:32 am
 * @git 
 */
package com.nopaper.work.gateway.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Database entity representing a gateway route configuration.
 * This entity stores the routing rules that are dynamically loaded from PostgreSQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gateway_routes", schema = "way")
public class GatewayRoute { // extends AbstractAuditEntity
    
	@Id
    private Long id;
    
    private String routeId;
    private String uri;
    private String path;
    private String method;
    private Boolean enabled;
    private Integer rateLimitRequests;
    private Integer rateLimitPeriodSeconds;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}