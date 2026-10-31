package com.obraia.backend.dto.phase;

import java.util.List;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.phase.WorkStatus;

/** Turns the work statuses into the { code, label } pairs the frontend shows. */
final class WorkStatuses {

    private WorkStatuses() {
    }

    static StatusResponse status(WorkStatus status) {
        return new StatusResponse(status.getCode(), status.getLabel());
    }

    /** Allowed changes, in the order of the life cycle. */
    static List<StatusResponse> next(WorkStatus status) {
        return status.nextStatuses().stream().sorted().map(WorkStatuses::status).toList();
    }
}
