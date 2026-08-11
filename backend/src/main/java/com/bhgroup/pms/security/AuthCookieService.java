package com.bhgroup.pms.security;

import com.bhgroup.pms.config.AppProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the httpOnly refresh-token cookie. The refresh token never appears
 * in a JSON response body or in JS-reachable storage - only here, scoped to
 * /api/v1/auth so it isn't attached to every request.
 */
@Component
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AppProperties appProperties;

    public ResponseCookie buildCookie(String rawRefreshToken, long maxAgeMs) {
        return baseCookie(rawRefreshToken)
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    public ResponseCookie buildClearingCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(appProperties.getSecurity().isRefreshCookieSecure())
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }
}
