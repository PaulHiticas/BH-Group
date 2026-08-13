package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.dto.user.UserStatusUpdateRequest;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.RefreshTokenRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.repository.VerificationTokenRepository;
import com.bhgroup.pms.security.SecureTokenGenerator;
import com.bhgroup.pms.service.mapper.UserMapperImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecureTokenGenerator secureTokenGenerator;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditService auditService;

    private UserAdminService userAdminService;
    private User targetUser;

    @BeforeEach
    void setUp() {
        userAdminService = new UserAdminService(
                userRepository, propertyRepository, refreshTokenRepository, verificationTokenRepository,
                passwordEncoder, secureTokenGenerator, emailService, auditService, new UserMapperImpl(), null);

        targetUser = User.builder()
                .email("target@bhgroup.io")
                .firstName("Target")
                .lastName("User")
                .role(Role.ADMINISTRATOR)
                .status(UserStatus.ACTIVE)
                .build();
        targetUser.setId(UUID.randomUUID());
    }

    @Test
    void updateStatus_rejectsSelfDisable() {
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.DISABLED, "target@bhgroup.io");

        assertThatThrownBy(() -> userAdminService.updateStatus(
                targetUser.getId(), request, targetUser.getId(), "SUPER_ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own account");
    }

    @Test
    void updateStatus_rejectsDisableWithoutMatchingEmailConfirmation() {
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        UUID actingUserId = UUID.randomUUID();

        UserStatusUpdateRequest wrongEmail = new UserStatusUpdateRequest(UserStatus.DISABLED, "wrong@bhgroup.io");

        assertThatThrownBy(() -> userAdminService.updateStatus(
                targetUser.getId(), wrongEmail, actingUserId, "SUPER_ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Type the account's email");
    }

    @Test
    void updateStatus_rejectsDisablingTheLastActiveSuperAdmin() {
        targetUser.setRole(Role.SUPER_ADMIN);
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(userRepository.countByRoleAndStatusAndIdNot(Role.SUPER_ADMIN, UserStatus.ACTIVE, targetUser.getId()))
                .thenReturn(0L);
        UUID actingUserId = UUID.randomUUID();

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.DISABLED, "target@bhgroup.io");

        assertThatThrownBy(() -> userAdminService.updateStatus(
                targetUser.getId(), request, actingUserId, "SUPER_ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last active Super Admin");
    }

    @Test
    void updateStatus_allowsDisablingASuperAdminWhenAnotherOneIsActive() {
        targetUser.setRole(Role.SUPER_ADMIN);
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(userRepository.countByRoleAndStatusAndIdNot(Role.SUPER_ADMIN, UserStatus.ACTIVE, targetUser.getId()))
                .thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID actingUserId = UUID.randomUUID();

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.DISABLED, "target@bhgroup.io");

        var response = userAdminService.updateStatus(targetUser.getId(), request, actingUserId, "SUPER_ADMIN");

        assertThat(response.status()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void updateStatus_rejectsDisablingAnOwnerWithProperties() {
        targetUser.setRole(Role.OWNER);
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        Property property = Property.builder().name("Some property").build();
        when(propertyRepository.findByOwnerId(targetUser.getId())).thenReturn(List.of(property));
        UUID actingUserId = UUID.randomUUID();

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.DISABLED, "target@bhgroup.io");

        assertThatThrownBy(() -> userAdminService.updateStatus(
                targetUser.getId(), request, actingUserId, "SUPER_ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reassign this owner's properties");
    }

    @Test
    void updateStatus_suspendingDoesNotRequireEmailConfirmationOrGuards() {
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID actingUserId = UUID.randomUUID();

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.SUSPENDED, null);

        var response = userAdminService.updateStatus(targetUser.getId(), request, actingUserId, "SUPER_ADMIN");

        assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
    }
}
