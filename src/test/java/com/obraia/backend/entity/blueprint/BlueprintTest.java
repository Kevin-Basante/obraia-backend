package com.obraia.backend.entity.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.project.Project;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.ProjectFixtures;

class BlueprintTest {

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Planos");

    @Test
    void aNewBlueprintIsPendingUntilTheAiReadsIt() {
        Blueprint blueprint = BlueprintFixtures.blueprint(project, "planta.png");
        assertEquals(ProcessingStatus.PENDING, blueprint.getProcessingStatus());

        blueprint.markCompleted();

        assertEquals(ProcessingStatus.COMPLETED, blueprint.getProcessingStatus());
    }

    @Test
    void aFailedBlueprintCanBeCompletedLater() {
        Blueprint blueprint = BlueprintFixtures.blueprint(project, "planta.png");

        blueprint.markFailed();
        assertEquals(ProcessingStatus.FAILED, blueprint.getProcessingStatus());
        blueprint.markCompleted();

        assertEquals(ProcessingStatus.COMPLETED, blueprint.getProcessingStatus());
    }
}
