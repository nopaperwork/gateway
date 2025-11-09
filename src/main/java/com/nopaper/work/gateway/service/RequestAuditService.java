/**
 * @package com.nopaper.work.gateway.service -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:35:46 pm
 * @git 
 */
package com.nopaper.work.gateway.service;

/*
 * 
 */

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nopaper.work.gateway.models.RequestAuditLog;
import com.nopaper.work.gateway.repositories.RequestAuditLogRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

/**
 * Asynchronous service for request auditing.
 * Logs are saved to database without blocking the gateway's reactive pipeline.
 */

@Slf4j
@Service
// @RequiredArgsConstructor
public class RequestAuditService {
    
    private final RequestAuditLogRepository auditLogRepository;
    
    private final ObjectMapper objectMapper;
    
    // No @Qualifier needed for ObjectMapper if there's only one bean
    public RequestAuditService(
            RequestAuditLogRepository auditLogRepository,
            ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Asynchronously save audit log to database.
     * This method returns immediately, allowing the gateway to continue processing.
     * 
     * @param auditLog The audit log entity to save
     * @return CompletableFuture that completes when save is done
     */
    @Async
    public CompletableFuture<Void> saveAuditLogAsync(RequestAuditLog auditLog) {
        log.debug("Saving audit log asynchronously for request: {}", auditLog.getRequestId());
        
        return auditLogRepository.save(auditLog)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess(saved -> log.debug("Audit log saved: {}", saved.getId()))
            .doOnError(error -> log.error("Failed to save audit log", error))
            .then()
            .toFuture();
    }
    
    /**
     * Helper method to cr	eate audit log builder with common fields.
     */
    public RequestAuditLog.RequestAuditLogBuilder createAuditLogBuilder(
            String requestId, String routeId, String method, String path) {
        return RequestAuditLog.builder()
            .requestId(requestId)
            .routeId(routeId)
            .method(method)
            .path(path)
            .createdAt(LocalDateTime.now());
    }
}
