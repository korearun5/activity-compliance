package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.FpoSoilProfileRequest;
import com.activityplatform.backend.fpo.api.FpoSoilProfileResponse;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoSoilProfileEntity;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.FpoSoilProfileRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FpoSoilProfileService {
  private final AuditEventService auditEventService;
  private final FpoMemberProfileRepository memberRepository;
  private final FpoSoilProfileRepository soilProfileRepository;
  private final TenantModuleService tenantModuleService;
  private final UserRepository userRepository;

  public FpoSoilProfileService(
      AuditEventService auditEventService,
      FpoMemberProfileRepository memberRepository,
      FpoSoilProfileRepository soilProfileRepository,
      TenantModuleService tenantModuleService,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.memberRepository = memberRepository;
    this.soilProfileRepository = soilProfileRepository;
    this.tenantModuleService = tenantModuleService;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<FpoSoilProfileResponse> list(CurrentUser currentUser, UUID memberId) {
    requireMemberDataModule(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    requireManagerOrOwner(currentUser, member);
    return soilProfileRepository
        .findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(currentUser.tenantId(), memberId)
        .stream()
        .map(FpoSoilProfileResponse::from)
        .toList();
  }

  @Transactional
  public FpoSoilProfileResponse create(
      CurrentUser currentUser,
      UUID memberId,
      FpoSoilProfileRequest request
  ) {
    requireMemberDataModule(currentUser);
    requireManager(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    Instant now = Instant.now();
    FpoSoilProfileEntity profile = new FpoSoilProfileEntity(
        UUID.randomUUID(),
        member.getTenant(),
        member,
        request.soilOrganicCarbon(),
        request.ph(),
        request.nitrogen(),
        request.phosphorus(),
        request.potassium(),
        normalizeOptional(request.reportFileName()),
        normalizeOptional(request.reportContentType()),
        normalizeReportUrl(request.reportUrl()),
        normalizeOptional(request.notes()),
        now
    );
    FpoSoilProfileEntity saved = soilProfileRepository.save(profile);
    audit(currentUser, saved, AuditAction.FPO_SOIL_PROFILE_CREATED);
    return FpoSoilProfileResponse.from(saved);
  }

  @Transactional
  public FpoSoilProfileResponse update(
      CurrentUser currentUser,
      UUID soilProfileId,
      FpoSoilProfileRequest request
  ) {
    requireMemberDataModule(currentUser);
    requireManager(currentUser);
    FpoSoilProfileEntity profile = requireSoilProfile(currentUser, soilProfileId);
    profile.updateDetails(
        request.soilOrganicCarbon(),
        request.ph(),
        request.nitrogen(),
        request.phosphorus(),
        request.potassium(),
        normalizeOptional(request.reportFileName()),
        normalizeOptional(request.reportContentType()),
        normalizeReportUrl(request.reportUrl()),
        normalizeOptional(request.notes()),
        Instant.now()
    );
    FpoSoilProfileEntity saved = soilProfileRepository.save(profile);
    audit(currentUser, saved, AuditAction.FPO_SOIL_PROFILE_UPDATED);
    return FpoSoilProfileResponse.from(saved);
  }

  private void requireMemberDataModule(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
  }

  private FpoMemberProfileEntity requireMember(CurrentUser currentUser, UUID memberId) {
    return memberRepository.findByIdAndTenantId(memberId, currentUser.tenantId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
  }

  private FpoSoilProfileEntity requireSoilProfile(CurrentUser currentUser, UUID soilProfileId) {
    return soilProfileRepository.findByIdAndTenantId(soilProfileId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Soil profile not found."));
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only Phase 1 staff can manage soil profiles.",
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
        "You do not have permission to view this member's soil profiles.",
        HttpStatus.FORBIDDEN
    );
  }

  private void audit(
      CurrentUser currentUser,
      FpoSoilProfileEntity profile,
      AuditAction action
  ) {
    auditEventService.record(
        profile.getTenant(),
        actor(currentUser),
        "FPO_SOIL_PROFILE",
        profile.getId(),
        action,
        Map.of("memberId", profile.getMemberProfile().getId().toString())
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private String normalizeReportUrl(String value) {
    String normalized = normalizeOptional(value);
    if (normalized == null) {
      return null;
    }

    try {
      URI uri = new URI(normalized);
      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        throw validation("Soil report URL must start with http or https.");
      }
    } catch (URISyntaxException exception) {
      throw validation("Soil report URL must be valid.");
    }

    return normalized;
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
