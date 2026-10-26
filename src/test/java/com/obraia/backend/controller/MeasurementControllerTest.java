package com.obraia.backend.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.BlueprintService;
import com.obraia.backend.service.BlueprintService.BlueprintFile;
import com.obraia.backend.service.MeasurementService;
import com.obraia.backend.support.BlueprintFixtures;

/** HTTP side of the blueprint, measurement and element type routes, without the database. */
class MeasurementControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private BlueprintService blueprintService;
    private MeasurementService measurementService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        blueprintService = mock(BlueprintService.class);
        measurementService = mock(MeasurementService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new BlueprintController(blueprintService),
                        new MeasurementController(measurementService), new ElementTypeController())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadWithoutFileAnswers400() throws Exception {
        mvc.perform(multipart("/api/projects/{id}/blueprints", projectId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_FILE"));
    }

    @Test
    void uploadSendsTheBytesToTheService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plano.png", "image/png", BlueprintFixtures.PNG);

        mvc.perform(multipart("/api/projects/{id}/blueprints", projectId).file(file))
                .andExpect(status().isCreated());
    }

    @Test
    void fileIsShownInlineWithItsRealType() throws Exception {
        UUID blueprintId = UUID.randomUUID();
        when(blueprintService.download(projectId, userId, blueprintId))
                .thenReturn(new BlueprintFile("primer piso.png", "image/png", BlueprintFixtures.PNG));

        mvc.perform(get("/api/projects/{id}/blueprints/{bid}/file", projectId, blueprintId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition",
                        startsWith("inline; filename=")))
                .andExpect(content().bytes(BlueprintFixtures.PNG));
    }

    @Test
    void invalidMeasurementAnswers400WithEachField() throws Exception {
        String body = """
                {"elementType": "wall", "floorNumber": 3, "lengthM": 0, "quantity": 0}
                """;

        mvc.perform(post("/api/projects/{id}/measurements", projectId).contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.floorNumber").exists())
                .andExpect(jsonPath("$.error.fields.lengthM").exists())
                .andExpect(jsonPath("$.error.fields.quantity").exists());
    }

    @Test
    void ruleErrorsFromTheServiceUseTheSameShape() throws Exception {
        when(measurementService.create(eq(projectId), eq(userId), any()))
                .thenThrow(new FieldValidationException(Map.of("heightM", "is required for walls")));
        String body = """
                {"elementType": "wall", "floorNumber": 1, "lengthM": 3.5, "quantity": 1}
                """;

        mvc.perform(post("/api/projects/{id}/measurements", projectId).contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.heightM").value("is required for walls"));
    }

    @Test
    void elementTypesTellWhichDimensionsAreNeeded() throws Exception {
        mvc.perform(get("/api/catalogs/element-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[4].code").value("wall"))
                .andExpect(jsonPath("$[4].baseUnit").value("m2"))
                .andExpect(jsonPath("$[4].requiredDimensions[1]").value("heightM"));
    }
}
