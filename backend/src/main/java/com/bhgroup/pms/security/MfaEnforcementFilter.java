package com.bhgroup.pms.security;

import com.bhgroup.pms.common.response.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Every account must have MFA enabled - there is no self-registration in
 * this system, only invited accounts, so "mandatory MFA on first login for
 * invited accounts" in practice means every account. Until an authenticated
 * user has completed MFA setup, this filter blocks everything except the
 * handful of endpoints needed to actually complete that setup (plus
 * logging out). Requests without an authenticated principal, or for
 * already-public endpoints, pass straight through - this only restricts
 * what a logged-in-but-not-yet-MFA'd user can reach.
 */
public class MfaEnforcementFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PRE_MFA_PATHS = Set.of(
            "/api/v1/auth/me",
            "/api/v1/auth/mfa/setup",
            "/api/v1/auth/mfa/enable",
            "/api/v1/auth/logout"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                && !principal.isMfaEnabled()
                && !ALLOWED_PRE_MFA_PATHS.contains(request.getRequestURI())) {

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError body = ApiError.of("MFA_SETUP_REQUIRED",
                    "Two-factor authentication setup is required before continuing",
                    request.getRequestURI());
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
