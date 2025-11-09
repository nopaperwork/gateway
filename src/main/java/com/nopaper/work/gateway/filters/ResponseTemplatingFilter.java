/**
 * @package com.nopaper.work.gateway.filters -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:32:51 pm
 * @git 
 */
package com.nopaper.work.gateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global filter for response templating.
 * 
 * Implements standard response format with:
 * - Timestamp
 * - Status code
 * - Success indicator
 * - Data payload
 * - Metadata (request ID, path, etc.)
 * 
 * Response format can be customized via X-Response-Format header:
 * - "standard" (default): Standard wrapped format
 * - "raw": Original response without wrapping
 * 
 * Filter Order: -50 (Execute after routing but before logging)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseTemplatingFilter implements GlobalFilter, Ordered {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Check if response templating is requested
        String responseFormat = exchange.getRequest()
            .getHeaders()
            .getFirst("X-Response-Format");
        
        // Default to standard format
        if (responseFormat == null || responseFormat.isEmpty()) {
            responseFormat = "standard";
        }
        
        // If raw format requested, skip templating
        if ("raw".equalsIgnoreCase(responseFormat)) {
            return chain.filter(exchange);
        }
        
        // Apply response templating
        return chain.filter(exchange.mutate()
            .response(decorateResponse(exchange))
            .build());
    }
    
    /**
     * Decorate the response to wrap it in standard format.
     */
    private ServerHttpResponse decorateResponse(ServerWebExchange exchange) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    
                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        // Combine all data buffers
                        DataBufferFactory factory = new DefaultDataBufferFactory();
                        DataBuffer join = factory.join(dataBuffers);
                        
                        byte[] content = new byte[join.readableByteCount()];
                        join.read(content);
                        DataBufferUtils.release(join);
                        
                        // Parse original response
                        String originalBody = new String(content, StandardCharsets.UTF_8);
                        
                        // Wrap in standard format
                        Map<String, Object> wrappedResponse = createStandardResponse(
                            exchange, originalBody, getDelegate().getStatusCode().value()
                        );
                        
                        try {
                            String wrappedJson = objectMapper.writeValueAsString(wrappedResponse);
                            byte[] wrappedBytes = wrappedJson.getBytes(StandardCharsets.UTF_8);
                            
                            // Update Content-Type and Content-Length
                            getDelegate().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            getDelegate().getHeaders().setContentLength(wrappedBytes.length);
                            
                            return bufferFactory.wrap(wrappedBytes);
                        } catch (Exception e) {
                            log.error("Error wrapping response", e);
                            return bufferFactory.wrap(content);
                        }
                    }));
                }
                
                return super.writeWith(body);
            }
        };
    }
    
    /**
     * Create standard response format.
     */
    private Map<String, Object> createStandardResponse(
            ServerWebExchange exchange, String originalBody, int statusCode) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", statusCode);
        response.put("success", statusCode >= 200 && statusCode < 300);
        
        // Parse original body if JSON
        Object dataPayload;
        try {
            dataPayload = objectMapper.readTree(originalBody);
        } catch (Exception e) {
            dataPayload = originalBody;
        }
        response.put("data", dataPayload);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("requestId", exchange.getRequest().getId());
        metadata.put("path", exchange.getRequest().getPath().value());
        metadata.put("method", exchange.getRequest().getMethod().name());
        response.put("metadata", metadata);
        
        return response;
    }
    
    @Override
    public int getOrder() {
        return -50;
    }
}
