/**
 * @package com.nopaper.work.gateway.dto -> gateway
 * @author saikatbarman
 * @date 2025 26-Oct-2025 11:48:13 pm
 * @git 
 */
package com.nopaper.work.gateway.dto;

/**
 * 
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Serializable DTO for caching route definitions in Redis.
 * This avoids the NotSerializableException when caching Spring's RouteDefinition.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDefinitionDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String routeId;
    private String uri;
    private String path;
    private String method;
    private Integer rateLimitRequests;
    private Integer rateLimitPeriodSeconds;
}