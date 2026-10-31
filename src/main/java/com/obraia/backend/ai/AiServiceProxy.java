package com.obraia.backend.ai;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.obraia.backend.config.AiProperties;

/**
 * Proxy of the AI service. It has the same interface as the adapter and
 * stands in front of it, so the callers do not notice it:
 * <ul>
 * <li>Protection: refuses the call when the AI service is not configured.</li>
 * <li>Cache: keeps the last analyses by the content of the file, so the
 * same blueprint is not sent twice.</li>
 * <li>Lazy creation: the real adapter (and its HTTP client) is only
 * created the first time a blueprint has to be read.</li>
 * </ul>
 */
@Component
@Primary
public class AiServiceProxy implements AiServiceClient {

    private final AiProperties properties;
    private final ObjectProvider<AiServiceAdapter> adapterProvider;
    private final Map<String, BlueprintAnalysis> cache;
    private AiServiceClient adapter;

    public AiServiceProxy(AiProperties properties, ObjectProvider<AiServiceAdapter> adapterProvider) {
        this.properties = properties;
        this.adapterProvider = adapterProvider;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, BlueprintAnalysis> eldest) {
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
        BlueprintAnalysis analysis = adapter().analyze(request);
        cache.put(key, analysis);
        return analysis;
    }

    private synchronized AiServiceClient adapter() {
        if (adapter == null) {
            adapter = adapterProvider.getObject();
        }
        return adapter;
    }

    /** The same file read for a project with the same floors gives the same answer. */
    static String cacheKey(AnalysisRequest request) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(request.content());
            return HexFormat.of().formatHex(hash) + ":" + request.floors();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
