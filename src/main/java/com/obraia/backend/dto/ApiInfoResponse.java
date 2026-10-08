package com.obraia.backend.dto;

/** Response of GET /: general information about the API. */
public record ApiInfoResponse(
        String name,
        String description,
        String status,
        String docs,
        String health) {
}
