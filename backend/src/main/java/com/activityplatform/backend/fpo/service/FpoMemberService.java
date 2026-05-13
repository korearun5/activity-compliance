package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.CreateFpoMemberRequest;
import com.activityplatform.backend.fpo.api.FpoMemberResponse;
import com.activityplatform.backend.fpo.api.UpdateFpoMemberRequest;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.user.api.CreateUserRequest;
import com.activityplatform.backend.user.api.UserResponse;
import com.activityplatform.backend.user.service.UserService;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FpoMemberService {
  private final AuditEventService auditEventService;
  private final FpoMemberProfileRepository memberRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final UserService userService;

  public FpoMemberService(
      AuditEventService auditEventService,
      FpoMemberProfileRepository memberRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository,
      UserService userService
  ) {
    this.auditEventService = auditEventService;
    this.memberRepository = memberRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.userService = userService;
  }

  @Transactional(readOnly = true)
  public Page<FpoMemberResponse> list(
      CurrentUser currentUser,
      FpoMemberStatus status,
      Pageable pageable
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
    requireManager(currentUser);

    if (status == null) {
      return memberRepository.findByTenantId(currentUser.tenantId(), pageable)
          .map(FpoMemberResponse::from);
    }

    return memberRepository.findByTenantIdAndStatus(currentUser.tenantId(), status, pageable)
        .map(FpoMemberResponse::from);
  }

  @Transactional(readOnly = true)
  public FpoMemberResponse get(CurrentUser currentUser, UUID memberId) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    requireManagerOrOwner(currentUser, member);
    return FpoMemberResponse.from(member);
  }

  @Transactional(readOnly = true)
  public FpoMemberResponse me(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
    FpoMemberProfileEntity member = memberRepository
        .findByTenantIdAndUserId(currentUser.tenantId(), currentUser.userId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
    return FpoMemberResponse.from(member);
  }

  @Transactional
  public FpoMemberResponse create(CurrentUser currentUser, CreateFpoMemberRequest request) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
    requireManager(currentUser);
    validateCreateUserChoice(request);

    String memberNumber = normalizeRequired(request.memberNumber());
    String mobileNumber = FpoMemberProfileRules.normalizeIndianMobile(request.mobileNumber());
    ensureMemberNumberAvailable(currentUser.tenantId(), memberNumber, null);
    ensureMobileAvailable(currentUser.tenantId(), mobileNumber, null);

    TenantEntity tenant = requireTenant(currentUser.tenantId());
    UserEntity user = resolveMemberUser(currentUser, request);
    if (memberRepository.existsByTenantIdAndUserId(currentUser.tenantId(), user.getId())) {
      throw conflict("This user already has an FPO member profile.");
    }

    UserEntity coordinator = resolveCoordinator(currentUser, request.coordinatorUserId());
    Instant now = Instant.now();
    FpoMemberProfileEntity member = new FpoMemberProfileEntity(
        UUID.randomUUID(),
        tenant,
        user,
        memberNumber,
        request.displayName().trim(),
        mobileNumber,
        FpoMemberProfileRules.normalizeOptionalIndianMobile(request.alternateMobileNumber()),
        FpoMemberProfileRules.normalizeOptionalAadhaar(request.aadhaarNumber()),
        request.village().trim(),
        request.taluka().trim(),
        request.districtName().trim(),
        request.stateName().trim(),
        FpoMemberProfileRules.normalizeGender(request.gender()),
        request.dateOfBirth(),
        request.age(),
        FpoMemberProfileRules.normalizeFarmerCategory(request.farmerCategory()),
        coordinator,
        request.status() == null ? FpoMemberStatus.ACTIVE : request.status(),
        now
    );

    FpoMemberProfileEntity savedMember = saveMember(member);
    auditMember(currentUser, savedMember, AuditAction.FPO_MEMBER_CREATED);
    return FpoMemberResponse.from(savedMember);
  }

  @Transactional
  public FpoMemberResponse update(
      CurrentUser currentUser,
      UUID memberId,
      UpdateFpoMemberRequest request
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
    requireManager(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    String memberNumber = normalizeRequired(request.memberNumber());
    String mobileNumber = FpoMemberProfileRules.normalizeIndianMobile(request.mobileNumber());
    ensureMemberNumberAvailable(currentUser.tenantId(), memberNumber, member.getId());
    ensureMobileAvailable(currentUser.tenantId(), mobileNumber, member.getId());

    member.updateDetails(
        memberNumber,
        request.displayName().trim(),
        mobileNumber,
        FpoMemberProfileRules.normalizeOptionalIndianMobile(request.alternateMobileNumber()),
        FpoMemberProfileRules.normalizeOptionalAadhaar(request.aadhaarNumber()),
        request.village().trim(),
        request.taluka().trim(),
        request.districtName().trim(),
        request.stateName().trim(),
        FpoMemberProfileRules.normalizeGender(request.gender()),
        request.dateOfBirth(),
        request.age(),
        FpoMemberProfileRules.normalizeFarmerCategory(request.farmerCategory()),
        resolveCoordinator(currentUser, request.coordinatorUserId()),
        request.status(),
        Instant.now()
    );

    FpoMemberProfileEntity savedMember = saveMember(member);
    auditMember(currentUser, savedMember, AuditAction.FPO_MEMBER_UPDATED);
    return FpoMemberResponse.from(savedMember);
  }

  @Transactional
  public FpoMemberResponse updateStatus(
      CurrentUser currentUser,
      UUID memberId,
      FpoMemberStatus status
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
    requireManager(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    member.updateStatus(status, Instant.now());
    FpoMemberProfileEntity savedMember = memberRepository.save(member);
    auditMember(currentUser, savedMember, AuditAction.FPO_MEMBER_STATUS_CHANGED);
    return FpoMemberResponse.from(savedMember);
  }

  private void validateCreateUserChoice(CreateFpoMemberRequest request) {
    if (request.userId() != null) {
      return;
    }

    if (!hasText(request.username()) || !hasText(request.password())) {
      throw validation("Provide either an existing userId or a username and password.");
    }
  }

  private UserEntity resolveMemberUser(
      CurrentUser currentUser,
      CreateFpoMemberRequest request
  ) {
    if (request.userId() != null) {
      UserEntity user = requireUser(currentUser, request.userId());
      requireFieldCoordinatorOnly(user);
      return user;
    }

    UserResponse user = userService.createFieldCoordinator(
        currentUser,
        new CreateUserRequest(
            request.username(),
            request.password(),
            request.displayName(),
            FpoMemberProfileRules.normalizeIndianMobile(request.mobileNumber()),
            request.village(),
            request.taluka()
        )
    );

    return requireUser(currentUser, user.id());
  }

  private void ensureMemberNumberAvailable(UUID tenantId, String memberNumber, UUID currentId) {
    memberRepository.findByTenantIdAndMemberNumberIgnoreCase(tenantId, memberNumber)
        .filter(member -> !member.getId().equals(currentId))
        .ifPresent(member -> {
          throw conflict("Member number already exists for this tenant.");
        });
  }

  private void ensureMobileAvailable(UUID tenantId, String mobileNumber, UUID currentId) {
    memberRepository.findByTenantIdAndMobileNumber(tenantId, mobileNumber)
        .filter(member -> !member.getId().equals(currentId))
        .ifPresent(member -> {
          throw conflict("Mobile number already exists for this tenant.");
        });
  }

  private UserEntity resolveCoordinator(CurrentUser currentUser, UUID coordinatorUserId) {
    if (coordinatorUserId == null) {
      return null;
    }

    UserEntity coordinator = requireUser(currentUser, coordinatorUserId);
    if (!hasAnyRole(coordinator, Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      throw validation("Coordinator must be an admin, FPO manager, or field coordinator user.");
    }
    return coordinator;
  }

  private void requireFieldCoordinatorOnly(UserEntity user) {
    Set<Role> roles = user.getRoles().stream()
        .map(RoleEntity::getCode)
        .map(Role::valueOf)
        .collect(Collectors.toUnmodifiableSet());

    if (!roles.equals(Set.of(Role.FIELD_COORDINATOR))) {
      throw validation("FPO members must be linked to field coordinator users until farmer login is added.");
    }
  }

  private boolean hasAnyRole(UserEntity user, Role... expectedRoles) {
    Set<String> roleCodes = user.getRoles().stream()
        .map(RoleEntity::getCode)
        .collect(Collectors.toUnmodifiableSet());
    for (Role role : expectedRoles) {
      if (roleCodes.contains(role.name())) {
        return true;
      }
    }
    return false;
  }

  private FpoMemberProfileEntity requireMember(CurrentUser currentUser, UUID memberId) {
    return memberRepository.findByIdAndTenantId(memberId, currentUser.tenantId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
  }

  private UserEntity requireUser(CurrentUser currentUser, UUID userId) {
    return userRepository.findByIdAndTenantId(userId, currentUser.tenantId())
        .orElseThrow(() -> notFound("User not found."));
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private FpoMemberProfileEntity saveMember(FpoMemberProfileEntity member) {
    try {
      return memberRepository.saveAndFlush(member);
    } catch (DataIntegrityViolationException exception) {
      throw conflict("Member number, mobile number, and linked user must be unique per tenant.");
    }
  }

  private void auditMember(
      CurrentUser currentUser,
      FpoMemberProfileEntity member,
      AuditAction action
  ) {
    auditEventService.record(
        member.getTenant(),
        actor(currentUser),
        "FPO_MEMBER",
        member.getId(),
        action,
        Map.of(
            "memberNumber", member.getMemberNumber(),
            "status", member.getStatus().name(),
            "userId", member.getUser().getId().toString()
        )
    );
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only Phase 1 staff can manage FPO members.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private void requireManagerOrOwner(CurrentUser currentUser, FpoMemberProfileEntity member) {
    if (currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      return;
    }

    if (member.getUser().getId().equals(currentUser.userId())) {
      return;
    }

    throw new ApplicationException(
        ErrorCode.ACCESS_DENIED,
        "You do not have permission to view this FPO member profile.",
        HttpStatus.FORBIDDEN
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private String normalizeRequired(String value) {
    return value.trim();
  }

  private String normalizeOptional(String value) {
    return hasText(value) ? value.trim() : null;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }

  private ApplicationException conflict(String message) {
    return new ApplicationException(ErrorCode.DUPLICATE_RESOURCE, message, HttpStatus.CONFLICT);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
