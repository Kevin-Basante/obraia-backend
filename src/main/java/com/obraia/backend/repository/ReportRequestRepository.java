package com.obraia.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.report.ReportRequest;

public interface ReportRequestRepository extends JpaRepository<ReportRequest, UUID> {

    Optional<ReportRequest> findByTokenHash(String tokenHash);

    /** The newest request of a member for a project. */
    Optional<ReportRequest> findFirstByProjectIdAndRequestedByOrderByCreatedAtDesc(UUID projectId, UUID requestedBy);

    /** A new request replaces the pending ones of the same member. */
    @Modifying
    @Query("""
            DELETE FROM ReportRequest r
            WHERE r.projectId = :projectId AND r.requestedBy = :requestedBy AND r.status = 'pending'
            """)
    int deletePending(@Param("projectId") UUID projectId, @Param("requestedBy") UUID requestedBy);
}
