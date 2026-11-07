package com.obraia.backend.event;

import java.util.UUID;

/** Published when the progress of a task changes: a new report, or a task completed or reopened. */
public record ProgressReportedEvent(UUID projectId) {
}
