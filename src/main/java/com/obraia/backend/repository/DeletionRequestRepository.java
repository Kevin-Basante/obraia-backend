package com.obraia.backend.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.project.DeletionRequest;

public interface DeletionRequestRepository extends JpaRepository<DeletionRequest, UUID> {

    Optional<DeletionRequest> findByTokenHash(String tokenHash);

    /** A new link replaces the previous ones for the same target. */
    @Modifying
    @Query("""
            DELETE FROM DeletionRequest d
            WHERE d.targetType = :targetType AND d.targetId = :targetId AND d.confirmedAt IS NULL
            """)
    int deletePending(@Param("targetType") String targetType, @Param("targetId") UUID targetId);

    @Modifying
    @Query("DELETE FROM DeletionRequest d WHERE d.expiresAt < :now AND d.confirmedAt IS NULL")
    int deleteExpired(@Param("now") Instant now);
}
