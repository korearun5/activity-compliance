package com.activityplatform.backend.user.service;

import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.user.api.CreateUserRequest;
import com.activityplatform.backend.user.api.UpdateUserRequest;
import com.activityplatform.backend.user.api.UserResponse;
import com.activityplatform.backend.user.api.UserStatus;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final AuditEventService auditEventService;
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public UserService(
      AuditEventService auditEventService,
      PasswordEncoder passwordEncoder,
      RoleRepository roleRepository,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.passwordEncoder = passwordEncoder;
    this.roleRepository = roleRepository;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> list(CurrentUser currentUser, Pageable pageable) {
    requireManager(currentUser);
    return userRepository.findByTenantId(currentUser.tenantId(), pageable)
        .map(UserResponse::from);
  }

  @Transactional(readOnly = true)
  public UserResponse get(CurrentUser currentUser, UUID userId) {
    requireManager(currentUser);
    return UserResponse.from(requireUser(currentUser, userId));
  }

  @Transactional(readOnly = true)
  public UserResponse me(CurrentUser currentUser) {
    UserEntity user = userRepository.findByIdAndTenantId(
            currentUser.userId(),
            currentUser.tenantId()
        )
        .orElseThrow(() -> notFound("User not found."));

    return UserResponse.from(user);
  }

  @Transactional
  public UserResponse createFieldCoordinator(CurrentUser currentUser, CreateUserRequest request) {
    requireManager(currentUser);
    return createSingleRoleUser(currentUser, request, Role.FIELD_COORDINATOR);
  }

  @Transactional
  public UserResponse createFarmer(CurrentUser currentUser, CreateUserRequest request) {
    requirePhaseOneStaff(currentUser);
    return createSingleRoleUser(currentUser, request, Role.FARMER);
  }

  private UserResponse createSingleRoleUser(
      CurrentUser currentUser,
      CreateUserRequest request,
      Role role
  ) {
    String username = normalizeUsername(request.username());

    if (userRepository.existsByTenantIdAndUsernameIgnoreCase(currentUser.tenantId(), username)) {
      throw new ApplicationException(
          ErrorCode.DUPLICATE_RESOURCE,
          "Username already exists for this tenant.",
          HttpStatus.CONFLICT
      );
    }

    TenantEntity tenant = tenantRepository.findById(currentUser.tenantId())
        .orElseThrow(() -> notFound("Tenant not found."));
    RoleEntity roleEntity = ensureRole(tenant, role);
    Instant now = Instant.now();
    UserEntity user = new UserEntity(
        UUID.randomUUID(),
        tenant,
        username,
        passwordEncoder.encode(request.password()),
        request.displayName().trim(),
        normalizeOptional(request.phone()),
        normalizeOptional(request.locationName()),
        normalizeOptional(request.siteName()),
        "ACTIVE",
        now
    );
    user.addRole(roleEntity);
    UserEntity savedUser = saveUser(user);

    auditEventService.record(
        tenant,
        actor(currentUser),
        "USER",
        savedUser.getId(),
        AuditAction.USER_CREATED,
        Map.of(
            "username", savedUser.getUsername(),
            "role", role.name()
        )
    );

    log.info(
        "{} user created userId={} tenantId={} actorUserId={}",
        role,
        savedUser.getId(),
        tenant.getId(),
        currentUser.userId()
    );

    return UserResponse.from(savedUser);
  }

  @Transactional
  public UserResponse update(CurrentUser currentUser, UUID userId, UpdateUserRequest request) {
    requireManager(currentUser);
    UserEntity user = requireUser(currentUser, userId);
    Instant now = Instant.now();
    user.updateProfile(
        request.displayName().trim(),
        normalizeOptional(request.phone()),
        normalizeOptional(request.locationName()),
        normalizeOptional(request.siteName()),
        now
    );
    UserEntity savedUser = userRepository.save(user);

    auditEventService.record(
        savedUser.getTenant(),
        actor(currentUser),
        "USER",
        savedUser.getId(),
        AuditAction.USER_UPDATED,
        Map.of("username", savedUser.getUsername())
    );
    log.info(
        "User profile updated userId={} tenantId={} actorUserId={}",
        savedUser.getId(),
        savedUser.getTenant().getId(),
        currentUser.userId()
    );

    return UserResponse.from(savedUser);
  }

  @Transactional
  public UserResponse updateStatus(CurrentUser currentUser, UUID userId, UserStatus status) {
    requireManager(currentUser);
    UserEntity user = requireUser(currentUser, userId);
    user.updateStatus(status.name(), Instant.now());
    UserEntity savedUser = userRepository.save(user);

    auditEventService.record(
        savedUser.getTenant(),
        actor(currentUser),
        "USER",
        savedUser.getId(),
        AuditAction.USER_STATUS_CHANGED,
        Map.of(
            "username", savedUser.getUsername(),
            "status", status.name()
        )
    );
    log.info(
        "User status changed userId={} tenantId={} status={} actorUserId={}",
        savedUser.getId(),
        savedUser.getTenant().getId(),
        status.name(),
        currentUser.userId()
    );

    return UserResponse.from(savedUser);
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins and FPO managers can manage users.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private void requirePhaseOneStaff(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only Phase 1 staff can create farmer users.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private UserEntity requireUser(CurrentUser currentUser, UUID userId) {
    return userRepository.findByIdAndTenantId(userId, currentUser.tenantId())
        .orElseThrow(() -> notFound("User not found."));
  }

  private UserEntity saveUser(UserEntity user) {
    try {
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw new ApplicationException(
          ErrorCode.DUPLICATE_RESOURCE,
          "Username already exists for this tenant.",
          HttpStatus.CONFLICT
      );
    }
  }

  private RoleEntity ensureRole(TenantEntity tenant, Role role) {
    return roleRepository.findByTenantCodeIgnoreCaseAndCodeIgnoreCase(tenant.getCode(), role.name())
        .orElseGet(() -> roleRepository.save(new RoleEntity(
            UUID.randomUUID(),
            tenant,
            role.name(),
            role.name(),
            Instant.now()
        )));
  }

  private String normalizeUsername(String username) {
    return username.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
