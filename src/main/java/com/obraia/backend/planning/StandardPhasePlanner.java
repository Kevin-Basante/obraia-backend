package com.obraia.backend.planning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.SizeCategory;

/**
 * Common steps of every planner: one phase after the other, counting
 * working days from the planned start. Each project type gives its own
 * starting tasks and can add phases at the end.
 */
public abstract class StandardPhasePlanner implements PhasePlanner {

    @Override
    public List<PlannedPhase> plan(List<PhaseTemplate> templates, SizeCategory size, LocalDate start) {
        List<PlannedPhase> phases = new ArrayList<>();
        LocalDate next = start == null ? null : WorkCalendar.firstWorkingDayFrom(start);
        for (PhaseTemplate template : templates) {
            PlannedPhase phase = phase(template.getId(), template.getName(), phases.size() + 1,
                    template.getEstimatedDurationDays(), next);
            phases.add(phase);
            next = following(phase);
        }
        for (ExtraPhase extra : extraPhases(size)) {
            PlannedPhase phase = phase(null, extra.name(), phases.size() + 1, extra.durationDays(), next);
            phases.add(phase);
            next = following(phase);
        }
        return phases;
    }

    /** Phase that only this project type has, added after the standard ones. */
    protected record ExtraPhase(String name, int durationDays) {
    }

    /** Starting tasks of each phase, by phase name. */
    protected abstract Map<String, List<String>> tasksByPhase();

    /** Phases added after the standard ones. None by default. */
    protected List<ExtraPhase> extraPhases(SizeCategory size) {
        return List.of();
    }

    private PlannedPhase phase(UUID templateId, String name, int order, int days, LocalDate start) {
        LocalDate end = start == null ? null : WorkCalendar.endDate(start, days);
        return new PlannedPhase(templateId, name, order, days, start, end,
                tasksByPhase().getOrDefault(name, List.of()));
    }

    private static LocalDate following(PlannedPhase phase) {
        return phase.plannedEnd() == null ? null : WorkCalendar.nextWorkingDay(phase.plannedEnd());
    }
}
