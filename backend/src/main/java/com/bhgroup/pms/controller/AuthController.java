package com.bhgroup.pms.controller;

import com.bhgroup.pms.dto.auth.AcceptInviteRequest;
import com.bhgroup.pms.dto.auth.AuthResponse;
import com.bhgroup.pms.dto.auth.ForgotPasswordRequest;
import com.bhgroup.pms.dto.auth.InviteInfoResponse;
import com.bhgroup.pms.dto.auth.LoginRequest;
import com.bhgroup.pms.dto.auth.MfaDisableRequest;
import com.bhgroup.pms.dto.auth.MfaEnableRequest;
import com.bhgroup.pms.dto.auth.MfaEnableResponse;
import com.bhgroup.pms.dto.auth.MfaRecoveryLoginRequest;
import com.bhgroup.pms.dto.auth.MfaSetupResponse;
import com.bhgroup.pms.dto.auth.MfaVerifyLoginRequest;
import com.bhgroup.pms.dto.auth.ResetPasswordRequest;
import com.bhgroup.pms.dto.auth.UserResponse;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.exception.UnauthorizedException;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.security.AuthCookieService;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.service.AuthService;
import com.bhgroup.pms.service.mapper.UserMapper;
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, tokens, verification, password reset and MFA")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthCookieService authCookieService;
    private final AppProperties appProperties;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password. Returns tokens (refresh token as an httpOnly cookie), or an MFA challenge if 2FA is enabled")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest request,
                                                       HttpServletRequest servletRequest) {
        Object result = authService.login(request, clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
        if (result instanceof AuthResponse authResponse) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookieFor(authResponse).toString())
                    .body(ApiResponse.success(authResponse.withoutRefreshToken()));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/mfa/verify-login")
    @Operation(summary = "Complete login by verifying a 6-digit TOTP code")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfaLogin(@Valid @RequestBody MfaVerifyLoginRequest request,
                                                                     HttpServletRequest servletRequest) {
        AuthResponse response = authService.verifyMfaLogin(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFor(response).toString())
                .body(ApiResponse.success(response.withoutRefreshToken()));
    }

    @PostMapping("/mfa/verify-recovery")
    @Operation(summary = "Complete login using a one-time recovery code instead of a TOTP code")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfaRecovery(
            @Valid @RequestBody MfaRecoveryLoginRequest request, HttpServletRequest servletRequest) {
        AuthResponse response = authService.verifyMfaRecoveryLogin(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFor(response).toString())
                .body(ApiResponse.success(response.withoutRefreshToken()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the httpOnly refresh-token cookie for a new access/refresh token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = AuthCookieService.COOKIE_NAME, required = false) String refreshTokenCookie,
            HttpServletRequest servletRequest) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        AuthResponse response = authService.refreshToken(refreshTokenCookie, clientIp(servletRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFor(response).toString())
                .body(ApiResponse.success(response.withoutRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the refresh token (from its httpOnly cookie), ending the session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = AuthCookieService.COOKIE_NAME, required = false) String refreshTokenCookie) {
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            authService.logout(refreshTokenCookie);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.buildClearingCookie().toString())
                .body(ApiResponse.message("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.message("If an account exists for this email, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token sent by email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.message("Password reset successfully. Please login with your new password."));
    }

    @GetMapping("/invite/{token}")
    @Operation(summary = "Look up a pending invitation by its token")
    public ResponseEntity<ApiResponse<InviteInfoResponse>> getInvite(@PathVariable String token) {
        InviteInfoResponse response = authService.getInviteInfo(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/invite/accept")
    @Operation(summary = "Accept an invitation by setting a password, activating the account and logging in")
    public ResponseEntity<ApiResponse<AuthResponse>> acceptInvite(@Valid @RequestBody AcceptInviteRequest request,
                                                                   HttpServletRequest servletRequest) {
        AuthResponse response = authService.acceptInvite(request, clientIp(servletRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFor(response).toString())
                .body(ApiResponse.success(response.withoutRefreshToken()));
    }

    @PostMapping("/mfa/setup")
    @Operation(summary = "Generate a new TOTP secret for the authenticated user")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa() {
        MfaSetupResponse response = authService.setupMfa(SecurityUtils.requireCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/enable")
    @Operation(summary = "Confirm and enable MFA by verifying a TOTP code. Returns one-time recovery codes shown only here.")
    public ResponseEntity<ApiResponse<MfaEnableResponse>> enableMfa(@Valid @RequestBody MfaEnableRequest request) {
        MfaEnableResponse response = authService.enableMfa(SecurityUtils.requireCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Two-factor authentication enabled"));
    }

    @PostMapping("/mfa/disable")
    @Operation(summary = "Disable MFA for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> disableMfa(@Valid @RequestBody MfaDisableRequest request) {
        authService.disableMfa(SecurityUtils.requireCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.message("Two-factor authentication disabled"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        UserResponse response = userRepository.findById(SecurityUtils.requireCurrentUserId())
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private ResponseCookie refreshCookieFor(AuthResponse response) {
        return authCookieService.buildCookie(response.refreshToken(),
                appProperties.getJwt().getRefreshTokenExpirationMs());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
