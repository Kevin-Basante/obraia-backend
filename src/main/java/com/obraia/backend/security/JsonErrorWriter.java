package com.obraia.backend.security;

import java.io.IOException;

import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the error JSON by hand: security errors happen before the request
 * reaches the controllers, so GlobalExceptionHandler cannot handle them.
 * The code and message are constants without quotes, so no escaping is needed.
 */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse response, String code, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}");
    }
}
