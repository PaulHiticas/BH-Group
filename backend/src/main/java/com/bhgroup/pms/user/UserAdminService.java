package com.bhgroup.pms.user;

import com.bhgroup.pms.audit.AuditAction;
import com.bhgroup.pms.audit.AuditService;
import com.bhgroup.pms.auth.UserMapper;
import com.bhgroup.pms.auth.dto.UserResponse;
import com.bhgroup.pms.common.exception.ConflictException;
import com.bhgroup.pms.common.exception.ForbiddenException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.token.RefreshTokenRepository;
import com.bhgroup.pms.user.dto.UserCreateRequest;
import com.bhgroup.pms.user.dto.UserStatusUpdateRequest;
import com.bhgroup.pms.user.dto.UserUpdateRequest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Set<Role> RESTRICTED_ROLES = Set.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final UserMapper userMapper;

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
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);

        auditService.record(AuditAction.USER_REGISTERED, user, "User created by administrator", null, null);

        return userMapper.toResponse(user);
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
