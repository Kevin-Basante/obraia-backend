package com.obraia.backend.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.config.QuotationProperties;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.budget.BudgetItemType;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.catalog.Material;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.entity.catalog.SizeCategory;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.planning.CommercialUnitCalculatorFactory;
import com.obraia.backend.planning.HouseCalculatorFactory;
import com.obraia.backend.planning.ProjectCalculatorFactories;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.MaterialFormulaRepository;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.PhaseTemplateRepository;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.CatalogFixtures;
import com.obraia.backend.support.ProjectFixtures;

class QuotationFacadeTest {

    // 90 m2 house in Cali (transport 3 %), so a small one (see ProjectFixtures).
    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Cotizada");
    private final Material block = CatalogFixtures.material("Bloque de arcilla No. 5", MaterialCategory.MASONRY,
            "1600");

    private BlueprintMeasurementRepository measurements;
    private PhaseRepository phases;
    private QuotationFacade facade;

    @BeforeEach
    void setUp() {
        measurements = mock(BlueprintMeasurementRepository.class);
        phases = mock(PhaseRepository.class);
        MaterialFormulaRepository formulas = mock(MaterialFormulaRepository.class);
        PhaseTemplateRepository templates = mock(PhaseTemplateRepository.class);
        when(formulas.findByProjectTypeId(project.getProjectType().getId())).thenReturn(List.of(
                CatalogFixtures.formula(project.getProjectType(), block, ElementType.WALL, "12.5")));
        when(templates.findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(project.getProjectType().getId(),
                SizeCategory.SMALL)).thenReturn(List.of(
                        CatalogFixtures.phase(project.getProjectType(), SizeCategory.SMALL, 1,
                                "Preliminares y replanteo", 5),
                        CatalogFixtures.phase(project.getProjectType(), SizeCategory.SMALL, 2, "Mampostería", 21)));
        ProjectCalculatorFactories factories = new ProjectCalculatorFactories(
                List.of(new HouseCalculatorFactory(), new CommercialUnitCalculatorFactory()));
        facade = new QuotationFacade(measurements, formulas, phases, templates, factories,
                new QuotationProperties(new BigDecimal("280000"), new BigDecimal("19")));
    }

    @Test
    void quotesFromTheManualMeasurementsAndTheStandardPlan() {
        // 4 m by 2.5 m = 10 m2 of wall = 125 blocks + 6.25 of waste.
        when(measurements.findByProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "4", null,
                        "2.5", 1)));

        Quotation quotation = facade.quoteProject(project);

        assertEquals(MeasurementSource.MANUAL, quotation.getMeasurementSource());
        assertEquals(Quotation.DaysSource.STANDARD_PLAN, quotation.getDaysSource());
        assertEquals(26, quotation.getWorkingDays());
        assertEquals("Cali", quotation.getCity());
        assertEquals(List.of(BudgetItemType.MATERIALS, BudgetItemType.WASTE, BudgetItemType.TRANSPORT,
                BudgetItemType.LABOR, BudgetItemType.TAX), quotation.lines().stream().map(CostLine::type).toList());
        // 200,000 + 10,000 + 6,300 + 7,280,000 + 19 % of 7,286,300.
        assertEquals(new BigDecimal("8880697.00"), quotation.total());
    }

    @Test
    void workingDaysComeFromThePhasesWhenTheyHaveDates() {
        when(measurements.findByProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "4", null,
                        "2.5", 1)));
        // Monday 2 to Saturday 14 of November: 12 working days.
        when(phases.findByProjectIdOrderBySequenceOrderAsc(project.getId())).thenReturn(List.of(
                new Phase(project, null, "Preliminares", 1,
                        new WorkSchedule(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6))),
                new Phase(project, null, "Mampostería", 2,
                        new WorkSchedule(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 14)))));

        Quotation quotation = facade.quoteProject(project);

        assertEquals(Quotation.DaysSource.PHASES, quotation.getDaysSource());
        assertEquals(12, quotation.getWorkingDays());
    }

    @Test
    void phasesWithoutDatesFallBackToTheStandardPlan() {
        when(measurements.findByProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "4", null,
                        "2.5", 1)));
        when(phases.findByProjectIdOrderBySequenceOrderAsc(project.getId())).thenReturn(List.of(
                new Phase(project, null, "Preliminares", 1, new WorkSchedule(null, null))));

        assertEquals(Quotation.DaysSource.STANDARD_PLAN, facade.quoteProject(project).getDaysSource());
    }

    @Test
    void manualMeasurementsWinOverTheOnesOfTheAi() {
        when(measurements.findByProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.OCR, ElementType.WALL, 1, "40", null,
                        "2.5", 1),
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.WALL, 1, "4", null,
                        "2.5", 1)));

        Quotation quotation = facade.quoteProject(project);

        assertEquals(MeasurementSource.MANUAL, quotation.getMeasurementSource());
        assertEquals(new BigDecimal("8880697.00"), quotation.total());
    }

    @Test
    void withoutManualMeasurementsTheOnesTheAiReadAreQuoted() {
        when(measurements.findByProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.OCR, ElementType.WALL, 1, "4", null,
                        "2.5", 1)));

        Quotation quotation = facade.quoteProject(project);

        assertEquals(MeasurementSource.OCR, quotation.getMeasurementSource());
        assertEquals(new BigDecimal("8880697.00"), quotation.total());
    }

    @Test
    void withoutMeasurementsOrBlueprintsItCannotBeQuoted() {
        ConflictException ex = assertThrows(ConflictException.class, () -> facade.quoteProject(project));

        assertEquals("NO_MEASUREMENTS", ex.getCode());
    }

    @Test
    void measuredElementsWithoutFormulasCannotBeQuoted() {
        when(measurements.findByProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(
                BlueprintFixtures.measurement(project, MeasurementSource.MANUAL, ElementType.ROOF, 1, "8", "6",
                        null, 1)));

        ConflictException ex = assertThrows(ConflictException.class, () -> facade.quoteProject(project));

        assertEquals("NO_MATERIAL_FORMULAS", ex.getCode());
    }
}
