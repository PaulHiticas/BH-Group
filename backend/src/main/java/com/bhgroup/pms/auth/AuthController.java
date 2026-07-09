package com.bhgroup.pms.auth;

import com.bhgroup.pms.auth.dto.AuthResponse;
import com.bhgroup.pms.auth.dto.ForgotPasswordRequest;
import com.bhgroup.pms.auth.dto.LoginRequest;
import com.bhgroup.pms.auth.dto.MfaDisableRequest;
import com.bhgroup.pms.auth.dto.MfaEnableRequest;
import com.bhgroup.pms.auth.dto.MfaSetupResponse;
import com.bhgroup.pms.auth.dto.MfaVerifyLoginRequest;
import com.bhgroup.pms.auth.dto.RefreshTokenRequest;
import com.bhgroup.pms.auth.dto.ResetPasswordRequest;
import com.bhgroup.pms.auth.dto.UserResponse;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, tokens, verification, password reset and MFA")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password. Returns tokens, or an MFA challenge if 2FA is enabled")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest request,
                                                       HttpServletRequest servletRequest) {
        Object result = authService.login(request, clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/mfa/verify-login")
    @Operation(summary = "Complete login by verifying a 6-digit TOTP code")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfaLogin(@Valid @RequestBody MfaVerifyLoginRequest request,
                                                                     HttpServletRequest servletRequest) {
        AuthResponse response = authService.verifyMfaLogin(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new access/refresh token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                              HttpServletRequest servletRequest) {
        AuthResponse response = authService.refreshToken(request, clientIp(servletRequest));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token, ending the session")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
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

    @PostMapping("/mfa/setup")
    @Operation(summary = "Generate a new TOTP secret for the authenticated user")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa() {
        MfaSetupResponse response = authService.setupMfa(SecurityUtils.requireCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/enable")
    @Operation(summary = "Confirm and enable MFA by verifying a TOTP code")
    public ResponseEntity<ApiResponse<Void>> enableMfa(@Valid @RequestBody MfaEnableRequest request) {
        authService.enableMfa(SecurityUtils.requireCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.message("Two-factor authentication enabled"));
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

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
