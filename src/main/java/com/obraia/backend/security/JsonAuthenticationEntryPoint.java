package com.obraia.backend.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers 401 with the same JSON error shape as the rest of the API when a
 * protected route is called without a valid access token.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        boolean tokenSent = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
        String code = tokenSent ? "INVALID_ACCESS_TOKEN" : "UNAUTHORIZED";
        String message = tokenSent
                ? "The access token is invalid or has expired"
                : "You must sign in to access this resource";

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        JsonErrorWriter.write(response, code, message);
    }
}
