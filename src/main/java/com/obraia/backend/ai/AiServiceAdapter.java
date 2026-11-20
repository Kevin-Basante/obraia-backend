package com.obraia.backend.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.obraia.backend.report.ReportSummary;

/**
 * Adapter between the backend and obraia-ia. The Python service speaks
 * HTTP with multipart forms or JSON, snake_case names and plain numbers; the
 * backend works with {@link AiServiceClient}, ElementType and BigDecimal.
 * This class translates one into the other, so nothing else in the
 * backend knows how the AI service is called.
 */
public class AiServiceAdapter implements AiServiceClient {

    static final String ANALYZE_PATH = "/api/v1/blueprints/analyze";
    static final String SUMMARY_PATH = "/api/v1/reports/summary";
    static final String HEALTH_PATH = "/health";
    static final String TOKEN_HEADER = "X-Service-Token";
    /** Code of obraia-ia when Gemini stays overloaded after its retries. */
    static final String BUSY_CODE = "ANALYZER_BUSY";

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
            throw failure(ex);
        } catch (ResourceAccessException ex) {
            throw notReached(ex);
        } catch (RestClientException ex) {
            throw new AiServiceException("The answer of the AI service could not be read", ex);
        }
        if (payload == null || payload.measurements() == null) {
            throw new AiServiceException("The AI service answered without measurements", null);
        }
        return toAnalysis(payload);
    }

    @Override
    public ReportSummary summarize(SummaryRequest request) {
        AiSummaryPayload payload;
        try {
            payload = restClient.post()
                    .uri(SUMMARY_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("language", request.language(), "report_type", request.reportType(),
                            "figures", request.figures()))
                    .retrieve()
                    .body(AiSummaryPayload.class);
        } catch (RestClientResponseException ex) {
            throw failure(ex);
        } catch (ResourceAccessException ex) {
            throw notReached(ex);
        } catch (RestClientException ex) {
            throw new AiServiceException("The answer of the AI service could not be read", ex);
        }
        if (payload == null || payload.summary() == null || payload.summary().isBlank()) {
            throw new AiServiceException("The AI service answered without a summary", null);
        }
        return new ReportSummary(payload.summary().strip(), payload.recommendations(), payload.model());
    }

    @Override
    public void wake() {
        try {
            restClient.get().uri(HEALTH_PATH).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw new AiServiceException("The AI service did not answer the health check", ex);
        }
    }

    /**
     * A connection that failed never reached obraia-ia. A call that waited
     * too long may have reached the model, so it is not marked as unreached.
     */
    private static AiServiceException notReached(ResourceAccessException ex) {
        String message = "The AI service could not be reached or did not answer in time";
        if (ex.getCause() instanceof SocketTimeoutException) {
            return new AiServiceException(message, ex);
        }
        return new AiServiceUnreachableException(message, ex);
    }

    /** The error of obraia-ia, with its own message when it sent one. */
    private static AiServiceException failure(RestClientResponseException ex) {
        String message = "The AI service answered " + ex.getStatusCode().value();
        AiServiceError.Body error = errorBody(ex);
        if (error == null && isRenderError(ex.getStatusCode().value())) {
            // Not the JSON of obraia-ia: the answer of Render while the service sleeps or starts.
            return new AiServiceUnreachableException(message, ex);
        }
        if (error != null && BUSY_CODE.equals(error.code())) {
            return new AiServiceBusyException(ex);
        }
        if (error != null && error.message() != null && !error.message().isBlank()) {
            message += ": " + error.message();
        }
        return new AiServiceException(message, ex);
    }

    /**
     * Statuses Render answers itself while obraia-ia sleeps or starts. A call
     * from another Render service, like this backend, gets 429 and does not
     * wake it; only a visit from outside Render does.
     */
    private static boolean isRenderError(int status) {
        return status == 429 || status == 502 || status == 503 || status == 504;
    }

    /** The error body, or null when it is not the JSON of obraia-ia (for example, a page of Render). */
    private static AiServiceError.Body errorBody(RestClientResponseException ex) {
        try {
            AiServiceError body = ex.getResponseBodyAs(AiServiceError.class);
            return body == null ? null : body.error();
        } catch (RuntimeException notJson) {
            return null;
        }
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
