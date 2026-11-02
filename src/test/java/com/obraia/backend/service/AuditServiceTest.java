package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.AuthAuditLog;
import com.obraia.backend.repository.AuthAuditLogRepository;
import com.obraia.backend.security.ClientInfo;

class AuditServiceTest {

    @Test
    void savesTheEventWithTheClientData() {
        AuthAuditLogRepository repository = mock(AuthAuditLogRepository.class);
        AuditService audit = new AuditService(repository);
        UUID userId = UUID.randomUUID();

        audit.record(AuditEvent.LOGIN_SUCCESS, userId, "ana@example.com", new ClientInfo("10.0.0.1", "Firefox"));
        audit.recordFailure(AuditEvent.LOGIN_FAILED, null, "x@example.com", new ClientInfo("10.0.0.2", "curl"));

        ArgumentCaptor<AuthAuditLog> saved = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(repository, times(2)).save(saved.capture());
        AuthAuditLog success = saved.getAllValues().get(0);
        assertEquals("login_success", success.getEventType());
        assertEquals(userId, success.getUserId());
        assertEquals("10.0.0.1", success.getIpAddress());
        assertEquals("Firefox", success.getUserAgent());
        assertEquals("login_failed", saved.getAllValues().get(1).getEventType());
    }
}
