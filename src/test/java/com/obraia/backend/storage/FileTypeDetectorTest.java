package com.obraia.backend.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.blueprint.BlueprintFileType;
import com.obraia.backend.storage.FileTypeDetector.DetectedFile;

class FileTypeDetectorTest {

    @Test
    void pdfWithFontsIsAVectorDrawing() {
        DetectedFile file = detect("%PDF-1.7\n1 0 obj << /Type /Font /BaseFont /Helvetica >>");

        assertEquals(BlueprintFileType.VECTOR_PDF, file.fileType());
        assertEquals("application/pdf", file.mimeType());
    }

    @Test
    void pdfWithoutFontsIsAScan() {
        assertEquals(BlueprintFileType.SCANNED_PDF, detect("%PDF-1.4\n1 0 obj << /Type /XObject >>").fileType());
    }

    @Test
    void jpegIsAnImage() {
        DetectedFile file = FileTypeDetector.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x0E})
                .orElseThrow();

        assertEquals(BlueprintFileType.IMAGE, file.fileType());
        assertEquals("jpg", file.extension());
    }

    @Test
    void webpIsAnImage() {
        assertEquals("image/webp", detect("RIFF\0\0\0\0WEBPVP8 ").mimeType());
    }

    @Test
    void renamedTextFileIsRejected() {
        assertTrue(FileTypeDetector.detect("plan.pdf is not a real pdf".getBytes(StandardCharsets.US_ASCII))
                .isEmpty());
    }

    @Test
    void tinyFileIsRejected() {
        assertTrue(FileTypeDetector.detect(new byte[] {(byte) 0xFF}).isEmpty());
    }

    private static DetectedFile detect(String content) {
        return FileTypeDetector.detect(content.getBytes(StandardCharsets.ISO_8859_1)).orElseThrow();
    }
}
