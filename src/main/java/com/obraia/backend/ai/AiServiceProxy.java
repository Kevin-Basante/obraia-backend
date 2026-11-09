package com.obraia.backend.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.obraia.backend.config.AiProperties;
import com.obraia.backend.report.ReportSummary;

/**
 * Proxy of the AI service. It has the same interface as the adapter and
 * stands in front of it, so the callers do not notice it:
 * <ul>
 * <li>Protection: refuses the call when the AI service is not configured
 * and limits how many analyses and how many report summaries each project
 * makes, because the free plan of the model has a quota. Only the calls
 * that work count.</li>
 * <li>Cache: keeps the last analyses by the content of the file, and the
 * last summaries by the figures of the report, so the same question is not
 * sent twice.</li>
 * <li>Lazy creation: the real adapter (and its HTTP client) is only
 * created the first time a blueprint has to be read.</li>
 * </ul>
 */
@Component
@Primary
public class AiServiceProxy implements AiServiceClient {

    private final AiProperties properties;
    private final ObjectProvider<AiServiceAdapter> adapterProvider;
    private final Clock clock;
    private final Map<String, BlueprintAnalysis> cache;
    private final Map<String, ReportSummary> summaries;
    private final Map<UUID, Deque<Instant>> callsByProject = new HashMap<>();
    private final Map<UUID, Deque<Instant>> summariesByProject = new HashMap<>();
    private AiServiceClient adapter;

    public AiServiceProxy(AiProperties properties, ObjectProvider<AiServiceAdapter> adapterProvider, Clock clock) {
        this.properties = properties;
        this.adapterProvider = adapterProvider;
        this.clock = clock;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, BlueprintAnalysis> eldest) {
                return size() > properties.cacheSize();
            }
        });
        this.summaries = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ReportSummary> eldest) {
                return size() > properties.cacheSize();
            }
        });
    }

    @Override
    public boolean available() {
        return properties.enabled();
    }

    @Override
    public BlueprintAnalysis analyze(AnalysisRequest request) {
        if (!available()) {
            throw new AiServiceDisabledException();
        }
        String key = cacheKey(request);
        BlueprintAnalysis cached = cache.get(key);
        if (cached != null) {
            return cached.asCached();
        }
        Instant call = registerCall(callsByProject, request.projectId());
        BlueprintAnalysis analysis;
        try {
            analysis = adapter().analyze(request);
        } catch (AiServiceException ex) {
            // A failed analysis (for example, the model was busy) does not use up the limit.
            releaseCall(callsByProject, request.projectId(), call);
            throw ex;
        }
        cache.put(key, analysis);
        return analysis;
    }

    /** Same figures, same summary: a report downloaded twice asks the AI once. */
    @Override
    public ReportSummary summarize(SummaryRequest request) {
        if (!available()) {
            throw new AiServiceDisabledException();
        }
        String key = summaryKey(request);
        ReportSummary cached = summaries.get(key);
        if (cached != null) {
            return cached;
        }
        Instant call = registerCall(summariesByProject, request.projectId());
        ReportSummary summary;
        try {
            summary = adapter().summarize(request);
        } catch (AiServiceException ex) {
            releaseCall(summariesByProject, request.projectId(), call);
            throw ex;
        }
        summaries.put(key, summary);
        return summary;
    }

    /** Counts the call, or refuses it when the project used all the calls of the window. */
    private synchronized Instant registerCall(Map<UUID, Deque<Instant>> callsOf, UUID projectId) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(properties.limitWindow());
        Deque<Instant> calls = callsOf.computeIfAbsent(projectId, id -> new ArrayDeque<>());
        while (!calls.isEmpty() && !calls.peekFirst().isAfter(windowStart)) {
            calls.pollFirst();
        }
        if (calls.size() >= properties.maxAnalysesPerProject()) {
            throw new AiLimitReachedException(properties.maxAnalysesPerProject(), properties.limitWindow());
        }
        calls.addLast(now);
        return now;
    }

    private synchronized void releaseCall(Map<UUID, Deque<Instant>> callsOf, UUID projectId, Instant call) {
        Deque<Instant> calls = callsOf.get(projectId);
        if (calls != null) {
            calls.removeLastOccurrence(call);
        }
    }

    private synchronized AiServiceClient adapter() {
        if (adapter == null) {
            adapter = adapterProvider.getObject();
        }
        return adapter;
    }

    /** The same file read for a project with the same floors gives the same answer. */
    static String cacheKey(AnalysisRequest request) {
        return sha256(request.content()) + ":" + request.floors();
    }

    /** The same figures in the same language and report give the same summary. */
    static String summaryKey(SummaryRequest request) {
        String text = request.language() + "|" + request.reportType() + "|" + request.figures();
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
