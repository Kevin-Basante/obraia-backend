package com.obraia.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.repository.MaterialRepository;
import com.obraia.backend.repository.ProjectTypeRepository;
import com.obraia.backend.service.MaterialService;
import com.obraia.backend.service.ProjectTypeService;
import com.obraia.backend.support.CatalogFixtures;

/**
 * Checks the HTTP side of the catalogs (status codes, JSON and cache
 * headers) without starting the server or the database.
 */
class CatalogControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MaterialRepository materials = mock(MaterialRepository.class);
        when(materials.findAllByOrderByNameAsc()).thenReturn(List.of(
                CatalogFixtures.material("Gravilla", MaterialCategory.AGGREGATES, "115000")));
        ProjectTypeRepository projectTypes = mock(ProjectTypeRepository.class);
        when(projectTypes.findAllByOrderByNameAsc()).thenReturn(List.of());

        mvc = MockMvcBuilders
                .standaloneSetup(new MaterialController(new MaterialService(materials)),
                        new ProjectTypeController(new ProjectTypeService(projectTypes, null, null)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void materialsAreReturnedWithABrowserCacheHeader() throws Exception {
        mvc.perform(get("/api/catalogs/materials"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=600, public"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category.code").value("aggregates"));
    }

    @Test
    void unknownCategoryAnswers400WithItsCode() throws Exception {
        mvc.perform(get("/api/catalogs/materials").param("category", "wood"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_MATERIAL_CATEGORY"));
    }

    @Test
    void idThatIsNotAUuidAnswers400() throws Exception {
        mvc.perform(get("/api/catalogs/project-types/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void unknownProjectTypeAnswers404() throws Exception {
        mvc.perform(get("/api/catalogs/project-types/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROJECT_TYPE_NOT_FOUND"));
    }
}
