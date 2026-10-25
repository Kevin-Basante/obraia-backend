package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.catalog.MaterialResponse;
import com.obraia.backend.entity.catalog.Material;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.MaterialRepository;
import com.obraia.backend.support.CatalogFixtures;

class MaterialServiceTest {

    private MaterialService service;
    private Material sand;

    @BeforeEach
    void setUp() {
        sand = CatalogFixtures.material("Arena de río (concreto)", MaterialCategory.AGGREGATES, "100000");
        MaterialRepository repository = mock(MaterialRepository.class);
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(
                sand,
                CatalogFixtures.material("Cemento gris uso general", MaterialCategory.CEMENT_CONCRETE, "33000"),
                CatalogFixtures.material("Gravilla", MaterialCategory.AGGREGATES, "115000")));
        service = new MaterialService(repository);
    }

    @Test
    void categoriesIncludeEveryCodeWithItsLabel() {
        assertEquals(MaterialCategory.values().length, service.listCategories().size());
        assertEquals("Cement and concrete", service.listCategories().get(0).label());
    }

    @Test
    void filtersByCategoryAndSearchTogether() {
        List<MaterialResponse> aggregates = service.listMaterials("aggregates", null);
        List<MaterialResponse> river = service.listMaterials("aggregates", "rio");

        assertEquals(2, aggregates.size());
        assertEquals(List.of("Arena de río (concreto)"), river.stream().map(MaterialResponse::name).toList());
    }

    @Test
    void unknownCategoryIsABadRequest() {
        assertThrows(BadRequestException.class, () -> service.listMaterials("wood", null));
    }

    @Test
    void findsOneMaterialById() {
        assertEquals("aggregates", service.getMaterial(sand.getId()).category().code());
    }

    @Test
    void unknownMaterialIsNotFound() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getMaterial(UUID.randomUUID()));

        assertEquals("MATERIAL_NOT_FOUND", ex.getCode());
    }
}
