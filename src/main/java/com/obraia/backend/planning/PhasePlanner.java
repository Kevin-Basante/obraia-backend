package com.obraia.backend.planning;

import java.time.LocalDate;
import java.util.List;

import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.SizeCategory;

/** Product of the Factory Method: turns the standard phases into the phases of one project. */
public interface PhasePlanner {

    /**
     * @param templates standard phases of the project type and size, in order
     * @param start     planned start of the project, or null
     */
    List<PlannedPhase> plan(List<PhaseTemplate> templates, SizeCategory size, LocalDate start);
}
