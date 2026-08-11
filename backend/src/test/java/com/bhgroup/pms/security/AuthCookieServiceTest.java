package com.bhgroup.pms.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhgroup.pms.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class AuthCookieServiceTest {

    private AppProperties appPropertiesWithSecure(boolean secure) {
        AppProperties properties = new AppProperties();
        AppProperties.Security security = new AppProperties.Security();
        security.setRefreshCookieSecure(secure);
        properties.setSecurity(security);
        return properties;
    }

    @Test
    void buildCookie_isHttpOnlyAndScopedToAuthPath() {
        AuthCookieService service = new AuthCookieService(appPropertiesWithSecure(false));

        ResponseCookie cookie = service.buildCookie("raw-token-value", 604_800_000L);

        assertThat(cookie.getName()).isEqualTo(AuthCookieService.COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("raw-token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().toMillis()).isEqualTo(604_800_000L);
    }

    @Test
    void buildCookie_secureFlagFollowsConfiguration() {
        assertThat(new AuthCookieService(appPropertiesWithSecure(true))
                .buildCookie("t", 1000L).isSecure()).isTrue();
        assertThat(new AuthCookieService(appPropertiesWithSecure(false))
                .buildCookie("t", 1000L).isSecure()).isFalse();
    }

    @Test
    void buildClearingCookie_isEmptyAndExpiresImmediately() {
        AuthCookieService service = new AuthCookieService(appPropertiesWithSecure(false));

        ResponseCookie cookie = service.buildClearingCookie();

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().isZero()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}
