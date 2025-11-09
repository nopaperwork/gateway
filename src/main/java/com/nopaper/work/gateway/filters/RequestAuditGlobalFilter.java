/**
 * @package com.nopaper.work.gateway.filters -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:36:36 pm
 * @git 
 */
package com.nopaper.work.gateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nopaper.work.gateway.models.RequestAuditLog;
import com.nopaper.work.gateway.service.RequestAuditService;
import com.nopaper.work.gateway.utils.IpAddressExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global filter for request/response auditing.
 * 
 * Features:
 * - Captures request and response details
 * - Logs asynchronously to database (non-blocking)
 * - Tracks processing time
 * - Captures errors
 * - Configurable body logging
 * 
 * Filter Order: Integer.MIN_VALUE (Execute first to capture full request lifecycle)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestAuditGlobalFilter implements GlobalFilter, Ordered {
    
    private final RequestAuditService auditService;
    private final ObjectMapper objectMapper;
    
    @Value("${gateway.audit.enabled:true}")
    private boolean auditEnabled;
    
    @Value("${gateway.audit.log-request-body:false}")
    private boolean logRequestBody;
    
    @Value("${gateway.audit.log-response-body:false}")
    private boolean logResponseBody;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!auditEnabled) {
            return chain.filter(exchange);
        }
        
        // Generate unique request ID
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        // Extract request details
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String queryParams = request.getQueryParams().toString();
        String clientIp = IpAddressExtractor.extractClientIp(exchange);
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        
        // Get route ID if available
        String routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR);
        
        // Capture request headers
        String requestHeaders = captureHeaders(request.getHeaders());
        
        // Create decorated request and response for body capture
        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(request);
        ServerHttpResponseDecorator responseDecorator = decorateResponse(
            exchange, requestId, routeId, method, path, queryParams, 
            clientIp, userAgent, requestHeaders, startTime
        );
        
        // Continue with decorated exchange
        return chain.filter(
            exchange.mutate()
                .request(requestDecorator)
                .response(responseDecorator)
                .build()
        ).doOnError(error -> {
            // Log error asynchronously
            long processingTime = System.currentTimeMillis() - startTime;
            logAuditWithError(
                requestId, routeId, method, path, queryParams,
                clientIp, userAgent, requestHeaders, processingTime, error
            );
        });
    }
    
    /**
     * Decorate response to capture response details.
     */
    private ServerHttpResponseDecorator decorateResponse(
            ServerWebExchange exchange, String requestId, String routeId,
            String method, String path, String queryParams, String clientIp,
            String userAgent, String requestHeaders, long startTime) {
        
        ServerHttpResponse originalResponse = exchange.getResponse();
        
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    
                    return super.writeWith(fluxBody
                        .collectList()
                        .flatMapMany(dataBuffers -> {
                            // Capture response body if enabled
                            String responseBody = null;
                            if (logResponseBody && !dataBuffers.isEmpty()) {
                                responseBody = dataBuffers.stream()
                                    .map(buffer -> {
                                        byte[] bytes = new byte[buffer.readableByteCount()];
                                        buffer.read(bytes);
                                        DataBufferUtils.release(buffer);
                                        return new String(bytes, StandardCharsets.UTF_8);
                                    })
                                    .collect(Collectors.joining());
                            }
                            
                            // Calculate processing time
                            long processingTime = System.currentTimeMillis() - startTime;
                            
                            // Capture response details
                            Integer responseStatus = getDelegate().getStatusCode() != null 
                                ? getDelegate().getStatusCode().value() 
                                : null;
                            String responseHeaders = captureHeaders(getDelegate().getHeaders());
                            
                            // Log audit asynchronously
                            logAudit(
                                requestId, routeId, method, path, queryParams,
                                clientIp, userAgent, requestHeaders, null,
                                responseStatus, responseHeaders, responseBody, processingTime
                            );
                            
                            // Return data buffers for actual response
                            return Flux.fromIterable(dataBuffers);
                        })
                    );
                }
                
                return super.writeWith(body);
            }
        };
    }
    
    /**
     * Log audit entry asynchronously.
     */
    private void logAudit(
            String requestId, String routeId, String method, String path,
            String queryParams, String clientIp, String userAgent,
            String requestHeaders, String requestBody, Integer responseStatus,
            String responseHeaders, String responseBody, Long processingTime) {
        
        RequestAuditLog auditLog = RequestAuditLog.builder()
            .requestId(requestId)
            .routeId(routeId)
            .method(method)
            .path(path)
            .queryParams(queryParams)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .requestHeaders(requestHeaders)
            .requestBody(requestBody)
            .responseStatus(responseStatus)
            .responseHeaders(responseHeaders)
            .responseBody(responseBody)
            .processingTimeMs(processingTime)
            .build();
        
        // Save asynchronously (non-blocking)
        auditService.saveAuditLogAsync(auditLog)
            .exceptionally(error -> {
                log.error("Failed to save audit log for request: {}", requestId, error);
                return null;
            });
    }
    
    /**
     * Log audit entry with error.
     */
    private void logAuditWithError(
            String requestId, String routeId, String method, String path,
            String queryParams, String clientIp, String userAgent,
            String requestHeaders, Long processingTime, Throwable error) {
        
        RequestAuditLog auditLog = RequestAuditLog.builder()
            .requestId(requestId)
            .routeId(routeId)
            .method(method)
            .path(path)
            .queryParams(queryParams)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .requestHeaders(requestHeaders)
            .processingTimeMs(processingTime)
            .errorMessage(error.getMessage())
            .responseStatus(500)
            .build();
        
        auditService.saveAuditLogAsync(auditLog)
            .exceptionally(saveError -> {
                log.error("Failed to save error audit log for request: {}", requestId, saveError);
                return null;
            });
    }
    
    /**
     * Capture headers as JSON string.
     */
    private String captureHeaders(HttpHeaders headers) {
        try {
            Map<String, String> headerMap = new HashMap<>();
            headers.forEach((key, values) -> {
                // Skip sensitive headers
                if (!isSensitiveHeader(key)) {
                    headerMap.put(key, String.join(",", values));
                }
            });
            return objectMapper.writeValueAsString(headerMap);
        } catch (Exception e) {
            log.warn("Failed to serialize headers", e);
            return "{}";
        }
    }
    
    /**
     * Check if header is sensitive (should not be logged).
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") ||
               lowerName.contains("password") ||
               lowerName.contains("token") ||
               lowerName.contains("secret") ||
               lowerName.contains("api-key");
    }
    
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE; // Execute first
    }
}
