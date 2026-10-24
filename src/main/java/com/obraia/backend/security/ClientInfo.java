package com.obraia.backend.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP address and browser of the request, saved in the security log.
 * Render sits behind a proxy, so the real IP comes in X-Forwarded-For.
 */
public record ClientInfo(String ipAddress, String userAgent) {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 500;

    public static ClientInfo from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return new ClientInfo(truncate(ip, MAX_IP_LENGTH),
                truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
