package com.obraia.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.AuthAuditLog;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {
}
