package com.obraia.backend.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

/**
 * Catalog answers can be kept by the browser for a while, so pages that
 * open several times do not ask the server again.
 */
final class CatalogResponses {

    static final Duration BROWSER_CACHE = Duration.ofMinutes(10);

    private CatalogResponses() {
    }

    static <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(BROWSER_CACHE).cachePublic()).body(body);
    }
}
