package com.activityplatform.backend.role.service;

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
import com.activityplatform.backend.role.api.RoleResponse;
import com.activityplatform.backend.role.api.UpdateUserRolesRequest;
import com.activityplatform.backend.role.api.UserRolesResponse;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleManagementService {
  private final AuditEventService auditEventService;
  private final RoleRepository roleRepository;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public RoleManagementService(
      AuditEventService auditEventService,
      RoleRepository roleRepository,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.roleRepository = roleRepository;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public List<RoleResponse> listRoles(CurrentUser currentUser) {
    requireManager(currentUser);
    TenantEntity tenant = requireTenant(currentUser);
    ensureRoles(tenant);

    return roleRepository.findByTenantIdOrderByCode(currentUser.tenantId()).stream()
        .sorted(Comparator.comparing(RoleEntity::getCode))
        .map(RoleResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public UserRolesResponse getUserRoles(CurrentUser currentUser, UUID userId) {
    requireManager(currentUser);
    return UserRolesResponse.from(requireUser(currentUser, userId));
  }

  @Transactional
  public UserRolesResponse updateUserRoles(
      CurrentUser currentUser,
      UUID userId,
      UpdateUserRolesRequest request
  ) {
    requireAdmin(currentUser);
    if (currentUser.userId().equals(userId)) {
      throw validation("Admins cannot change their own roles through this API.");
    }

    TenantEntity tenant = requireTenant(currentUser);
    UserEntity user = requireUser(currentUser, userId);
    EnumSet<Role> requestedRoles = normalizeRoles(request.roles());
    validateRoleCombination(requestedRoles);
    Set<String> previousRoles = roleNames(user);

    Set<RoleEntity> roleEntities = new LinkedHashSet<>();
    for (Role role : requestedRoles) {
      roleEntities.add(ensureRole(tenant, role));
    }

    user.replaceRoles(roleEntities, Instant.now());
    UserEntity savedUser = userRepository.save(user);
    Set<String> updatedRoles = roleNames(savedUser);

    auditEventService.record(
        tenant,
        actor(currentUser),
        "USER",
        savedUser.getId(),
        AuditAction.USER_ROLES_UPDATED,
        Map.of(
            "username", savedUser.getUsername(),
            "previousRoles", previousRoles,
            "roles", updatedRoles
        )
    );

    return UserRolesResponse.from(savedUser);
  }

  private EnumSet<Role> normalizeRoles(Set<Role> roles) {
    if (roles == null || roles.isEmpty()) {
      throw validation("At least one role is required.");
    }

    return EnumSet.copyOf(roles);
  }

  private void validateRoleCombination(Set<Role> roles) {
    if (roles.contains(Role.FIELD_COORDINATOR) && roles.size() > 1) {
      throw validation("Field coordinator cannot be combined with other roles.");
    }

    if (roles.contains(Role.FARMER) && roles.size() > 1) {
      throw validation("Farmer cannot be combined with staff roles.");
    }
  }

  private void ensureRoles(TenantEntity tenant) {
    for (Role role : Role.values()) {
      ensureRole(tenant, role);
    }
  }

  private RoleEntity ensureRole(TenantEntity tenant, Role role) {
    return roleRepository.findByTenantIdAndCodeIgnoreCase(tenant.getId(), role.name())
        .orElseGet(() -> roleRepository.save(new RoleEntity(
            UUID.randomUUID(),
            tenant,
            role.name(),
            role.name(),
            Instant.now()
        )));
  }

  private TenantEntity requireTenant(CurrentUser currentUser) {
    return tenantRepository.findById(currentUser.tenantId())
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private UserEntity requireUser(CurrentUser currentUser, UUID userId) {
    return userRepository.findByIdAndTenantId(userId, currentUser.tenantId())
        .orElseThrow(() -> notFound("User not found."));
  }

  private Set<String> roleNames(UserEntity user) {
    return user.getRoles().stream()
        .map(RoleEntity::getCode)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins and FPO managers can view roles.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private void requireAdmin(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins can update roles.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
