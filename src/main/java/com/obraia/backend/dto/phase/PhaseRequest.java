package com.obraia.backend.dto.phase;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Name and planned dates of a phase. Either date may be missing. */
public record PhaseRequest(
        @NotBlank @Size(min = 3, max = 100) String name,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate) {

    /** The error appears under the field "plannedDatesInOrder", like in a project. */
    @JsonIgnore
    @AssertTrue(message = "the planned end date must be on or after the planned start date")
    public boolean isPlannedDatesInOrder() {
        return plannedStartDate == null || plannedEndDate == null || !plannedEndDate.isBefore(plannedStartDate);
    }
}
