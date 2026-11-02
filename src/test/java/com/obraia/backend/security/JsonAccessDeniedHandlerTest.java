package com.obraia.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JsonAccessDeniedHandlerTest {

    @Test
    void answers403WithTheErrorJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JsonAccessDeniedHandler().handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("no role"));

        assertEquals(403, response.getStatus());
        assertEquals("{\"error\":{\"code\":\"FORBIDDEN\","
                + "\"message\":\"You do not have permission to perform this action\"}}",
                response.getContentAsString());
    }
}
