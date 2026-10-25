package com.obraia.backend.dto.catalog;

import com.obraia.backend.entity.catalog.MaterialCategory;

/** A code the API accepts as a filter and the text to show for it. */
public record CategoryResponse(String code, String label) {

    public static CategoryResponse from(MaterialCategory category) {
        return new CategoryResponse(category.getCode(), category.getLabel());
    }
}
