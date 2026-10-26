package com.obraia.backend.entity.blueprint;

/**
 * Kind of blueprint file. The AI reads each one differently: a photo or a
 * scanned PDF needs OCR, a vector PDF already contains its text.
 */
public enum BlueprintFileType {

    IMAGE("image", "Image"),
    SCANNED_PDF("scanned_pdf", "Scanned PDF"),
    VECTOR_PDF("vector_pdf", "Vector PDF");

    private final String code;
    private final String label;

    BlueprintFileType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static BlueprintFileType fromCode(String code) {
        for (BlueprintFileType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown blueprint file type: " + code);
    }
}
