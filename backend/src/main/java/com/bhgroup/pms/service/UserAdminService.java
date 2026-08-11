package com.bhgroup.pms.service;

import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.service.mapper.UserMapper;
import com.bhgroup.pms.dto.auth.UserResponse;
import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ConflictException;
import com.bhgroup.pms.common.exception.ForbiddenException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.repository.RefreshTokenRepository;
import com.bhgroup.pms.dto.user.UserCreateRequest;
import com.bhgroup.pms.dto.user.UserStatusUpdateRequest;
import com.bhgroup.pms.dto.user.UserUpdateRequest;
import com.bhgroup.pms.domain.VerificationToken;
import com.bhgroup.pms.domain.VerificationTokenType;
import com.bhgroup.pms.repository.VerificationTokenRepository;
import com.bhgroup.pms.security.SecureTokenGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.repository.UserSpecifications;
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Set<Role> RESTRICTED_ROLES = Set.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR);

    private static final java.util.Map<Role, String> ROLE_LABELS = java.util.Map.of(
            Role.SUPER_ADMIN, "Super Admin",
            Role.ADMINISTRATOR, "Administrator",
            Role.OWNER, "Proprietar",
            Role.CLEANER, "Cleaner",
            Role.MAINTENANCE, "Mentenanță",
            Role.ACCOUNTANT, "Contabil",
            Role.SUPPORT_AGENT, "Agent suport"
    );

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenGenerator secureTokenGenerator;
    private final EmailService emailService;
    private final AuditService auditService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, Role role, UserStatus status, Pageable pageable) {
        Specification<User> spec = UserSpecifications.combine(
                UserSpecifications.search(search),
                UserSpecifications.hasRole(role),
                UserSpecifications.hasStatus(status)
        );
        Page<User> page = userRepository.findAll(spec, pageable);
        return PageResponse.of(page, userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return userMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request, String actingRole) {
        assertCanAssignRole(request.role(), actingRole);

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(secureTokenGenerator.generateRawToken()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(request.role())
                .status(UserStatus.PENDING)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);

        auditService.record(AuditAction.USER_INVITED, user, "User invited by administrator", null, null);
        sendInvite(user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public void resendInvite(UUID id, String actingRole) {
        User user = findOrThrow(id);
        assertCanAssignRole(user.getRole(), actingRole);

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BadRequestException("Only pending invitations can be resent");
        }

        auditService.record(AuditAction.USER_INVITE_RESENT, user, "Invitation resent by administrator", null, null);
        sendInvite(user);
    }

    private void sendInvite(User user) {
        verificationTokenRepository.invalidateActiveTokens(user.getId(), VerificationTokenType.USER_INVITE);

        String rawToken = secureTokenGenerator.generateRawToken();
        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(rawToken)
                .type(VerificationTokenType.USER_INVITE)
                .expiresAt(Instant.now().plus(
                        appProperties.getSecurity().getUserInviteTokenExpirationMinutes(), ChronoUnit.MINUTES))
                .build();
        verificationTokenRepository.save(token);

        emailService.sendUserInviteEmail(user.getEmail(), user.getFirstName(), ROLE_LABELS.get(user.getRole()),
                rawToken, appProperties.getSecurity().getUserInviteTokenExpirationMinutes());
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request, String actingRole) {
        User user = findOrThrow(id);
        assertCanAssignRole(user.getRole(), actingRole);
        assertCanAssignRole(request.role(), actingRole);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRole(request.role());

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UserStatusUpdateRequest request, String actingRole) {
        User user = findOrThrow(id);
        assertCanAssignRole(user.getRole(), actingRole);

        user.setStatus(request.status());
        if (request.status() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
        }
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void assertCanAssignRole(Role targetRole, String actingRole) {
        if (RESTRICTED_ROLES.contains(targetRole) && !Role.SUPER_ADMIN.name().equals(actingRole)) {
            throw new ForbiddenException("Only a Super Admin can assign or modify Super Admin / Administrator accounts");
        }
    }
}
