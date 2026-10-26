package com.obraia.backend.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import com.obraia.backend.entity.blueprint.BlueprintFileType;

/**
 * Recognizes a blueprint by its first bytes instead of trusting the file
 * name or the type sent by the browser. Accepts PDF, PNG, JPEG and WEBP.
 */
public final class FileTypeDetector {

    /** What the file really is. */
    public record DetectedFile(BlueprintFileType fileType, String mimeType, String extension) {
    }

    private static final byte[] PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
    // A PDF made by a drawing program declares fonts for its text; a scan only holds images.
    private static final byte[] FONT = "/Font".getBytes(StandardCharsets.US_ASCII);

    private FileTypeDetector() {
    }

    public static Optional<DetectedFile> detect(byte[] content) {
        if (startsWith(content, PDF, 0)) {
            BlueprintFileType type = contains(content, FONT) ? BlueprintFileType.VECTOR_PDF
                    : BlueprintFileType.SCANNED_PDF;
            return Optional.of(new DetectedFile(type, "application/pdf", "pdf"));
        }
        if (startsWith(content, PNG, 0)) {
            return Optional.of(new DetectedFile(BlueprintFileType.IMAGE, "image/png", "png"));
        }
        if (startsWith(content, JPEG, 0)) {
            return Optional.of(new DetectedFile(BlueprintFileType.IMAGE, "image/jpeg", "jpg"));
        }
        if (startsWith(content, RIFF, 0) && startsWith(content, WEBP, 8)) {
            return Optional.of(new DetectedFile(BlueprintFileType.IMAGE, "image/webp", "webp"));
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, byte[] prefix, int offset) {
        return content.length >= offset + prefix.length
                && Arrays.equals(content, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static boolean contains(byte[] content, byte[] part) {
        for (int i = 0; i + part.length <= content.length; i++) {
            if (Arrays.equals(content, i, i + part.length, part, 0, part.length)) {
                return true;
            }
        }
        return false;
    }
}
