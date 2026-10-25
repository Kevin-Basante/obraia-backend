package com.obraia.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory cache for the catalogs. They only change through migrations,
 * which restart the server, so the cache never needs to expire.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PROJECT_TYPES = "projectTypes";
    public static final String LOCATIONS = "locations";
    public static final String MATERIALS = "materials";
    public static final String PHASE_TEMPLATES = "phaseTemplates";
    public static final String MATERIAL_FORMULAS = "materialFormulas";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PROJECT_TYPES, LOCATIONS, MATERIALS, PHASE_TEMPLATES, MATERIAL_FORMULAS);
    }
}
