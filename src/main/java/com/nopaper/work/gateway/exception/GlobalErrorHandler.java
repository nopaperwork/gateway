/**
 * @package com.nopaper.work.gateway.exception -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:34:31 pm
 * @git 
 */
package com.nopaper.work.gateway.exception;

/**
 * 
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global error handler for the API Gateway.
 * 
 * Implements centralized error handling with:
 * - Consistent error response format
 * - Detailed error information
 * - Appropriate HTTP status codes
 * - Request context (ID, path, etc.)
 * 
 * Order: -2 (High priority to catch all errors)
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("Error handling request: {} {}", 
            exchange.getRequest().getMethod(), 
            exchange.getRequest().getPath(), 
            ex);
        
        // Determine appropriate status code and message
        ErrorResponse errorResponse = buildErrorResponse(exchange, ex);
        
        // Set response status
        exchange.getResponse().setStatusCode(errorResponse.getHttpStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // Serialize error response
        String errorJson;
        try {
            errorJson = objectMapper.writeValueAsString(errorResponse.getBody());
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            errorJson = "{\"error\":\"Internal server error\"}";
        }
        
        // Write error response
        DataBuffer buffer = exchange.getResponse()
            .bufferFactory()
            .wrap(errorJson.getBytes(StandardCharsets.UTF_8));
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
    
    /**
     * Build error response based on exception type.
     */
    private ErrorResponse buildErrorResponse(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String message;
        String errorCode;
        String errorType;
        
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            errorCode = "GATEWAY_" + status.name();
            errorType = "ResponseStatusException";
            
        } else if (ex instanceof java.net.ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service temporarily unavailable";
            errorCode = "SERVICE_UNAVAILABLE";
            errorType = "ConnectException";
            
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Request timeout";
            errorCode = "GATEWAY_TIMEOUT";
            errorType = "TimeoutException";
            
        } else if (ex instanceof io.netty.handler.timeout.ReadTimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Read timeout from downstream service";
            errorCode = "READ_TIMEOUT";
            errorType = "ReadTimeoutException";
            
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Internal server error";
            errorCode = "INTERNAL_ERROR";
            errorType = ex.getClass().getSimpleName();
        }
        
        // Build error body
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now());
        errorBody.put("status", status.value());
        errorBody.put("error", status.getReasonPhrase());
        errorBody.put("message", message);
        errorBody.put("errorCode", errorCode);
        errorBody.put("errorType", errorType);
        errorBody.put("path", exchange.getRequest().getPath().value());
        errorBody.put("requestId", exchange.getRequest().getId());
        errorBody.put("method", exchange.getRequest().getMethod().name());
        
        return new ErrorResponse(status, errorBody);
    }
    
    /**
     * Inner class to hold error response data.
     */
    private static class ErrorResponse {
        private final HttpStatus httpStatus;
        private final Map<String, Object> body;
        
        public ErrorResponse(HttpStatus httpStatus, Map<String, Object> body) {
            this.httpStatus = httpStatus;
            this.body = body;
        }
        
        public HttpStatus getHttpStatus() {
            return httpStatus;
        }
        
        public Map<String, Object> getBody() {
            return body;
        }
    }
}
