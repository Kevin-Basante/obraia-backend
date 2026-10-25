package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.catalog.Location;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findAllByOrderByCityAsc();
}
