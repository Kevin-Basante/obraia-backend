package com.obraia.backend.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.auth.TokenRequest;
import com.obraia.backend.storage.StoredFileNotFoundException;

/** Every error leaves the API with the same JSON shape: { error: { code, message } }. */
class GlobalExceptionHandlerTest {

    @RestController
    static class FailingRoutes {

        @GetMapping("/items/{id}")
        String item(@PathVariable UUID id) {
            return id.toString();
        }

        @PostMapping("/items")
        String create(@RequestBody TokenRequest request) {
            return request.token();
        }

        @GetMapping("/files")
        String file() {
            throw new StoredFileNotFoundException("projects/a/plan.pdf");
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("database password is wrong");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FailingRoutes())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void anIdThatIsNotAUuidIs400() throws Exception {
        mvc.perform(get("/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.error.message").value("Invalid value for 'id': abc"));
    }

    @Test
    void brokenJsonIs400() throws Exception {
        mvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content("{\"token\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void anotherContentTypeIs415() throws Exception {
        mvc.perform(post("/items").contentType(MediaType.TEXT_PLAIN).content("token"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void aWrongMethodIs405() throws Exception {
        mvc.perform(delete("/items"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void aLostFileIs404() throws Exception {
        mvc.perform(get("/files"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void unexpectedErrorsHideTheirDetail() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred"));
    }
}
