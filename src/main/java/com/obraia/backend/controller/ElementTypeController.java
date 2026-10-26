package com.obraia.backend.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.catalog.ElementTypeResponse;
import com.obraia.backend.entity.catalog.ElementType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Public list of the building elements that can be measured. */
@RestController
@RequestMapping("/api/catalogs/element-types")
@Tag(name = "Catalogs")
public class ElementTypeController {

    private static final List<ElementTypeResponse> ELEMENT_TYPES = Arrays.stream(ElementType.values())
            .map(ElementTypeResponse::from)
            .toList();

    @GetMapping
    @Operation(summary = "Building elements in construction order, with their unit and required dimensions")
    public ResponseEntity<List<ElementTypeResponse>> list() {
        return CatalogResponses.cached(ELEMENT_TYPES);
    }
}
