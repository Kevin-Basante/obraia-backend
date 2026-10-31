package com.obraia.backend.event;

import java.util.UUID;

/** Published when a task of the project gets a progress report. */
public record ProgressReportedEvent(UUID projectId) {
}
