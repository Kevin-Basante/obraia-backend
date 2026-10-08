package com.obraia.backend.dto;

/**
 * Every error has the same JSON shape:
 * {@code { "error": { "code": "...", "message": "..." } }}
 */
public record ErrorResponse(ErrorDetail error) {

    public record ErrorDetail(String code, String message) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetail(code, message));
    }
}
