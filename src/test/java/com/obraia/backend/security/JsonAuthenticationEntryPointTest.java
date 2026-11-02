package com.obraia.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class JsonAuthenticationEntryPointTest {

    private final JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();

    @Test
    void withoutTokenAsksToSignIn() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response, new InsufficientAuthenticationException("no"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"You must sign in to access this resource\"}}",
                response.getContentAsString());
    }

    @Test
    void withABadTokenSaysTheTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("expired"));

        assertEquals(401, response.getStatus());
        assertEquals(true, response.getContentAsString().contains("\"INVALID_ACCESS_TOKEN\""));
    }
}
