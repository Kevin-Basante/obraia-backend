package com.obraia.backend.dto.phase;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Progress of a task on a day.
 *
 * @param reportDate      today when it is missing; it cannot be in the future
 * @param fromSiteManager true when the site manager of the project gave the information; false when missing
 */
public record ProgressReportRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal completionPercentage,
        @Size(max = 1000) String notes,
        LocalDate reportDate,
        Boolean fromSiteManager) {

    public boolean isFromSiteManager() {
        return Boolean.TRUE.equals(fromSiteManager);
    }
}
