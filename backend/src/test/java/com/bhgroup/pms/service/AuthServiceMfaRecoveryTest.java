package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.UnauthorizedException;
import com.bhgroup.pms.domain.MfaRecoveryCode;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.dto.auth.MfaEnableRequest;
import com.bhgroup.pms.dto.auth.MfaRecoveryLoginRequest;
import com.bhgroup.pms.repository.MfaRecoveryCodeRepository;
import com.bhgroup.pms.repository.RefreshTokenRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.repository.VerificationTokenRepository;
import com.bhgroup.pms.security.JwtService;
import com.bhgroup.pms.security.SecureTokenGenerator;
import com.bhgroup.pms.service.mapper.UserMapperImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceMfaRecoveryTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private TotpService totpService;
    @Mock
    private MfaRecoveryCodeRepository mfaRecoveryCodeRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditService auditService;
    @Mock
    private com.bhgroup.pms.config.AppProperties appProperties;

    private final SecureTokenGenerator secureTokenGenerator = new SecureTokenGenerator();
    private final List<MfaRecoveryCode> savedCodes = new ArrayList<>();

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, refreshTokenRepository, verificationTokenRepository, passwordEncoder,
                authenticationManager, jwtService, secureTokenGenerator, totpService, mfaRecoveryCodeRepository,
                emailService, auditService, new UserMapperImpl(), appProperties);

        user = User.builder()
                .email("staff@bhgroup.io")
                .firstName("Staff")
                .lastName("Member")
                .role(Role.ADMINISTRATOR)
                .status(UserStatus.ACTIVE)
                .mfaSecret("SECRET")
                .mfaEnabled(false)
                .build();
        user.setId(UUID.randomUUID());

        // In-memory fake persistence: save() appends/replaces, findByUserIdAndUsedAtIsNull() filters.
        // Not every test exercises both, hence lenient() to avoid strict-stubbing failures.
        lenient().when(mfaRecoveryCodeRepository.save(any(MfaRecoveryCode.class))).thenAnswer(invocation -> {
            MfaRecoveryCode code = invocation.getArgument(0);
            if (code.getId() == null) code.setId(UUID.randomUUID());
            savedCodes.removeIf(c -> c.getId().equals(code.getId()));
            savedCodes.add(code);
            return code;
        });
        lenient().when(mfaRecoveryCodeRepository.findByUserIdAndUsedAtIsNull(any(UUID.class))).thenAnswer(invocation -> {
            UUID userId = invocation.getArgument(0);
            return savedCodes.stream()
                    .filter(c -> c.getUser().getId().equals(userId) && c.getUsedAt() == null)
                    .toList();
        });
    }

    @Test
    void enableMfa_generatesEightUniqueRecoveryCodesAndOnlyStoresHashes() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.verifyCode("SECRET", "123456")).thenReturn(true);

        var response = authService.enableMfa(user.getId(), new MfaEnableRequest("123456"));

        assertThat(response.recoveryCodes()).hasSize(8);
        assertThat(response.recoveryCodes()).doesNotHaveDuplicates();
        assertThat(savedCodes).hasSize(8);
        savedCodes.forEach(code -> assertThat(response.recoveryCodes()).doesNotContain(code.getCodeHash()));
    }

    @Test
    void verifyMfaRecoveryLogin_rejectsAnUnknownCode() {
        when(jwtService.isTokenValidOfType(eq("challenge-token"), any())).thenReturn(true);
        when(jwtService.getUserId("challenge-token")).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        user.setMfaEnabled(true);

        MfaRecoveryLoginRequest request = new MfaRecoveryLoginRequest("challenge-token", "ZZZZZ-ZZZZZ");

        assertThatThrownBy(() -> authService.verifyMfaRecoveryLogin(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verifyMfaRecoveryLogin_consumesTheCodeExactlyOnce() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.verifyCode("SECRET", "123456")).thenReturn(true);
        var enableResponse = authService.enableMfa(user.getId(), new MfaEnableRequest("123456"));
        String rawCode = enableResponse.recoveryCodes().get(0);

        when(jwtService.isTokenValidOfType(eq("challenge-token"), any())).thenReturn(true);
        when(jwtService.getUserId("challenge-token")).thenReturn(user.getId());
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(1000L);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(1000L);

        MfaRecoveryLoginRequest request = new MfaRecoveryLoginRequest("challenge-token", rawCode);
        var authResponse = authService.verifyMfaRecoveryLogin(request, "127.0.0.1", "test-agent");

        assertThat(authResponse.accessToken()).isEqualTo("access-token");
        verify(refreshTokenRepository, times(1)).save(any());

        // Using the same code again must fail - it's single-use.
        assertThatThrownBy(() -> authService.verifyMfaRecoveryLogin(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
