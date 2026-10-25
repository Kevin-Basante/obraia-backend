package com.obraia.backend.entity.catalog;

import java.math.BigDecimal;

import com.obraia.backend.exception.BadRequestException;

/**
 * Project size. It decides which phase templates apply: a small project
 * has up to 120 m2 of built area, a large one has more.
 */
public enum SizeCategory {

    SMALL("small"),
    LARGE("large");

    /** Largest built area, in m2, of a small project. */
    public static final BigDecimal SMALL_MAX_AREA_M2 = new BigDecimal("120");

    private final String code;

    SizeCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SizeCategory forArea(BigDecimal builtAreaM2) {
        return builtAreaM2.compareTo(SMALL_MAX_AREA_M2) <= 0 ? SMALL : LARGE;
    }

    public static SizeCategory fromCode(String code) {
        for (SizeCategory size : values()) {
            if (size.code.equalsIgnoreCase(code.trim())) {
                return size;
            }
        }
        throw new BadRequestException("INVALID_SIZE_CATEGORY",
                "Unknown size category: " + code + ". Valid values: small, large");
    }
}
