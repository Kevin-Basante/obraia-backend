package com.obraia.backend.dto.user;

import java.time.LocalDate;
import java.util.List;

/**
 * What the user loses by deleting the account. The frontend shows it in
 * the warnings before asking for the password.
 *
 * @param ownProjects      projects the user created; they are deleted too
 * @param sharedProjects   the own projects other members also lose
 * @param joinedProjects   projects of other people the user leaves
 * @param purgeDate        day the data is removed for good if the deletion is confirmed today
 * @param linkValidMinutes how long the confirmation link works
 */
public record AccountDeletionImpactResponse(List<String> ownProjects, List<String> sharedProjects,
        long joinedProjects, LocalDate purgeDate, long linkValidMinutes) {
}
