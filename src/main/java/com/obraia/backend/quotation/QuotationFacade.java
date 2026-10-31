package com.obraia.backend.quotation;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.obraia.backend.config.QuotationProperties;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.Location;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.planning.PlannedPhase;
import com.obraia.backend.planning.ProjectCalculatorFactories;
import com.obraia.backend.planning.WorkCalendar;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.MaterialFormulaRepository;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.PhaseTemplateRepository;

/**
 * Facade of the quotation: one call, {@link #quoteProject}, hides the
 * parts that take part in it. It reads the measurements, asks the factory
 * of the project type for the quantities of material, takes the transport
 * rate of the city and the working days of the plan, then stacks the cost
 * layers (Decorator). When nobody typed measurements, it uses the ones
 * the AI read.
 */
@Component
public class QuotationFacade {

    private final BlueprintMeasurementRepository measurementRepository;
    private final MaterialFormulaRepository formulaRepository;
    private final PhaseRepository phaseRepository;
    private final PhaseTemplateRepository templateRepository;
    private final ProjectCalculatorFactories factories;
    private final QuotationProperties properties;

    public QuotationFacade(BlueprintMeasurementRepository measurementRepository,
            MaterialFormulaRepository formulaRepository, PhaseRepository phaseRepository,
            PhaseTemplateRepository templateRepository, ProjectCalculatorFactories factories,
            QuotationProperties properties) {
        this.measurementRepository = measurementRepository;
        this.formulaRepository = formulaRepository;
        this.phaseRepository = phaseRepository;
        this.templateRepository = templateRepository;
        this.factories = factories;
        this.properties = properties;
    }

    public Quotation quoteProject(Project project) {
        Quotation.Builder builder = Quotation.builder().projectId(project.getId());
        List<BlueprintMeasurement> measurements = measurementsToQuote(project, builder);
        MaterialTakeoff takeoff = factories.forType(project.getProjectType()).calculateMaterials(measurements,
                formulaRepository.findByProjectTypeId(project.getProjectType().getId()));
        if (takeoff.needs().isEmpty()) {
            throw new ConflictException("NO_MATERIAL_FORMULAS",
                    "The project type has no material formulas for the measured elements");
        }
        Location location = project.getLocation();
        builder.location(location.getCity(), location.getTransportPct()).takeoff(takeoff);
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

    /** The manual measurements when there are any; if not, the ones the AI already read. */
    private List<BlueprintMeasurement> measurementsToQuote(Project project, Quotation.Builder builder) {
        List<BlueprintMeasurement> all = measurementRepository.findByProjectIdOrderByCreatedAtAsc(project.getId());
        List<BlueprintMeasurement> manual = ofSource(all, MeasurementSource.MANUAL);
        if (!manual.isEmpty()) {
            builder.measurementSource(MeasurementSource.MANUAL);
            return manual;
        }
        List<BlueprintMeasurement> read = ofSource(all, MeasurementSource.OCR);
        if (read.isEmpty()) {
            throw new ConflictException("NO_MEASUREMENTS",
                    "Add the measurements of the blueprint, or let the AI read it, before quoting the project");
        }
        builder.measurementSource(MeasurementSource.OCR);
        return read;
    }

    private static List<BlueprintMeasurement> ofSource(List<BlueprintMeasurement> measurements,
            MeasurementSource source) {
        return measurements.stream().filter(measurement -> measurement.getSource() == source).toList();
    }

    /** From the dates of the phases when all have them; otherwise from the standard plan of the type. */
    private int workingDays(Project project, Quotation.Builder builder) {
        List<WorkSchedule> schedules = phaseRepository.findByProjectIdOrderBySequenceOrderAsc(project.getId())
                .stream().map(Phase::getSchedule).toList();
        boolean dated = !schedules.isEmpty() && schedules.stream()
                .allMatch(schedule -> schedule.getPlannedStartDate() != null && schedule.getPlannedEndDate() != null);
        if (dated) {
            LocalDate start = schedules.stream().map(WorkSchedule::getPlannedStartDate)
                    .min(Comparator.naturalOrder()).orElseThrow();
            LocalDate end = schedules.stream().map(WorkSchedule::getPlannedEndDate)
                    .max(Comparator.naturalOrder()).orElseThrow();
            int days = WorkCalendar.workingDaysBetween(start, end);
            builder.workingDays(days, Quotation.DaysSource.PHASES);
            return days;
        }
        List<PlannedPhase> standard = factories.forType(project.getProjectType()).planPhases(
                templateRepository.findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(
                        project.getProjectType().getId(), project.getSizeCategory()),
                project.getSizeCategory(), null);
        int days = standard.stream().mapToInt(PlannedPhase::durationDays).sum();
        builder.workingDays(days, Quotation.DaysSource.STANDARD_PLAN);
        return days;
    }
}
