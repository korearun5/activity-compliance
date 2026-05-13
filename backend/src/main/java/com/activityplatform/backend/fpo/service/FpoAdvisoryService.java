package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.FpoAdvisoryRequest;
import com.activityplatform.backend.fpo.api.FpoAdvisoryResponse;
import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import com.activityplatform.backend.fpo.domain.AdvisoryTargetType;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoAdvisoryEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FpoAdvisoryRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.notification.domain.NotificationChannel;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FpoAdvisoryService {
  private final AuditEventService auditEventService;
  private final CropCatalogRepository cropRepository;
  private final CropSeasonRepository seasonRepository;
  private final FpoAdvisoryRepository advisoryRepository;
  private final FpoMemberProfileRepository memberRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public FpoAdvisoryService(
      AuditEventService auditEventService,
      CropCatalogRepository cropRepository,
      CropSeasonRepository seasonRepository,
      FpoAdvisoryRepository advisoryRepository,
      FpoMemberProfileRepository memberRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.cropRepository = cropRepository;
    this.seasonRepository = seasonRepository;
    this.advisoryRepository = advisoryRepository;
    this.memberRepository = memberRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<FpoAdvisoryResponse> list(
      CurrentUser currentUser,
      AdvisoryStatus status,
      UUID cropId,
      UUID seasonId,
      String targetVillage
  ) {
    requireAdvisoryModule(currentUser);
    FpoMemberProfileEntity ownMember = currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)
        ? null
        : ownMember(currentUser);

    return advisoryRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId()).stream()
        .filter(advisory -> status == null || advisory.getStatus() == status)
        .filter(advisory -> cropId == null
            || (advisory.getCrop() != null && advisory.getCrop().getId().equals(cropId)))
        .filter(advisory -> seasonId == null
            || (advisory.getSeason() != null && advisory.getSeason().getId().equals(seasonId)))
        .filter(advisory -> !hasText(targetVillage)
            || equalsIgnoreCase(advisory.getTargetVillage(), targetVillage.trim()))
        .filter(advisory -> ownMember == null || isVisibleToMember(advisory, ownMember))
        .map(FpoAdvisoryResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public FpoAdvisoryResponse get(CurrentUser currentUser, UUID advisoryId) {
    requireAdvisoryModule(currentUser);
    FpoAdvisoryEntity advisory = requireAdvisory(currentUser, advisoryId);
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)
        && !isVisibleToMember(advisory, ownMember(currentUser))) {
      throw accessDenied("You do not have permission to view this advisory.");
    }

    return FpoAdvisoryResponse.from(advisory);
  }

  @Transactional
  public FpoAdvisoryResponse create(CurrentUser currentUser, FpoAdvisoryRequest request) {
    requireAdvisoryModule(currentUser);
    requireManager(currentUser);
    AdvisoryTargetType targetType = request.targetType() == null
        ? AdvisoryTargetType.ALL_MEMBERS
        : request.targetType();
    FpoMemberProfileEntity targetMember = resolveTargetMember(currentUser, request, targetType);
    String targetVillage = resolveTargetVillage(request, targetType);
    Instant now = Instant.now();
    FpoAdvisoryEntity advisory = new FpoAdvisoryEntity(
        UUID.randomUUID(),
        requireTenant(currentUser.tenantId()),
        resolveActiveCrop(currentUser, request.cropId()),
        resolveActiveSeason(currentUser, request.seasonId()),
        targetType,
        targetVillage,
        targetMember,
        request.title().trim(),
        request.message().trim(),
        request.channel() == null ? NotificationChannel.IN_APP : request.channel(),
        request.status() == null ? AdvisoryStatus.DRAFT : request.status(),
        actor(currentUser),
        now
    );

    FpoAdvisoryEntity saved = advisoryRepository.save(advisory);
    auditAdvisory(currentUser, saved, AuditAction.FPO_ADVISORY_CREATED, null);
    return FpoAdvisoryResponse.from(saved);
  }

  @Transactional
  public FpoAdvisoryResponse updateStatus(
      CurrentUser currentUser,
      UUID advisoryId,
      AdvisoryStatus status
  ) {
    requireAdvisoryModule(currentUser);
    requireManager(currentUser);
    FpoAdvisoryEntity advisory = requireAdvisory(currentUser, advisoryId);
    AdvisoryStatus previousStatus = advisory.getStatus();
    advisory.updateStatus(status, Instant.now());
    FpoAdvisoryEntity saved = advisoryRepository.save(advisory);
    auditAdvisory(currentUser, saved, AuditAction.FPO_ADVISORY_STATUS_CHANGED, previousStatus);
    return FpoAdvisoryResponse.from(saved);
  }

  private void requireAdvisoryModule(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.ADVISORY);
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private FpoAdvisoryEntity requireAdvisory(CurrentUser currentUser, UUID advisoryId) {
    return advisoryRepository.findByIdAndTenantId(advisoryId, currentUser.tenantId())
        .orElseThrow(() -> notFound("FPO advisory not found."));
  }

  private FpoMemberProfileEntity ownMember(CurrentUser currentUser) {
    return memberRepository.findByTenantIdAndUserId(currentUser.tenantId(), currentUser.userId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
  }

  private FpoMemberProfileEntity resolveTargetMember(
      CurrentUser currentUser,
      FpoAdvisoryRequest request,
      AdvisoryTargetType targetType
  ) {
    if (targetType != AdvisoryTargetType.MEMBER) {
      if (request.targetMemberId() != null) {
        throw validation("Target member can only be set for MEMBER advisories.");
      }
      return null;
    }

    if (request.targetMemberId() == null) {
      throw validation("Target member is required for MEMBER advisories.");
    }

    FpoMemberProfileEntity member = memberRepository
        .findByIdAndTenantId(request.targetMemberId(), currentUser.tenantId())
        .orElseThrow(() -> notFound("Target FPO member profile not found."));
    if (member.getStatus() != FpoMemberStatus.ACTIVE) {
      throw validation("Target member must be active.");
    }
    return member;
  }

  private String resolveTargetVillage(
      FpoAdvisoryRequest request,
      AdvisoryTargetType targetType
  ) {
    if (targetType != AdvisoryTargetType.VILLAGE) {
      if (hasText(request.targetVillage())) {
        throw validation("Target village can only be set for VILLAGE advisories.");
      }
      return null;
    }

    if (!hasText(request.targetVillage())) {
      throw validation("Target village is required for VILLAGE advisories.");
    }
    return request.targetVillage().trim();
  }

  private CropCatalogEntity resolveActiveCrop(CurrentUser currentUser, UUID cropId) {
    if (cropId == null) {
      return null;
    }

    CropCatalogEntity crop = cropRepository.findByIdAndTenantId(cropId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop not found."));
    if (crop.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Advisory crop must be active.");
    }
    return crop;
  }

  private CropSeasonEntity resolveActiveSeason(CurrentUser currentUser, UUID seasonId) {
    if (seasonId == null) {
      return null;
    }

    CropSeasonEntity season = seasonRepository.findByIdAndTenantId(
        seasonId,
        currentUser.tenantId()
    ).orElseThrow(() -> notFound("Crop season not found."));
    if (season.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Advisory season must be active.");
    }
    return season;
  }

  private boolean isVisibleToMember(
      FpoAdvisoryEntity advisory,
      FpoMemberProfileEntity member
  ) {
    if (advisory.getStatus() != AdvisoryStatus.PUBLISHED) {
      return false;
    }

    return switch (advisory.getTargetType()) {
      case ALL_MEMBERS -> true;
      case VILLAGE -> equalsIgnoreCase(advisory.getTargetVillage(), member.getVillage());
      case MEMBER -> advisory.getTargetMember() != null
          && advisory.getTargetMember().getId().equals(member.getId());
    };
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)) {
      throw accessDenied("Only admins and supervisors can manage advisories.");
    }
  }

  private void auditAdvisory(
      CurrentUser currentUser,
      FpoAdvisoryEntity advisory,
      AuditAction action,
      AdvisoryStatus previousStatus
  ) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("status", advisory.getStatus().name());
    metadata.put("targetType", advisory.getTargetType().name());
    metadata.put("channel", advisory.getChannel().name());
    if (previousStatus != null) {
      metadata.put("previousStatus", previousStatus.name());
    }
    if (advisory.getCrop() != null) {
      metadata.put("cropId", advisory.getCrop().getId().toString());
    }
    if (advisory.getSeason() != null) {
      metadata.put("seasonId", advisory.getSeason().getId().toString());
    }
    if (advisory.getTargetMember() != null) {
      metadata.put("targetMemberId", advisory.getTargetMember().getId().toString());
    }
    if (hasText(advisory.getTargetVillage())) {
      metadata.put("targetVillage", advisory.getTargetVillage());
    }

    auditEventService.record(
        advisory.getTenant(),
        actor(currentUser),
        "FPO_ADVISORY",
        advisory.getId(),
        action,
        metadata
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private boolean equalsIgnoreCase(String left, String right) {
    return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }

  private ApplicationException accessDenied(String message) {
    return new ApplicationException(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
