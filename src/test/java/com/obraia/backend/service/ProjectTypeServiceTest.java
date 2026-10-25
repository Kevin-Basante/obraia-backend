package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.catalog.MaterialFormulaResponse;
import com.obraia.backend.dto.catalog.PhasePlanResponse;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.catalog.Material;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.catalog.SizeCategory;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.MaterialFormulaRepository;
import com.obraia.backend.repository.PhaseTemplateRepository;
import com.obraia.backend.repository.ProjectTypeRepository;
import com.obraia.backend.support.CatalogFixtures;

class ProjectTypeServiceTest {

    private ProjectTypeRepository projectTypes;
    private PhaseTemplateRepository phaseTemplates;
    private MaterialFormulaRepository formulas;
    private ProjectTypeService service;
    private ProjectType house;

    @BeforeEach
    void setUp() {
        projectTypes = mock(ProjectTypeRepository.class);
        phaseTemplates = mock(PhaseTemplateRepository.class);
        formulas = mock(MaterialFormulaRepository.class);
        service = new ProjectTypeService(projectTypes, phaseTemplates, formulas);
        house = CatalogFixtures.projectType("Vivienda unifamiliar");
        when(projectTypes.findAllByOrderByNameAsc()).thenReturn(List.of(house));
    }

    @Test
    void unknownProjectTypeIsNotFound() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getProjectType(UUID.randomUUID()));

        assertEquals("PROJECT_TYPE_NOT_FOUND", ex.getCode());
    }

    @Test
    void phasePlanAddsTheDurationOfEveryPhase() {
        when(phaseTemplates.findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(house.getId(), SizeCategory.SMALL))
                .thenReturn(List.of(
                        CatalogFixtures.phase(house, SizeCategory.SMALL, 1, "Preliminares", 5),
                        CatalogFixtures.phase(house, SizeCategory.SMALL, 2, "Cimentación", 14)));

        PhasePlanResponse plan = service.getPhasePlan(house.getId(), null, null);

        assertEquals("small", plan.sizeCategory());
        assertEquals(19, plan.totalDurationDays());
        assertEquals("Cimentación", plan.phases().get(1).name());
    }

    @Test
    void builtAreaDecidesTheSize() {
        service.getPhasePlan(house.getId(), "small", new BigDecimal("150"));

        verify(phaseTemplates).findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(house.getId(),
                SizeCategory.LARGE);
    }

    @Test
    void zeroAreaIsRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.getPhasePlan(house.getId(), null, BigDecimal.ZERO));

        assertEquals("INVALID_AREA", ex.getCode());
    }

    @Test
    void formulasAreSortedByConstructionOrderAndFilteredByElement() {
        Material block = CatalogFixtures.material("Bloque de arcilla No. 5", MaterialCategory.MASONRY, "1600");
        Material cement = CatalogFixtures.material("Cemento gris", MaterialCategory.CEMENT_CONCRETE, "33000");
        when(formulas.findByProjectTypeId(house.getId())).thenReturn(List.of(
                CatalogFixtures.formula(house, block, ElementType.WALL, "12.5"),
                CatalogFixtures.formula(house, cement, ElementType.WALL, "0.15"),
                CatalogFixtures.formula(house, cement, ElementType.FOUNDATION, "7")));

        List<MaterialFormulaResponse> all = service.getMaterialFormulas(house.getId(), null);
        List<MaterialFormulaResponse> walls = service.getMaterialFormulas(house.getId(), "wall");

        assertEquals(List.of("foundation", "wall", "wall"), all.stream().map(MaterialFormulaResponse::elementType).toList());
        assertEquals(List.of("Bloque de arcilla No. 5", "Cemento gris"),
                walls.stream().map(MaterialFormulaResponse::materialName).toList());
    }
}
