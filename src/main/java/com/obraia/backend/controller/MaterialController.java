package com.obraia.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.catalog.CategoryResponse;
import com.obraia.backend.dto.catalog.MaterialResponse;
import com.obraia.backend.service.MaterialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Public catalog of materials and their reference prices. */
@RestController
@RequestMapping("/api/catalogs")
@Tag(name = "Catalogs")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/material-categories")
    @Operation(summary = "List the material categories")
    public ResponseEntity<List<CategoryResponse>> categories() {
        return CatalogResponses.cached(materialService.listCategories());
    }

    @GetMapping("/materials")
    @Operation(summary = "List the materials", description = "Prices are in COP and include VAT.")
    @ApiResponse(responseCode = "200", description = "Materials sorted by name")
    @ApiResponse(responseCode = "400", description = "Unknown category",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<MaterialResponse>> materials(
            @Parameter(description = "Category code", example = "steel")
            @RequestParam(required = false) String category,
            @Parameter(description = "Part of the material name; accents are optional", example = "arena")
            @RequestParam(required = false) String search) {
        return CatalogResponses.cached(materialService.listMaterials(category, search));
    }

    @GetMapping("/materials/{id}")
    @Operation(summary = "One material")
    @ApiResponse(responseCode = "200", description = "The material")
    @ApiResponse(responseCode = "404", description = "The material does not exist",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MaterialResponse> material(@PathVariable UUID id) {
        return CatalogResponses.cached(materialService.getMaterial(id));
    }
}
