package com.bhgroup.pms.service;

import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.dto.auth.AuthResponse;
import com.bhgroup.pms.dto.auth.ForgotPasswordRequest;
import com.bhgroup.pms.dto.auth.LoginRequest;
import com.bhgroup.pms.dto.auth.MfaChallengeResponse;
import com.bhgroup.pms.dto.auth.MfaDisableRequest;
import com.bhgroup.pms.dto.auth.MfaEnableRequest;
import com.bhgroup.pms.dto.auth.MfaSetupResponse;
import com.bhgroup.pms.dto.auth.MfaVerifyLoginRequest;
import com.bhgroup.pms.dto.auth.RefreshTokenRequest;
import com.bhgroup.pms.dto.auth.ResetPasswordRequest;
import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.exception.UnauthorizedException;
import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.security.JwtService;
import com.bhgroup.pms.security.SecureTokenGenerator;
import com.bhgroup.pms.domain.RefreshToken;
import com.bhgroup.pms.repository.RefreshTokenRepository;
import com.bhgroup.pms.domain.VerificationToken;
import com.bhgroup.pms.repository.VerificationTokenRepository;
import com.bhgroup.pms.domain.VerificationTokenType;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.RefreshToken;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.VerificationToken;
import com.bhgroup.pms.domain.VerificationTokenType;
import com.bhgroup.pms.dto.auth.AuthResponse;
import com.bhgroup.pms.dto.auth.ForgotPasswordRequest;
import com.bhgroup.pms.dto.auth.LoginRequest;
import com.bhgroup.pms.dto.auth.MfaChallengeResponse;
import com.bhgroup.pms.dto.auth.MfaDisableRequest;
import com.bhgroup.pms.dto.auth.MfaEnableRequest;
import com.bhgroup.pms.dto.auth.MfaSetupResponse;
import com.bhgroup.pms.dto.auth.MfaVerifyLoginRequest;
import com.bhgroup.pms.dto.auth.RefreshTokenRequest;
import com.bhgroup.pms.dto.auth.ResetPasswordRequest;
import com.bhgroup.pms.repository.RefreshTokenRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.repository.VerificationTokenRepository;
import com.bhgroup.pms.service.mapper.UserMapper;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecureTokenGenerator secureTokenGenerator;
    private final TotpService totpService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;

    @Transactional
    public Object login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new UnauthorizedException("Account is temporarily locked due to too many failed login attempts");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        } catch (BadCredentialsException ex) {
            registerFailedLoginAttempt(user, ipAddress);
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        if (user.isMfaEnabled()) {
            userRepository.save(user);
            String challengeToken = jwtService.generateMfaChallengeToken(user.getId());
            return new MfaChallengeResponse(challengeToken, 5 * 60L);
        }

        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        auditService.record(AuditAction.USER_LOGIN_SUCCESS, user, "User logged in", ipAddress, userAgent);
        return issueAuthResponse(user, ipAddress);
    }

    @Transactional
    public AuthResponse verifyMfaLogin(MfaVerifyLoginRequest request, String ipAddress, String userAgent) {
        if (!jwtService.isTokenValidOfType(request.challengeToken(), JwtService.TOKEN_TYPE_MFA_CHALLENGE)) {
            throw new UnauthorizedException("MFA challenge expired or invalid, please login again");
        }
        UUID userId = jwtService.getUserId(request.challengeToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid MFA challenge"));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null
                || !totpService.verifyCode(user.getMfaSecret(), request.code())) {
            auditService.record(AuditAction.MFA_CHALLENGE_FAILED, user, "Invalid MFA code", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid MFA code");
        }

        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        auditService.record(AuditAction.MFA_CHALLENGE_SUCCESS, user, "MFA challenge passed", ipAddress, userAgent);
        return issueAuthResponse(user, ipAddress);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress) {
        String tokenHash = secureTokenGenerator.hash(request.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (!existing.isActive()) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        User user = existing.getUser();

        String newRawToken = secureTokenGenerator.generateRawToken();
        String newTokenHash = secureTokenGenerator.hash(newRawToken);

        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(existing);

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .createdByIp(ipAddress)
                .build();
        refreshTokenRepository.save(newToken);

        auditService.record(AuditAction.TOKEN_REFRESHED, user, "Refresh token rotated", ipAddress, null);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.of(accessToken, newRawToken, jwtService.getAccessTokenExpirationMs(),
                userMapper.toResponse(user));
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = secureTokenGenerator.hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            auditService.record(AuditAction.USER_LOGOUT, token.getUser(), "User logged out", null, null);
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            verificationTokenRepository.invalidateActiveTokens(user.getId(), VerificationTokenType.PASSWORD_RESET);

            String rawToken = secureTokenGenerator.generateRawToken();
            VerificationToken token = VerificationToken.builder()
                    .user(user)
                    .token(rawToken)
                    .type(VerificationTokenType.PASSWORD_RESET)
                    .expiresAt(Instant.now().plus(
                            appProperties.getSecurity().getPasswordResetTokenExpirationMinutes(), ChronoUnit.MINUTES))
                    .build();
            verificationTokenRepository.save(token);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), rawToken,
                    appProperties.getSecurity().getPasswordResetTokenExpirationMinutes());

            auditService.record(AuditAction.PASSWORD_RESET_REQUESTED, user, "Password reset requested", null, null);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken token = verificationTokenRepository
                .findByTokenAndType(request.token(), VerificationTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (!token.isValid()) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        token.setUsedAt(Instant.now());
        verificationTokenRepository.save(token);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());

        auditService.record(AuditAction.PASSWORD_RESET_COMPLETED, user, "Password reset completed", null, null);
    }

    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userRepository.save(user);

        String otpAuthUrl = totpService.buildOtpAuthUrl(secret, user.getEmail(), appProperties.getName());
        return new MfaSetupResponse(secret, otpAuthUrl);
    }

    @Transactional
    public void enableMfa(UUID userId, MfaEnableRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getMfaSecret() == null || !totpService.verifyCode(user.getMfaSecret(), request.code())) {
            throw new BadRequestException("Invalid MFA code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
        auditService.record(AuditAction.MFA_ENABLED, user, "MFA enabled", null, null);
    }

    @Transactional
    public void disableMfa(UUID userId, MfaDisableRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        auditService.record(AuditAction.MFA_DISABLED, user, "MFA disabled", null, null);
    }

    private void registerFailedLoginAttempt(User user, String ipAddress) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= appProperties.getSecurity().getMaxLoginAttempts()) {
            user.setLockedUntil(Instant.now().plus(
                    appProperties.getSecurity().getLoginLockoutMinutes(), ChronoUnit.MINUTES));
            auditService.record(AuditAction.USER_LOCKED_OUT, user, "Account locked after failed attempts", ipAddress, null);
        } else {
            auditService.record(AuditAction.USER_LOGIN_FAILED, user, "Failed login attempt", ipAddress, null);
        }
        userRepository.save(user);
    }

    private AuthResponse issueAuthResponse(User user, String ipAddress) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = secureTokenGenerator.generateRawToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(secureTokenGenerator.hash(rawRefreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .createdByIp(ipAddress)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(accessToken, rawRefreshToken, jwtService.getAccessTokenExpirationMs(),
                userMapper.toResponse(user));
    }
}
