package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.catalog.ProjectType;

public interface ProjectTypeRepository extends JpaRepository<ProjectType, UUID> {

    List<ProjectType> findAllByOrderByNameAsc();
}
