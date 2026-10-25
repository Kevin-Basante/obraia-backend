package com.obraia.backend.entity.catalog;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.obraia.backend.exception.BadRequestException;

/** Groups of the material catalog. The code is the value stored in the database. */
public enum MaterialCategory {

    CEMENT_CONCRETE("cement_concrete", "Cement and concrete"),
    AGGREGATES("aggregates", "Aggregates"),
    MASONRY("masonry", "Masonry"),
    STEEL("steel", "Steel"),
    ROOFING("roofing", "Roofing"),
    FINISHES("finishes", "Finishes"),
    OTHER("other", "Other");

    private final String code;
    private final String label;

    MaterialCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** Finds the category of a code sent by the client; unknown codes are a 400 error. */
    public static MaterialCategory fromCode(String code) {
        return Arrays.stream(values())
                .filter(category -> category.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("INVALID_MATERIAL_CATEGORY",
                        "Unknown material category: " + code + ". Valid values: " + validCodes()));
    }

    private static String validCodes() {
        return Arrays.stream(values()).map(MaterialCategory::getCode).collect(Collectors.joining(", "));
    }
}
