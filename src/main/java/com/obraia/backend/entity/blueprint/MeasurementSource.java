package com.obraia.backend.entity.blueprint;

/** Who took a measurement: the user by hand, or the AI reading the blueprint (block 7). */
public enum MeasurementSource {

    MANUAL("manual", "Manual"),
    OCR("ocr", "AI");

    private final String code;
    private final String label;

    MeasurementSource(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static MeasurementSource fromCode(String code) {
        for (MeasurementSource source : values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown measurement source: " + code);
    }
}
