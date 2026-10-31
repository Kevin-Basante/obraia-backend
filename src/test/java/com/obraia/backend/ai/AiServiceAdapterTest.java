package com.obraia.backend.ai;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.support.BlueprintFixtures;

/** The adapter against a fake obraia-ia: no real HTTP call is made. */
class AiServiceAdapterTest {

    private static final String URL = "https://obraia-ia.test";
    private static final AnalysisRequest REQUEST = new AnalysisRequest(UUID.randomUUID(), "plano.png",
            BlueprintFixtures.PNG, 2);

    private MockRestServiceServer server;
    private AiServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL)
                .defaultHeader(AiServiceAdapter.TOKEN_HEADER, "secret");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AiServiceAdapter(builder.build());
    }

    @Test
    void sendsTheFileAndTheFloorsWithTheToken() {
        server.expect(requestTo(URL + AiServiceAdapter.ANALYZE_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AiServiceAdapter.TOKEN_HEADER, "secret"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("filename=\"plano.png\"")))
                .andExpect(content().string(containsString("name=\"floors\"")))
                .andRespond(withSuccess("""
                        {"measurements": [], "warnings": [], "discarded": 0, "model": "gemini-test", "elapsed_ms": 5}
                        """, MediaType.APPLICATION_JSON));

        adapter.analyze(REQUEST);

        server.verify();
    }

    @Test
    void translatesTheAnswerToTheDomain() {
        server.expect(requestTo(URL + AiServiceAdapter.ANALYZE_PATH)).andRespond(withSuccess("""
                {
                  "measurements": [
                    {"element_type": "wall", "floor_number": 1, "length_m": 36.0, "width_m": null,
                     "height_m": 2.4, "quantity": 2, "notes": "Muros de fachada"},
                    {"element_type": "slab", "floor_number": 2, "length_m": 9.0, "width_m": 8.0,
                     "height_m": 0.12, "quantity": 1, "notes": null}
                  ],
                  "warnings": ["Escala no visible"],
                  "discarded": 1,
                  "model": "gemini-test",
                  "elapsed_ms": 8421
                }
                """, MediaType.APPLICATION_JSON));

        BlueprintAnalysis analysis = adapter.analyze(REQUEST);

        assertEquals(2, analysis.measurements().size());
        AiMeasurement wall = analysis.measurements().get(0);
        assertEquals(ElementType.WALL, wall.elementType());
        assertEquals(new BigDecimal("36.00"), wall.lengthM());
        assertNull(wall.widthM());
        assertEquals(new BigDecimal("2.40"), wall.heightM());
        assertEquals(2, wall.quantity());
        assertEquals(new BigDecimal("0.12"), analysis.measurements().get(1).heightM());
        assertEquals(List.of("Escala no visible"), analysis.warnings());
        assertEquals(1, analysis.discarded());
        assertEquals("gemini-test", analysis.model());
        assertFalse(analysis.cached());
    }

    @Test
    void unknownElementsAreCountedAsDiscarded() {
        server.expect(requestTo(URL + AiServiceAdapter.ANALYZE_PATH)).andRespond(withSuccess("""
                {"measurements": [{"element_type": "stairs", "floor_number": 1, "length_m": 3.0,
                  "width_m": 1.0, "height_m": 2.6, "quantity": 1, "notes": null}],
                 "warnings": null, "discarded": 0, "model": "gemini-test", "elapsed_ms": 5}
                """, MediaType.APPLICATION_JSON));

        BlueprintAnalysis analysis = adapter.analyze(REQUEST);

        assertEquals(0, analysis.measurements().size());
        assertEquals(1, analysis.discarded());
        assertEquals(List.of(), analysis.warnings());
    }

    @Test
    void answerThatIsNotJsonBecomesAiServiceFailed() {
        server.expect(requestTo(URL + AiServiceAdapter.ANALYZE_PATH))
                .andRespond(withSuccess("<html>Service waking up</html>", MediaType.APPLICATION_JSON));

        assertThrows(AiServiceException.class, () -> adapter.analyze(REQUEST));
    }

    @Test
    void answerWithoutMeasurementsBecomesAiServiceFailed() {
        server.expect(requestTo(URL + AiServiceAdapter.ANALYZE_PATH))
                .andRespond(withSuccess("{\"warnings\": [], \"discarded\": 0}", MediaType.APPLICATION_JSON));

        AiServiceException ex = assertThrows(AiServiceException.class, () -> adapter.analyze(REQUEST));

        assertEquals("The AI service answered without measurements", ex.getMessage());
    }
}
