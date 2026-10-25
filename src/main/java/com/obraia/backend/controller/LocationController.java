package com.obraia.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.catalog.LocationResponse;
import com.obraia.backend.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Public catalog of cities. */
@RestController
@RequestMapping("/api/catalogs/locations")
@Tag(name = "Catalogs")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "List the cities")
    public ResponseEntity<List<LocationResponse>> list(
            @Parameter(description = "Only cities of this department", example = "Antioquia")
            @RequestParam(required = false) String department,
            @Parameter(description = "Part of the city name; accents are optional", example = "bogota")
            @RequestParam(required = false) String search) {
        return CatalogResponses.cached(locationService.listLocations(department, search));
    }

    @GetMapping("/departments")
    @Operation(summary = "List the departments that have cities")
    public ResponseEntity<List<String>> departments() {
        return CatalogResponses.cached(locationService.listDepartments());
    }
}
