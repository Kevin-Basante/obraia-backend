package com.obraia.backend.dto.phase;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A task and the phase it belongs to. Editing it with another phaseId moves it there. */
public record TaskRequest(
        @NotNull UUID phaseId,
        @NotBlank @Size(min = 3, max = 150) String name,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate) {

    @JsonIgnore
    @AssertTrue(message = "the planned end date must be on or after the planned start date")
    public boolean isPlannedDatesInOrder() {
        return plannedStartDate == null || plannedEndDate == null || !plannedEndDate.isBefore(plannedStartDate);
    }
}
