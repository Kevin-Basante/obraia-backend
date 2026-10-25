package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.config.CacheConfig;
import com.obraia.backend.entity.catalog.ProjectType;

public interface ProjectTypeRepository extends JpaRepository<ProjectType, UUID> {

    @Cacheable(CacheConfig.PROJECT_TYPES)
    List<ProjectType> findAllByOrderByNameAsc();
}
