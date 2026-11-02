package com.obraia.backend.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.User;

/** Spring Data generates the SQL from the method names. Deleted accounts are ignored. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Removes for good the accounts deleted before the limit. An account that
     * still has a project or a company is skipped (ON DELETE RESTRICT); its
     * projects are purged first, so it goes on a later run.
     */
    @Modifying
    @Query(value = """
            DELETE FROM users u
            WHERE u.deleted_at < :limit
              AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.created_by = u.id)
              AND NOT EXISTS (SELECT 1 FROM companies c WHERE c.created_by = u.id)
            """, nativeQuery = true)
    int purgeDeletedBefore(@Param("limit") Instant limit);
}
