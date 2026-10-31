package com.obraia.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Quotation settings (prefix "app.quotation").
 *
 * @param laborDailyRate cost in COP of one working day of the crew
 * @param vatPct         VAT added to labor and transport, in percent
 */
@ConfigurationProperties(prefix = "app.quotation")
public record QuotationProperties(BigDecimal laborDailyRate, BigDecimal vatPct) {
}
