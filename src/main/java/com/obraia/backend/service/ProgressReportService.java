package com.obraia.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.phase.ProgressReportRequest;
import com.obraia.backend.dto.phase.ProgressReportResponse;
import com.obraia.backend.entity.phase.ProgressReport;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.SiteManager;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * Daily progress of the tasks. Reporting moves the task forward: more
 * than 0% starts it and 100% completes it, and the phase follows.
 */
@Service
public class ProgressReportService {

    private static final BigDecimal COMPLETE = new BigDecimal("100");

    private final ProgressReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final ProjectAccessService accessService;
    private final WorkPlanViews views;
    private final EntityManager entityManager;

    public ProgressReportService(ProgressReportRepository reportRepository, UserRepository userRepository,
            TaskService taskService, ProjectAccessService accessService, WorkPlanViews views,
            EntityManager entityManager) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.accessService = accessService;
        this.views = views;
        this.entityManager = entityManager;
    }

    /** Newest first. */
    @Transactional(readOnly = true)
    public List<ProgressReportResponse> list(UUID projectId, UUID userId, UUID taskId) {
        accessService.requireMembership(projectId, userId);
        Task task = taskService.requireTask(projectId, taskId);
        return reportRepository.findByTaskIdOrderByReportDateDescCreatedAtDesc(task.getId()).stream()
                .map(ProgressReportResponse::from)
                .toList();
    }

    @Transactional
    public ProgressReportResponse create(UUID projectId, UUID userId, UUID taskId, ProgressReportRequest request) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Task task = taskService.requireTask(projectId, taskId);
        LocalDate today = views.today();
        LocalDate reportDate = request.reportDate() == null ? today : request.reportDate();

        Map<String, String> errors = new LinkedHashMap<>();
        if (reportDate.isAfter(today)) {
            errors.put("reportDate", "cannot be in the future");
        }
        SiteManager siteManager = request.isFromSiteManager() ? project.getSiteManager() : null;
        if (request.isFromSiteManager() && siteManager == null) {
            errors.put("fromSiteManager", "the project has no site manager assigned");
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }

        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().strip();
        ProgressReport report = reportRepository.saveAndFlush(new ProgressReport(task,
                userRepository.getReferenceById(userId), siteManager, request.completionPercentage(), notes,
                reportDate));
        moveTask(task, request.completionPercentage(), today);
        // Reads created_at, which the database fills in.
        entityManager.refresh(report);
        return ProgressReportResponse.from(report);
    }

    private void moveTask(Task task, BigDecimal percentage, LocalDate today) {
        WorkSchedule schedule = task.getSchedule();
        if (percentage.signum() > 0) {
            WorkProgress.startIfPlanned(schedule, today);
        }
        if (percentage.compareTo(COMPLETE) == 0 && schedule.getStatus() != WorkStatus.COMPLETED) {
            WorkProgress.startIfPlanned(schedule, today);
            schedule.changeStatus(WorkStatus.COMPLETED, today);
        }
        taskService.syncPhase(task.getPhase());
        entityManager.flush();
    }
}
