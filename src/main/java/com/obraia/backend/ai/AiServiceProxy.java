package com.obraia.backend.ai;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.obraia.backend.config.AiProperties;

/**
 * Proxy of the AI service. It has the same interface as the adapter and
 * stands in front of it, so the callers do not notice it:
 * <ul>
 * <li>Protection: refuses the call when the AI service is not configured.</li>
 * <li>Lazy creation: the real adapter (and its HTTP client) is only
 * created the first time a blueprint has to be read.</li>
 * </ul>
 */
@Component
@Primary
public class AiServiceProxy implements AiServiceClient {

    private final AiProperties properties;
    private final ObjectProvider<AiServiceAdapter> adapterProvider;
    private AiServiceClient adapter;

    public AiServiceProxy(AiProperties properties, ObjectProvider<AiServiceAdapter> adapterProvider) {
        this.properties = properties;
        this.adapterProvider = adapterProvider;
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
        return adapter().analyze(request);
    }

    private synchronized AiServiceClient adapter() {
        if (adapter == null) {
            adapter = adapterProvider.getObject();
        }
        return adapter;
    }
}
