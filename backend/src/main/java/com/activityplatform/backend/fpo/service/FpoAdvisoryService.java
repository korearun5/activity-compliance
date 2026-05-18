package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.FpoAdvisoryImageRequest;
import com.activityplatform.backend.fpo.api.FpoAdvisoryRequest;
import com.activityplatform.backend.fpo.api.FpoAdvisoryResponse;
import com.activityplatform.backend.fpo.domain.AdvisoryCategory;
import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import com.activityplatform.backend.fpo.domain.AdvisoryTargetType;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoAdvisoryEntity;
import com.activityplatform.backend.fpo.domain.FpoAdvisoryImageEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FpoAdvisoryRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.notification.domain.NotificationChannel;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
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
  private final SeasonalCropPlanRepository cropPlanRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public FpoAdvisoryService(
      AuditEventService auditEventService,
      CropCatalogRepository cropRepository,
      CropSeasonRepository seasonRepository,
      FpoAdvisoryRepository advisoryRepository,
      FpoMemberProfileRepository memberRepository,
      SeasonalCropPlanRepository cropPlanRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.cropRepository = cropRepository;
    this.seasonRepository = seasonRepository;
    this.advisoryRepository = advisoryRepository;
    this.memberRepository = memberRepository;
    this.cropPlanRepository = cropPlanRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<FpoAdvisoryResponse> list(
      CurrentUser currentUser,
      AdvisoryStatus status,
      AdvisoryCategory category,
      AdvisoryTargetType targetType,
      UUID cropId,
      UUID seasonId
  ) {
    requireAdvisoryModule(currentUser);
    boolean canManage = canManageAdvisories(currentUser);
    FpoMemberProfileEntity ownMember = currentUser.hasAnyRole(Role.FARMER)
        ? ownMember(currentUser)
        : null;

    return advisoryRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId()).stream()
        .filter(advisory -> canManage || advisory.getStatus() == AdvisoryStatus.PUBLISHED)
        .filter(advisory -> status == null || advisory.getStatus() == status)
        .filter(advisory -> category == null || advisory.getCategory() == category)
        .filter(advisory -> targetType == null || advisory.getTargetType() == targetType)
        .filter(advisory -> cropId == null
            || (advisory.getCrop() != null && advisory.getCrop().getId().equals(cropId)))
        .filter(advisory -> seasonId == null
            || (advisory.getSeason() != null && advisory.getSeason().getId().equals(seasonId)))
        .filter(advisory -> ownMember == null || isVisibleToMember(advisory, ownMember))
        .map(FpoAdvisoryResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public FpoAdvisoryResponse get(CurrentUser currentUser, UUID advisoryId) {
    requireAdvisoryModule(currentUser);
    FpoAdvisoryEntity advisory = requireAdvisory(currentUser, advisoryId);
    if (!canManageAdvisories(currentUser) && advisory.getStatus() != AdvisoryStatus.PUBLISHED) {
      throw accessDenied("You do not have permission to view this advisory.");
    }
    if (currentUser.hasAnyRole(Role.FARMER)
        && !isVisibleToMember(advisory, ownMember(currentUser))) {
      throw accessDenied("You do not have permission to view this advisory.");
    }

    return FpoAdvisoryResponse.from(advisory);
  }

  @Transactional
  public FpoAdvisoryResponse create(CurrentUser currentUser, FpoAdvisoryRequest request) {
    requireAdvisoryModule(currentUser);
    requireManager(currentUser);
    TenantEntity tenant = requireTenant(currentUser.tenantId());
    AdvisoryTargetType targetType = request.targetType() == null
        ? AdvisoryTargetType.ALL_MEMBERS
        : request.targetType();
    AdvisoryCategory category = requireCategory(request.category());
    CropCatalogEntity crop = resolveTargetCrop(currentUser, request.cropId(), targetType);
    CropSeasonEntity season = resolveActiveSeason(currentUser, request.seasonId());
    NotificationChannel channel = resolvePhase1Channel(request.channel());
    Instant now = Instant.now();
    FpoAdvisoryEntity advisory = new FpoAdvisoryEntity(
        UUID.randomUUID(),
        tenant,
        crop,
        season,
        targetType,
        category,
        request.title().trim(),
        request.message().trim(),
        channel,
        request.status() == null ? AdvisoryStatus.DRAFT : request.status(),
        actor(currentUser),
        now
    );
    buildImages(tenant, request.images(), now).forEach(advisory::addImage);

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
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.MEMBER_DATA);
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

  private CropCatalogEntity resolveTargetCrop(
      CurrentUser currentUser,
      UUID cropId,
      AdvisoryTargetType targetType
  ) {
    if (targetType == AdvisoryTargetType.ALL_MEMBERS) {
      if (cropId != null) {
        throw validation("Crop can only be set for CROP targeted advisories.");
      }
      return null;
    }

    if (cropId == null) {
      throw validation("Crop is required for CROP targeted advisories.");
    }
    CropCatalogEntity crop = cropRepository.findByIdAndTenantId(cropId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop not found."));
    if (crop.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Advisory crop must be active.");
    }
    return crop;
  }

  private NotificationChannel resolvePhase1Channel(NotificationChannel channel) {
    NotificationChannel resolved = channel == null ? NotificationChannel.IN_APP : channel;
    if (resolved != NotificationChannel.IN_APP) {
      throw validation("Phase 1 advisories support IN_APP channel only.");
    }
    return resolved;
  }

  private List<FpoAdvisoryImageEntity> buildImages(
      TenantEntity tenant,
      List<FpoAdvisoryImageRequest> images,
      Instant now
  ) {
    if (images == null || images.isEmpty()) {
      return List.of();
    }
    if (images.size() > 10) {
      throw validation("A maximum of 10 advisory images can be attached.");
    }

    return IntStream.range(0, images.size())
        .mapToObj(index -> buildImage(tenant, images.get(index), index, now))
        .toList();
  }

  private FpoAdvisoryImageEntity buildImage(
      TenantEntity tenant,
      FpoAdvisoryImageRequest image,
      int sortOrder,
      Instant now
  ) {
    return new FpoAdvisoryImageEntity(
        UUID.randomUUID(),
        tenant,
        normalizeImageUrl(image.imageUrl()),
        normalizeOptional(image.storageKey()),
        normalizeOptional(image.originalFilename()),
        normalizeImageContentType(image.contentType()),
        sortOrder,
        now
    );
  }

  private AdvisoryCategory requireCategory(AdvisoryCategory category) {
    if (category == null) {
      throw validation("Advisory category is required.");
    }
    return category;
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
      case CROP -> advisory.getCrop() != null
          && cropPlanRepository.existsByTenantIdAndMemberProfileIdAndCropIdAndStatus(
              advisory.getTenant().getId(),
              member.getId(),
              advisory.getCrop().getId(),
              CropPlanStatus.CONFIRMED
          );
    };
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      throw accessDenied("Only admins and FPO managers can manage advisories.");
    }
  }

  private void auditAdvisory(
      CurrentUser currentUser,
      FpoAdvisoryEntity advisory,
      AuditAction action,
      AdvisoryStatus previousStatus
  ) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("category", advisory.getCategory().name());
    metadata.put("status", advisory.getStatus().name());
    metadata.put("targetType", advisory.getTargetType().name());
    metadata.put("channel", advisory.getChannel().name());
    metadata.put("imageCount", advisory.getImages().size());
    if (previousStatus != null) {
      metadata.put("previousStatus", previousStatus.name());
    }
    if (advisory.getCrop() != null) {
      metadata.put("cropId", advisory.getCrop().getId().toString());
    }
    if (advisory.getSeason() != null) {
      metadata.put("seasonId", advisory.getSeason().getId().toString());
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

  private String normalizeImageUrl(String value) {
    String trimmed = normalizeRequired(value, "Image URL is required.");
    try {
      URI uri = new URI(trimmed);
      String scheme = uri.getScheme();
      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        throw validation("Advisory image URL must use http or https.");
      }
      return trimmed;
    } catch (URISyntaxException exception) {
      throw validation("Advisory image URL is not valid.");
    }
  }

  private String normalizeImageContentType(String value) {
    String trimmed = normalizeOptional(value);
    if (trimmed == null) {
      return null;
    }
    String normalized = trimmed.toLowerCase(Locale.ROOT);
    if (!normalized.startsWith("image/")) {
      throw validation("Advisory attachment content type must be an image.");
    }
    return normalized;
  }

  private String normalizeRequired(String value, String message) {
    if (!hasText(value)) {
      throw validation(message);
    }
    return value.trim();
  }

  private String normalizeOptional(String value) {
    return hasText(value) ? value.trim() : null;
  }

  private boolean canManageAdvisories(CurrentUser currentUser) {
    return currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER);
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
