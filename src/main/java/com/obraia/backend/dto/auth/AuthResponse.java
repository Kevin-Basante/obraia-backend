package com.obraia.backend.dto.auth;

import com.obraia.backend.dto.user.UserResponse;

/**
 * Answer of login and refresh.
 *
 * @param accessToken  JWT to send in the header "Authorization: Bearer ..."
 * @param tokenType    always "Bearer"
 * @param expiresIn    seconds until the access token expires
 * @param refreshToken token to get a new access token when it expires
 * @param user         the signed-in user
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
        UserResponse user) {

    public static AuthResponse bearer(String accessToken, long expiresIn, String refreshToken, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, refreshToken, user);
    }
}
