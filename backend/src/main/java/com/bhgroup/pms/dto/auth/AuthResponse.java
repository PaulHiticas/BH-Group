package com.bhgroup.pms.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code refreshToken} is only populated on the object the controller uses
 * to build the httpOnly cookie - {@link #withoutRefreshToken()} is what
 * actually goes in the JSON response body, so the raw token is never
 * present in a place JavaScript (or a log of the response) could read it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs / 1000, user);
    }

    public AuthResponse withoutRefreshToken() {
        return new AuthResponse(accessToken, null, tokenType, expiresIn, user);
    }
}
