package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.phase.TaskRequest;
import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.dto.project.StatusChangeRequest;
import com.obraia.backend.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Tasks of the phases. */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@Tag(name = "Tasks", description = "Tasks of the phases")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a task to a phase")
    @ApiResponse(responseCode = "201", description = "Task added")
    @ApiResponse(responseCode = "400", description = "Invalid fields or a phase of another project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TaskResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, ProjectController.userId(jwt), request);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Edit a task", description = "Another phaseId moves the task to that phase.")
    @ApiResponse(responseCode = "200", description = "Task saved")
    @ApiResponse(responseCode = "404", description = "The task does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID taskId, @Valid @RequestBody TaskRequest request) {
        return taskService.update(projectId, ProjectController.userId(jwt), taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task and its progress reports")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "404", description = "The task does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @PathVariable UUID taskId) {
        taskService.delete(projectId, ProjectController.userId(jwt), taskId);
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Change the status of a task", description = "The phase follows its tasks.")
    @ApiResponse(responseCode = "200", description = "Status changed")
    @ApiResponse(responseCode = "409", description = "The change is not allowed from the current status",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TaskResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID taskId, @Valid @RequestBody StatusChangeRequest request) {
        return taskService.changeStatus(projectId, ProjectController.userId(jwt), taskId, request.status());
    }

}
