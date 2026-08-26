package com.bhgroup.pms.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Same regression class as MfaEnforcementFilterTest: this filter also used
 * to build `new ObjectMapper()` for a body (ApiError) that carries an
 * Instant, so every 429 it emitted would have thrown mid-write instead of
 * returning the intended rate-limit response. Runs with a JavaTimeModule
 * mapper like the real Spring-managed bean to prove that's fixed, and
 * exercises the MFA verify/recovery rules added alongside it.
 */
class RateLimitingFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RateLimitingFilter filter = new RateLimitingFilter(objectMapper);

    @Test
    void blocksMfaVerifyLoginAfterTheLimitAndReturnsCleanJson() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = doPost("/api/v1/auth/mfa/verify-login");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = doPost("/api/v1/auth/mfa/verify-login");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentType()).isEqualTo("application/json");
        var body = objectMapper.readTree(blocked.getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void blocksMfaVerifyRecoveryAfterTheLimitAndReturnsCleanJson() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = doPost("/api/v1/auth/mfa/verify-recovery");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = doPost("/api/v1/auth/mfa/verify-recovery");

        assertThat(blocked.getStatus()).isEqualTo(429);
        var body = objectMapper.readTree(blocked.getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void blocksAssistantChatAfterTheLimitAndReturnsCleanJson() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doPost("/api/v1/assistant/chat");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = doPost("/api/v1/assistant/chat");

        assertThat(blocked.getStatus()).isEqualTo(429);
        var body = objectMapper.readTree(blocked.getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void doesNotRateLimitUnrelatedEndpoints() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doPost("/api/v1/properties");
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse doPost(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
