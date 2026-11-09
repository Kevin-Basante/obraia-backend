package com.obraia.backend.report;

/** A PDF ready to download. */
public record ReportFile(String filename, byte[] content) {
}
