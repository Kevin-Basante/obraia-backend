package com.obraia.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the PDF reports (prefix "app.reports").
 *
 * @param approvalLinkTtl how long the creator has to approve a request for the full report
 * @param downloadWindow  how long the member can download the full report after the approval
 * @param shareLinkTtl    how long a shared link of the public report works
 * @param summaryTimeout  how long a download waits for the summary of the AI before going on without it
 */
@ConfigurationProperties(prefix = "app.reports")
public record ReportProperties(Duration approvalLinkTtl, Duration downloadWindow, Duration shareLinkTtl,
        Duration summaryTimeout) {
}
