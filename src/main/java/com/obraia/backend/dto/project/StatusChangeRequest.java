package com.obraia.backend.dto.project;

import jakarta.validation.constraints.NotBlank;

/** New status code, for example "in_progress". */
public record StatusChangeRequest(@NotBlank String status) {
}
