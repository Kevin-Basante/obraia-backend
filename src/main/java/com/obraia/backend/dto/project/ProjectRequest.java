package com.obraia.backend.dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data to create or edit a project. ObraIA plans houses and commercial
 * units of 1 or 2 floors, so the area has a sensible upper limit.
 */
public record ProjectRequest(
        @NotBlank @Size(min = 3, max = 150) String name,
        @NotNull UUID projectTypeId,
        @NotNull UUID locationId,
        @Size(max = 255) String address,
        @NotNull @DecimalMin(value = "1.00") @DecimalMax(value = "2000.00") @Digits(integer = 4, fraction = 2)
        BigDecimal builtAreaM2,
        @NotNull @Min(1) @Max(2) Integer floors,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate) {

    /**
     * The end date cannot be before the start date. Either one may be
     * missing. The error appears under the field "plannedDatesInOrder".
     */
    @JsonIgnore
    @AssertTrue(message = "the planned end date must be on or after the planned start date")
    public boolean isPlannedDatesInOrder() {
        return plannedStartDate == null || plannedEndDate == null || !plannedEndDate.isBefore(plannedStartDate);
    }
}
