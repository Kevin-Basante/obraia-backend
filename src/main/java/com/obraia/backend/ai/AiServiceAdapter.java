package com.obraia.backend.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.obraia.backend.config.AiProperties;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.exception.BadRequestException;

/**
 * Adapter between the backend and obraia-ia. The Python service speaks
 * HTTP with multipart forms, snake_case names and plain numbers; the
 * backend works with {@link AiServiceClient}, ElementType and BigDecimal.
 * This class translates one into the other, so nothing else in the
 * backend knows how the AI service is called.
 */
public class AiServiceAdapter implements AiServiceClient {

    static final String ANALYZE_PATH = "/api/v1/blueprints/analyze";
    static final String TOKEN_HEADER = "X-Service-Token";

    private final RestClient restClient;

    public AiServiceAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Adapter with the URL, token and timeouts of the settings. */
    public static AiServiceAdapter create(AiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return new AiServiceAdapter(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(TOKEN_HEADER, properties.serviceToken())
                .requestFactory(requestFactory)
                .build());
    }

    @Override
    public BlueprintAnalysis analyze(AnalysisRequest request) {
        AiServicePayload payload;
        try {
            payload = restClient.post()
                    .uri(ANALYZE_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form(request))
                    .retrieve()
                    .body(AiServicePayload.class);
        } catch (RestClientResponseException ex) {
            throw new AiServiceException("The AI service answered " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new AiServiceException("The AI service could not be reached or did not answer in time", ex);
        } catch (RestClientException ex) {
            throw new AiServiceException("The answer of the AI service could not be read", ex);
        }
        if (payload == null || payload.measurements() == null) {
            throw new AiServiceException("The AI service answered without measurements", null);
        }
        return toAnalysis(payload);
    }

    private static MultiValueMap<String, Object> form(AnalysisRequest request) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        // The filename makes Spring send the bytes as a file part.
        form.add("file", new ByteArrayResource(request.content()) {
            @Override
            public String getFilename() {
                return request.filename();
            }
        });
        form.add("floors", String.valueOf(request.floors()));
        return form;
    }

    /** Elements with a type the backend does not know are counted as discarded. */
    static BlueprintAnalysis toAnalysis(AiServicePayload payload) {
        List<AiMeasurement> measurements = new ArrayList<>();
        int discarded = payload.discarded();
        for (AiServicePayload.Item item : payload.measurements()) {
            ElementType element = elementType(item.elementType());
            if (element == null) {
                discarded++;
                continue;
            }
            measurements.add(new AiMeasurement(element, item.floorNumber(), meters(item.lengthM()),
                    meters(item.widthM()), meters(item.heightM()), item.quantity(), item.notes()));
        }
        List<String> warnings = payload.warnings() == null ? List.of() : payload.warnings();
        return new BlueprintAnalysis(measurements, warnings, discarded, payload.model(), false);
    }

    private static ElementType elementType(String code) {
        try {
            return code == null ? null : ElementType.fromCode(code);
        } catch (BadRequestException ex) {
            return null;
        }
    }

    /** Meters with two decimals, like the columns of blueprint_measurements. */
    private static BigDecimal meters(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
