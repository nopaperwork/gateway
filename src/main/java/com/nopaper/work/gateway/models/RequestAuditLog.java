/**
 * @package com.nopaper.work.gateway.models -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:18:37 pm
 * @git 
 */
package com.nopaper.work.gateway.models;

/**
 * 
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entity for storing request/response audit logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor	
@Table(name = "request_audit_log", schema = "way")
public class RequestAuditLog {
    
    @Id
    private Long id;
    
    private String requestId;
    private String routeId;
    private String method;
    private String path;
    private String queryParams;
    private String clientIp;
    private String userAgent;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseHeaders;
    private String responseBody;
    private Long processingTimeMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
