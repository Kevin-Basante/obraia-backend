package com.obraia.backend.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.catalog.SizeCategory;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.support.CatalogFixtures;

/** The Factory Method: each project type gets its own planner. */
class ProjectCalculatorFactoryTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private final ProjectType house = CatalogFixtures.projectType("Vivienda unifamiliar");
    private final ProjectType shop = CatalogFixtures.projectType("Local comercial");
    private final ProjectCalculatorFactories factories = new ProjectCalculatorFactories(
            List.of(new HouseCalculatorFactory(), new CommercialUnitCalculatorFactory()));

    private List<PhaseTemplate> templates(ProjectType type) {
        return List.of(
                CatalogFixtures.phase(type, SizeCategory.SMALL, 1, "Preliminares y replanteo", 5),
                CatalogFixtures.phase(type, SizeCategory.SMALL, 2, "Cimentación", 14));
    }

    @Test
    void eachTypeGetsItsOwnFactory() {
        assertInstanceOf(HouseCalculatorFactory.class, factories.forType(house));
        assertInstanceOf(CommercialUnitCalculatorFactory.class, factories.forType(shop));
        assertInstanceOf(CommercialUnitCalculatorFactory.class,
                factories.forType(CatalogFixtures.projectType("LOCAL COMERCIAL")));
    }

    @Test
    void unknownTypeHasNoPlanner() {
        ConflictException ex = assertThrows(ConflictException.class,
                () -> factories.forType(CatalogFixtures.projectType("Bodega")));

        assertEquals("NO_PLANNER_FOR_TYPE", ex.getCode());
    }

    @Test
    void housePhasesFollowEachOtherInWorkingDays() {
        List<PlannedPhase> phases = factories.forType(house).planPhases(templates(house), SizeCategory.SMALL,
                MONDAY);

        assertEquals(2, phases.size());
        PlannedPhase first = phases.get(0);
        assertEquals(1, first.order());
        assertEquals(MONDAY, first.plannedStart());
        assertEquals(LocalDate.of(2026, 11, 6), first.plannedEnd());
        // The next phase starts on the following working day.
        assertEquals(LocalDate.of(2026, 11, 7), phases.get(1).plannedStart());
        assertEquals(List.of("Limpieza del lote", "Cerramiento provisional", "Replanteo de ejes"), first.tasks());
        assertEquals(templates(house).size(), phases.size());
    }

    @Test
    void phasesKeepTheirTemplate() {
        List<PhaseTemplate> templates = templates(house);

        List<PlannedPhase> phases = factories.forType(house).planPhases(templates, SizeCategory.SMALL, MONDAY);

        assertEquals(templates.get(1).getId(), phases.get(1).templateId());
    }

    @Test
    void commercialUnitAddsTheFitOutPhase() {
        List<PlannedPhase> small = factories.forType(shop).planPhases(templates(shop), SizeCategory.SMALL, MONDAY);
        List<PlannedPhase> large = factories.forType(shop).planPhases(templates(shop), SizeCategory.LARGE, MONDAY);

        PlannedPhase fitOut = small.get(2);
        assertEquals(CommercialUnitPhasePlanner.FIT_OUT, fitOut.name());
        assertEquals(3, fitOut.order());
        assertNull(fitOut.templateId());
        assertEquals(7, fitOut.durationDays());
        assertEquals(10, large.get(2).durationDays());
        assertEquals(List.of("Fachada y aviso", "Vitrina", "Cortina metálica"), fitOut.tasks());
    }

    @Test
    void withoutStartDateThePhasesHaveNoDates() {
        List<PlannedPhase> phases = factories.forType(house).planPhases(templates(house), SizeCategory.SMALL, null);

        assertTrue(phases.stream().allMatch(phase -> phase.plannedStart() == null && phase.plannedEnd() == null));
    }

    @Test
    void sundayStartMovesTheWholePlan() {
        List<PlannedPhase> phases = factories.forType(house).planPhases(templates(house), SizeCategory.SMALL,
                MONDAY.minusDays(1));

        assertEquals(MONDAY, phases.get(0).plannedStart());
    }
}
