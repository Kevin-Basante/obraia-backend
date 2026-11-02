package com.obraia.backend.entity.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** The four blueprint enums are stored as their codes, the values allowed by the CHECK constraints. */
class BlueprintConvertersTest {

    @Test
    void fileTypeRoundTrip() {
        BlueprintFileTypeConverter converter = new BlueprintFileTypeConverter();
        assertEquals("scanned_pdf", converter.convertToDatabaseColumn(BlueprintFileType.SCANNED_PDF));
        assertEquals(BlueprintFileType.VECTOR_PDF, converter.convertToEntityAttribute("vector_pdf"));
    }

    @Test
    void measurementSourceRoundTrip() {
        MeasurementSourceConverter converter = new MeasurementSourceConverter();
        assertEquals("ocr", converter.convertToDatabaseColumn(MeasurementSource.OCR));
        assertEquals(MeasurementSource.MANUAL, converter.convertToEntityAttribute("manual"));
    }

    @Test
    void processingStatusRoundTrip() {
        ProcessingStatusConverter converter = new ProcessingStatusConverter();
        assertEquals("completed", converter.convertToDatabaseColumn(ProcessingStatus.COMPLETED));
        assertEquals(ProcessingStatus.FAILED, converter.convertToEntityAttribute("failed"));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void storageProviderRoundTrip() {
        StorageProviderConverter converter = new StorageProviderConverter();
        assertEquals("external", converter.convertToDatabaseColumn(StorageProvider.EXTERNAL));
        assertEquals(StorageProvider.LOCAL, converter.convertToEntityAttribute("local"));
    }

    @Test
    void anUnknownCodeIsAnError() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessingStatusConverter().convertToEntityAttribute("lost"));
    }
}
