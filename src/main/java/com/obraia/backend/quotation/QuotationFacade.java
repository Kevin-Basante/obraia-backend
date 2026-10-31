package com.obraia.backend.quotation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.obraia.backend.config.QuotationProperties;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.Location;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.planning.PlannedPhase;
import com.obraia.backend.planning.ProjectCalculatorFactories;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.MaterialFormulaRepository;
import com.obraia.backend.repository.PhaseTemplateRepository;

/**
 * Facade of the quotation: one call, {@link #quoteProject}, hides the
 * parts that take part in it. It reads the measurements, asks the factory
 * of the project type for the quantities of material, takes the transport
 * rate of the city and the working days of the plan, then stacks the cost
 * layers (Decorator). In block 7 the measurements of the AI join through
 * the AI service proxy.
 */
@Component
public class QuotationFacade {

    private final BlueprintMeasurementRepository measurementRepository;
    private final MaterialFormulaRepository formulaRepository;
    private final PhaseTemplateRepository templateRepository;
    private final ProjectCalculatorFactories factories;
    private final QuotationProperties properties;

    public QuotationFacade(BlueprintMeasurementRepository measurementRepository,
            MaterialFormulaRepository formulaRepository, PhaseTemplateRepository templateRepository,
            ProjectCalculatorFactories factories, QuotationProperties properties) {
        this.measurementRepository = measurementRepository;
        this.formulaRepository = formulaRepository;
        this.templateRepository = templateRepository;
        this.factories = factories;
        this.properties = properties;
    }

    public Quotation quoteProject(Project project) {
        List<BlueprintMeasurement> measurements = measurementRepository
                .findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .filter(measurement -> measurement.getSource() == MeasurementSource.MANUAL).toList();
        if (measurements.isEmpty()) {
            throw new ConflictException("NO_MEASUREMENTS",
                    "Add the measurements of the blueprint before quoting the project");
        }
        MaterialTakeoff takeoff = factories.forType(project.getProjectType()).calculateMaterials(measurements,
                formulaRepository.findByProjectTypeId(project.getProjectType().getId()));
        if (takeoff.needs().isEmpty()) {
            throw new ConflictException("NO_MATERIAL_FORMULAS",
                    "The project type has no material formulas for the measured elements");
        }
        Location location = project.getLocation();
        Quotation.Builder builder = Quotation.builder().projectId(project.getId())
                .location(location.getCity(), location.getTransportPct()).takeoff(takeoff);
        int workingDays = workingDays(project, builder);
        CostComponent cost = new TaxCost(
                new LaborCost(
                        new TransportCost(
                                new WasteCost(new MaterialsCost(takeoff.needs()), takeoff.needs()),
                                location.getTransportPct(), location.getCity()),
                        workingDays, properties.laborDailyRate()),
                properties.vatPct());
        return builder.cost(cost).build();
    }

    /** Working days of the standard phases of the project type. */
    private int workingDays(Project project, Quotation.Builder builder) {
        List<PlannedPhase> standard = factories.forType(project.getProjectType()).planPhases(
                templateRepository.findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(
                        project.getProjectType().getId(), project.getSizeCategory()),
                project.getSizeCategory(), null);
        int days = standard.stream().mapToInt(PlannedPhase::durationDays).sum();
        builder.workingDays(days, Quotation.DaysSource.STANDARD_PLAN);
        return days;
    }
}
