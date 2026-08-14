package com.bhgroup.pms.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Regression coverage for the bug the code review flagged: the filter used
 * to build its own `new ObjectMapper()`, which has no JavaTimeModule and
 * throws serializing ApiError.timestamp (an Instant) - turning every
 * MFA-required block into an unhandled 500 instead of a clean 403 JSON body.
 * These tests run the filter with the same kind of Spring-managed mapper it
 * gets in production (JavaTimeModule registered) to prove the response is
 * now well-formed.
 */
class MfaEnforcementFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MfaEnforcementFilter filter = new MfaEnforcementFilter(objectMapper);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksAndReturnsCleanJsonWhenMfaIsNotYetEnabled() throws Exception {
        authenticateAs(userWithMfa(false));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(chain.getRequest()).isNull();

        var body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("MFA_SETUP_REQUIRED");
        assertThat(body.get("timestamp").asText()).isNotBlank();
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/properties");
    }

    @Test
    void allowsTheMfaSetupEndpointsThroughEvenWhenNotYetEnabled() throws Exception {
        authenticateAs(userWithMfa(false));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/mfa/setup");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void passesThroughOnceMfaIsEnabled() throws Exception {
        authenticateAs(userWithMfa(true));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void passesThroughUnauthenticatedRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private User userWithMfa(boolean mfaEnabled) {
        User user = User.builder()
                .email("staff@bhgroup.io")
                .firstName("Staff")
                .lastName("Member")
                .role(Role.ADMINISTRATOR)
                .status(UserStatus.ACTIVE)
                .mfaEnabled(mfaEnabled)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
