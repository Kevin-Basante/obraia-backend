package com.obraia.backend.dto.blueprint;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A measurement typed by the user. Which of width and height are needed
 * depends on the element (see MeasurementRules); the service checks it.
 */
public record MeasurementRequest(
        UUID blueprintId,
        @NotBlank String elementType,
        @NotNull @Min(1) @Max(2) Integer floorNumber,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000.00") @Digits(integer = 4, fraction = 2) BigDecimal lengthM,
        @DecimalMin("0.01") @DecimalMax("1000.00") @Digits(integer = 4, fraction = 2) BigDecimal widthM,
        @DecimalMin("0.01") @DecimalMax("1000.00") @Digits(integer = 4, fraction = 2) BigDecimal heightM,
        @NotNull @Min(1) @Max(1000) Integer quantity,
        @Size(max = 500) String notes) {
}
