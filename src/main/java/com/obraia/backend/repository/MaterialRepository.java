package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.catalog.Material;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findAllByOrderByNameAsc();
}
