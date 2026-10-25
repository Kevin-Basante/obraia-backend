package com.obraia.backend.dto.project;

import java.time.LocalDate;
import java.util.Map;

/**
 * What the user loses by deleting the project. The frontend shows it in
 * the warnings before asking for the email confirmation.
 *
 * @param content       how many rows of each module the project has
 * @param purgeDate     day the data is removed for good if the deletion is confirmed today
 * @param linkValidMinutes how long the confirmation link works
 */
public record DeletionImpactResponse(String projectName, Map<String, Integer> content, LocalDate purgeDate,
        long linkValidMinutes) {
}
