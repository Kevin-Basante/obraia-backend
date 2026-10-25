package com.obraia.backend.entity.catalog;

import com.obraia.backend.exception.BadRequestException;

/**
 * Parts of the building measured in a blueprint. Each one has consumption
 * formulas per base unit (m3 of concrete, m2 of wall, floor or roof).
 * The order is the order of construction.
 */
public enum ElementType {

    FOUNDATION("foundation", "Foundation"),
    COLUMN("column", "Columns"),
    BEAM("beam", "Beams"),
    SLAB("slab", "Slabs"),
    WALL("wall", "Walls"),
    FLOOR("floor", "Floor base"),
    ROOF("roof", "Roof");

    private final String code;
    private final String label;

    ElementType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ElementType fromCode(String code) {
        for (ElementType element : values()) {
            if (element.code.equalsIgnoreCase(code.trim())) {
                return element;
            }
        }
        throw new BadRequestException("INVALID_ELEMENT_TYPE", "Unknown element type: " + code
                + ". Valid values: foundation, column, beam, slab, wall, floor, roof");
    }
}
