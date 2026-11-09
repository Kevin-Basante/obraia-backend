package com.obraia.backend.ai;

import java.util.List;

/** Answer of POST /api/v1/reports/summary of obraia-ia. Only the adapter uses it. */
record AiSummaryPayload(String summary, List<String> recommendations, String model) {
}
