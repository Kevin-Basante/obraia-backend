package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.project.SiteManager;

/** Every query filters by the user who saved the contact. */
public interface SiteManagerRepository extends JpaRepository<SiteManager, UUID> {

    List<SiteManager> findByCreatedByOrderByFullNameAsc(UUID createdBy);

    Optional<SiteManager> findByIdAndCreatedBy(UUID id, UUID createdBy);
}
