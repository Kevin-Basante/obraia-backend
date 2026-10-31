package com.obraia.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory cache. The catalogs only change through migrations, which
 * restart the server, so they never need to expire. The budget versions
 * of a project are removed from the cache each time a quotation or an
 * approval changes them.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PROJECT_TYPES = "projectTypes";
    public static final String LOCATIONS = "locations";
    public static final String MATERIALS = "materials";
    public static final String PHASE_TEMPLATES = "phaseTemplates";
    public static final String MATERIAL_FORMULAS = "materialFormulas";
    public static final String BUDGET_VERSIONS = "budgetVersions";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PROJECT_TYPES, LOCATIONS, MATERIALS, PHASE_TEMPLATES,
                MATERIAL_FORMULAS, BUDGET_VERSIONS);
    }
}
