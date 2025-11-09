/**
 * @package com.nopaper.work.gateway.dto -> gateway
 * @author saikatbarman
 * @date 2025 27-Oct-2025 1:28:51 am
 * @git 
 */
package com.nopaper.work.gateway.dto;

/**
 * 
 */

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serializable DTO for caching route definitions in Redis.
 * This avoids the NotSerializableException when caching Spring's RouteDefinition.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
// Make sure to implement Serializable for extra compatibility
public class CustomRouteDefinitionDTO implements Serializable {

    private static final long serialVersionUID = -7344844061060713021L;
	private String routeId;
    private String uri;
    private String path;
    private String method;
    private boolean enabled;
    private Integer rateLimitRequests;
    private Integer rateLimitPeriodSeconds;

    // Complex types
    private List<String> allowedMethods;
    private Map<String, String> metadata; // e.g. description, owner, tags
    private List<CustomFilterDTO> filters;   // Nested complex object
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Standard getters and setters
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getRateLimitRequests() { return rateLimitRequests; }
    public void setRateLimitRequests(Integer rateLimitRequests) { this.rateLimitRequests = rateLimitRequests; }

    public Integer getRateLimitPeriodSeconds() { return rateLimitPeriodSeconds; }
    public void setRateLimitPeriodSeconds(Integer rateLimitPeriodSeconds) { this.rateLimitPeriodSeconds = rateLimitPeriodSeconds; }

    public List<String> getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public List<CustomFilterDTO> getFilters() { return filters; }
    public void setFilters(List<CustomFilterDTO> filters) { this.filters = filters; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
