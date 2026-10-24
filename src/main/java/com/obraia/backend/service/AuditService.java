package com.obraia.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.AuthAuditLog;
import com.obraia.backend.repository.AuthAuditLogRepository;
import com.obraia.backend.security.ClientInfo;

/** Saves the security log (auth_audit_logs). */
@Service
public class AuditService {

    private final AuthAuditLogRepository repository;

    public AuditService(AuthAuditLogRepository repository) {
        this.repository = repository;
    }

    /** Saves the event in the current transaction: if the operation fails, the event is not saved either. */
    @Transactional
    public void record(AuditEvent event, UUID userId, String email, ClientInfo client) {
        save(event, userId, email, client);
    }

    /**
     * Saves the event in its own transaction. Used for failed attempts: the
     * operation throws an exception and rolls back, but the attempt must stay.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(AuditEvent event, UUID userId, String email, ClientInfo client) {
        save(event, userId, email, client);
    }

    private void save(AuditEvent event, UUID userId, String email, ClientInfo client) {
        repository.save(new AuthAuditLog(event, userId, email, client.ipAddress(), client.userAgent()));
    }
}
