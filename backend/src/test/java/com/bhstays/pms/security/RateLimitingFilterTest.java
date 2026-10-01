package com.bhstays.pms.security;

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
    void blocksAssistantHandoffAfterTheLimitAndReturnsCleanJson() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = doPost("/api/v1/assistant/handoff");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = doPost("/api/v1/assistant/handoff");

        assertThat(blocked.getStatus()).isEqualTo(429);
        var body = objectMapper.readTree(blocked.getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void blocksAssistantChatPollingAfterTheLimit_butDoesNotAffectThePostChatRule() throws Exception {
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/assistant/chat/some-token/messages");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletRequest blockedRequest =
                new MockHttpServletRequest("GET", "/api/v1/assistant/chat/some-token/messages");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, new MockFilterChain());
        assertThat(blockedResponse.getStatus()).isEqualTo(429);

        // The POST /chat rule (20/10min) is independent - still open.
        MockHttpServletResponse postChatResponse = doPost("/api/v1/assistant/chat");
        assertThat(postChatResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotRateLimitUnrelatedEndpoints() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doPost("/api/v1/properties");
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void capsAiPricingRecommendationsPerProperty() throws Exception {
        String propertyA = "/api/v1/properties/11111111-1111-1111-1111-111111111111/pricing/ai-recommendation";
        String propertyB = "/api/v1/properties/22222222-2222-2222-2222-222222222222/pricing/ai-recommendation";

        for (int i = 0; i < 10; i++) {
            assertThat(doPost(propertyA).getStatus()).isEqualTo(200);
        }
        assertThat(doPost(propertyA).getStatus()).isEqualTo(429);

        // A different property has its own allowance - one being re-analysed
        // must not lock the rest of the portfolio out.
        assertThat(doPost(propertyB).getStatus()).isEqualTo(200);
    }

    @Test
    void leavesOtherPropertyEndpointsAlone() throws Exception {
        // The rule is anchored on the /pricing/ai-recommendation suffix, so the
        // shared /api/v1/properties/ prefix must not throttle anything else.
        for (int i = 0; i < 25; i++) {
            assertThat(doPost("/api/v1/properties/11111111-1111-1111-1111-111111111111/photos")
                    .getStatus()).isEqualTo(200);
            assertThat(doPost("/api/v1/properties/11111111-1111-1111-1111-111111111111/pricing/config")
                    .getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse doPost(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
