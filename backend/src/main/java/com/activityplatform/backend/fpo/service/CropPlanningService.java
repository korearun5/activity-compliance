package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.CropCatalogRequest;
import com.activityplatform.backend.fpo.api.CropCatalogResponse;
import com.activityplatform.backend.fpo.api.CropHistoryRequest;
import com.activityplatform.backend.fpo.api.CropHistoryResponse;
import com.activityplatform.backend.fpo.api.CropPlanRequest;
import com.activityplatform.backend.fpo.api.CropPlanResponse;
import com.activityplatform.backend.fpo.api.CropSeasonRequest;
import com.activityplatform.backend.fpo.api.CropSeasonResponse;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FarmerCropHistoryEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FarmerCropHistoryRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CropPlanningService {
  private final AuditEventService auditEventService;
  private final CropCatalogRepository cropRepository;
  private final CropSeasonRepository seasonRepository;
  private final FarmPlotRepository plotRepository;
  private final FarmerCropHistoryRepository cropHistoryRepository;
  private final FpoMemberProfileRepository memberRepository;
  private final SeasonalCropPlanRepository cropPlanRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public CropPlanningService(
      AuditEventService auditEventService,
      CropCatalogRepository cropRepository,
      CropSeasonRepository seasonRepository,
      FarmPlotRepository plotRepository,
      FarmerCropHistoryRepository cropHistoryRepository,
      FpoMemberProfileRepository memberRepository,
      SeasonalCropPlanRepository cropPlanRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.cropRepository = cropRepository;
    this.seasonRepository = seasonRepository;
    this.plotRepository = plotRepository;
    this.cropHistoryRepository = cropHistoryRepository;
    this.memberRepository = memberRepository;
    this.cropPlanRepository = cropPlanRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<CropCatalogResponse> listCrops(CurrentUser currentUser) {
    requireCropPlanningModule(currentUser);
    return cropRepository.findByTenantIdOrderByNameAsc(currentUser.tenantId()).stream()
        .map(CropCatalogResponse::from)
        .toList();
  }

  @Transactional
  public CropCatalogResponse createCrop(
      CurrentUser currentUser,
      CropCatalogRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    String code = normalizeCode(request.code());
    ensureCropCodeAvailable(currentUser.tenantId(), code, null);
    Instant now = Instant.now();
    CropCatalogEntity crop = new CropCatalogEntity(
        UUID.randomUUID(),
        requireTenant(currentUser.tenantId()),
        code,
        request.name().trim(),
        normalizeOptional(request.category()),
        request.status() == null ? FarmRecordStatus.ACTIVE : request.status(),
        now
    );

    CropCatalogEntity saved = cropRepository.save(crop);
    auditCrop(currentUser, saved, AuditAction.FPO_CROP_CREATED);
    return CropCatalogResponse.from(saved);
  }

  @Transactional
  public CropCatalogResponse updateCrop(
      CurrentUser currentUser,
      UUID cropId,
      CropCatalogRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    CropCatalogEntity crop = requireCrop(currentUser, cropId);
    String code = normalizeCode(request.code());
    ensureCropCodeAvailable(currentUser.tenantId(), code, crop.getId());
    crop.updateDetails(
        code,
        request.name().trim(),
        normalizeOptional(request.category()),
        request.status() == null ? crop.getStatus() : request.status(),
        Instant.now()
    );

    CropCatalogEntity saved = cropRepository.save(crop);
    auditCrop(currentUser, saved, AuditAction.FPO_CROP_UPDATED);
    return CropCatalogResponse.from(saved);
  }

  @Transactional
  public CropCatalogResponse updateCropStatus(
      CurrentUser currentUser,
      UUID cropId,
      FarmRecordStatus status
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    CropCatalogEntity crop = requireCrop(currentUser, cropId);
    crop.updateStatus(status, Instant.now());
    CropCatalogEntity saved = cropRepository.save(crop);
    auditCrop(currentUser, saved, AuditAction.FPO_CROP_STATUS_CHANGED);
    return CropCatalogResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<CropSeasonResponse> listSeasons(CurrentUser currentUser) {
    requireCropPlanningModule(currentUser);
    return seasonRepository.findByTenantIdOrderBySeasonYearDescNameAsc(currentUser.tenantId())
        .stream()
        .map(CropSeasonResponse::from)
        .toList();
  }

  @Transactional
  public CropSeasonResponse createSeason(
      CurrentUser currentUser,
      CropSeasonRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    String code = normalizeCode(request.code());
    validateSeasonMonths(request.startMonth(), request.endMonth());
    ensureSeasonCodeAvailable(currentUser.tenantId(), code, request.seasonYear(), null);
    Instant now = Instant.now();
    CropSeasonEntity season = new CropSeasonEntity(
        UUID.randomUUID(),
        requireTenant(currentUser.tenantId()),
        code,
        request.name().trim(),
        request.startMonth(),
        request.endMonth(),
        request.seasonYear(),
        request.status() == null ? FarmRecordStatus.ACTIVE : request.status(),
        now
    );

    CropSeasonEntity saved = seasonRepository.save(season);
    auditSeason(currentUser, saved, AuditAction.FPO_SEASON_CREATED);
    return CropSeasonResponse.from(saved);
  }

  @Transactional
  public CropSeasonResponse updateSeason(
      CurrentUser currentUser,
      UUID seasonId,
      CropSeasonRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    CropSeasonEntity season = requireSeason(currentUser, seasonId);
    String code = normalizeCode(request.code());
    validateSeasonMonths(request.startMonth(), request.endMonth());
    ensureSeasonCodeAvailable(currentUser.tenantId(), code, request.seasonYear(), season.getId());
    season.updateDetails(
        code,
        request.name().trim(),
        request.startMonth(),
        request.endMonth(),
        request.seasonYear(),
        request.status() == null ? season.getStatus() : request.status(),
        Instant.now()
    );

    CropSeasonEntity saved = seasonRepository.save(season);
    auditSeason(currentUser, saved, AuditAction.FPO_SEASON_UPDATED);
    return CropSeasonResponse.from(saved);
  }

  @Transactional
  public CropSeasonResponse updateSeasonStatus(
      CurrentUser currentUser,
      UUID seasonId,
      FarmRecordStatus status
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    CropSeasonEntity season = requireSeason(currentUser, seasonId);
    season.updateStatus(status, Instant.now());
    CropSeasonEntity saved = seasonRepository.save(season);
    auditSeason(currentUser, saved, AuditAction.FPO_SEASON_STATUS_CHANGED);
    return CropSeasonResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<CropHistoryResponse> listCropHistory(
      CurrentUser currentUser,
      UUID memberId
  ) {
    requireCropPlanningModule(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    requireManagerOrOwner(currentUser, member);
    return cropHistoryRepository
        .findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(
            currentUser.tenantId(),
            memberId
        )
        .stream()
        .map(CropHistoryResponse::from)
        .toList();
  }

  @Transactional
  public CropHistoryResponse createCropHistory(
      CurrentUser currentUser,
      UUID memberId,
      CropHistoryRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    CropCatalogEntity crop = requireActiveCrop(currentUser, request.cropId());
    CropSeasonEntity season = resolveActiveSeason(currentUser, request.seasonId());
    validateCropHistory(request);
    Instant now = Instant.now();
    FarmerCropHistoryEntity history = new FarmerCropHistoryEntity(
        UUID.randomUUID(),
        member.getTenant(),
        member,
        crop,
        season,
        request.cropYear(),
        request.areaAcres(),
        request.yieldQuantity(),
        normalizeOptional(request.yieldUnit()),
        normalizeOptional(request.notes()),
        now
    );

    FarmerCropHistoryEntity saved = cropHistoryRepository.save(history);
    auditCropHistory(currentUser, saved, AuditAction.FPO_CROP_HISTORY_CREATED);
    return CropHistoryResponse.from(saved);
  }

  @Transactional
  public CropHistoryResponse updateCropHistory(
      CurrentUser currentUser,
      UUID historyId,
      CropHistoryRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    FarmerCropHistoryEntity history = requireCropHistory(currentUser, historyId);
    CropCatalogEntity crop = requireActiveCrop(currentUser, request.cropId());
    CropSeasonEntity season = resolveActiveSeason(currentUser, request.seasonId());
    validateCropHistory(request);
    history.updateDetails(
        crop,
        season,
        request.cropYear(),
        request.areaAcres(),
        request.yieldQuantity(),
        normalizeOptional(request.yieldUnit()),
        normalizeOptional(request.notes()),
        Instant.now()
    );

    FarmerCropHistoryEntity saved = cropHistoryRepository.save(history);
    auditCropHistory(currentUser, saved, AuditAction.FPO_CROP_HISTORY_UPDATED);
    return CropHistoryResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<CropPlanResponse> listCropPlans(
      CurrentUser currentUser,
      UUID memberId,
      UUID cropId,
      UUID seasonId,
      CropPlanStatus status
  ) {
    requireCropPlanningModule(currentUser);
    List<SeasonalCropPlanEntity> plans = currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)
        ? cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId())
        : cropPlanRepository.findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(
            currentUser.tenantId(),
            ownMember(currentUser).getId()
        );

    return plans.stream()
        .filter(plan -> memberId == null || plan.getMemberProfile().getId().equals(memberId))
        .filter(plan -> cropId == null || plan.getCrop().getId().equals(cropId))
        .filter(plan -> seasonId == null || plan.getSeason().getId().equals(seasonId))
        .filter(plan -> status == null || plan.getStatus() == status)
        .map(CropPlanResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public CropPlanResponse getCropPlan(CurrentUser currentUser, UUID planId) {
    requireCropPlanningModule(currentUser);
    SeasonalCropPlanEntity plan = requireCropPlan(currentUser, planId);
    requireManagerOrOwner(currentUser, plan.getMemberProfile());
    return CropPlanResponse.from(plan);
  }

  @Transactional
  public CropPlanResponse createCropPlan(
      CurrentUser currentUser,
      CropPlanRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, request.memberId());
    CropCatalogEntity crop = requireActiveCrop(currentUser, request.cropId());
    CropSeasonEntity season = requireActiveSeason(currentUser, request.seasonId());
    validatePlanDates(request);
    FarmPlotEntity plot = resolveActivePlot(currentUser, member, request);
    Instant now = Instant.now();
    SeasonalCropPlanEntity plan = new SeasonalCropPlanEntity(
        UUID.randomUUID(),
        member.getTenant(),
        member,
        plot,
        crop,
        season,
        request.plannedAreaAcres(),
        request.plannedSowingDate(),
        request.expectedHarvestDate(),
        request.status() == null ? CropPlanStatus.DRAFT : request.status(),
        now
    );

    SeasonalCropPlanEntity saved = cropPlanRepository.save(plan);
    auditCropPlan(currentUser, saved, AuditAction.FPO_CROP_PLAN_CREATED);
    return CropPlanResponse.from(saved);
  }

  @Transactional
  public CropPlanResponse updateCropPlan(
      CurrentUser currentUser,
      UUID planId,
      CropPlanRequest request
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    SeasonalCropPlanEntity plan = requireCropPlan(currentUser, planId);
    FpoMemberProfileEntity member = requireMember(currentUser, request.memberId());
    CropCatalogEntity crop = requireActiveCrop(currentUser, request.cropId());
    CropSeasonEntity season = requireActiveSeason(currentUser, request.seasonId());
    validatePlanDates(request);
    FarmPlotEntity plot = resolveActivePlot(currentUser, member, request);
    plan.updateDetails(
        member,
        plot,
        crop,
        season,
        request.plannedAreaAcres(),
        request.plannedSowingDate(),
        request.expectedHarvestDate(),
        request.status() == null ? plan.getStatus() : request.status(),
        Instant.now()
    );

    SeasonalCropPlanEntity saved = cropPlanRepository.save(plan);
    auditCropPlan(currentUser, saved, AuditAction.FPO_CROP_PLAN_UPDATED);
    return CropPlanResponse.from(saved);
  }

  @Transactional
  public CropPlanResponse updateCropPlanStatus(
      CurrentUser currentUser,
      UUID planId,
      CropPlanStatus status
  ) {
    requireCropPlanningModule(currentUser);
    requireManager(currentUser);
    SeasonalCropPlanEntity plan = requireCropPlan(currentUser, planId);
    plan.updateStatus(status, Instant.now());
    SeasonalCropPlanEntity saved = cropPlanRepository.save(plan);
    auditCropPlan(currentUser, saved, AuditAction.FPO_CROP_PLAN_STATUS_CHANGED);
    return CropPlanResponse.from(saved);
  }

  private void requireCropPlanningModule(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.CROP_PLANNING);
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private FpoMemberProfileEntity requireMember(CurrentUser currentUser, UUID memberId) {
    return memberRepository.findByIdAndTenantId(memberId, currentUser.tenantId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
  }

  private FpoMemberProfileEntity ownMember(CurrentUser currentUser) {
    return memberRepository.findByTenantIdAndUserId(currentUser.tenantId(), currentUser.userId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
  }

  private CropCatalogEntity requireCrop(CurrentUser currentUser, UUID cropId) {
    return cropRepository.findByIdAndTenantId(cropId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop not found."));
  }

  private CropCatalogEntity requireActiveCrop(CurrentUser currentUser, UUID cropId) {
    CropCatalogEntity crop = requireCrop(currentUser, cropId);
    if (crop.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Crop must be active.");
    }
    return crop;
  }

  private CropSeasonEntity requireSeason(CurrentUser currentUser, UUID seasonId) {
    return seasonRepository.findByIdAndTenantId(seasonId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop season not found."));
  }

  private CropSeasonEntity requireActiveSeason(CurrentUser currentUser, UUID seasonId) {
    CropSeasonEntity season = requireSeason(currentUser, seasonId);
    if (season.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Crop season must be active.");
    }
    return season;
  }

  private CropSeasonEntity resolveActiveSeason(CurrentUser currentUser, UUID seasonId) {
    return seasonId == null ? null : requireActiveSeason(currentUser, seasonId);
  }

  private FarmerCropHistoryEntity requireCropHistory(CurrentUser currentUser, UUID historyId) {
    return cropHistoryRepository.findByIdAndTenantId(historyId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop history record not found."));
  }

  private SeasonalCropPlanEntity requireCropPlan(CurrentUser currentUser, UUID planId) {
    return cropPlanRepository.findByIdAndTenantId(planId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Crop plan not found."));
  }

  private FarmPlotEntity resolveActivePlot(
      CurrentUser currentUser,
      FpoMemberProfileEntity member,
      CropPlanRequest request
  ) {
    if (request.plotId() == null) {
      return null;
    }

    FarmPlotEntity plot = plotRepository.findByIdAndTenantId(
        request.plotId(),
        currentUser.tenantId()
    ).orElseThrow(() -> notFound("Farm plot not found."));
    if (!plot.getMemberProfile().getId().equals(member.getId())) {
      throw validation("Farm plot must belong to the selected FPO member.");
    }
    if (plot.getStatus() != FarmRecordStatus.ACTIVE) {
      throw validation("Crop plans can only use active farm plots.");
    }
    if (request.plannedAreaAcres().compareTo(plot.getAreaAcres()) > 0) {
      throw validation("Planned acreage cannot exceed selected plot area.");
    }
    return plot;
  }

  private void ensureCropCodeAvailable(UUID tenantId, String code, UUID currentId) {
    cropRepository.findByTenantIdAndCodeIgnoreCase(tenantId, code)
        .filter(crop -> !crop.getId().equals(currentId))
        .ifPresent(crop -> {
          throw conflict("Crop code already exists for this tenant.");
        });
  }

  private void ensureSeasonCodeAvailable(
      UUID tenantId,
      String code,
      Integer seasonYear,
      UUID currentId
  ) {
    seasonRepository.findByTenantIdAndCodeIgnoreCaseAndSeasonYear(tenantId, code, seasonYear)
        .filter(season -> !season.getId().equals(currentId))
        .ifPresent(season -> {
          throw conflict("Season code already exists for this tenant and year.");
        });
  }

  private void validateSeasonMonths(Integer startMonth, Integer endMonth) {
    if ((startMonth == null) != (endMonth == null)) {
      throw validation("Provide both start month and end month, or leave both blank.");
    }
  }

  private void validateCropHistory(CropHistoryRequest request) {
    if (request.yieldQuantity() != null && !hasText(request.yieldUnit())) {
      throw validation("Yield unit is required when yield quantity is provided.");
    }
  }

  private void validatePlanDates(CropPlanRequest request) {
    if (
        request.plannedSowingDate() != null
            && request.expectedHarvestDate() != null
            && request.expectedHarvestDate().isBefore(request.plannedSowingDate())
    ) {
      throw validation("Expected harvest date cannot be before planned sowing date.");
    }
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins and supervisors can manage crop planning records.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private void requireManagerOrOwner(CurrentUser currentUser, FpoMemberProfileEntity member) {
    if (currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)) {
      return;
    }

    if (member.getUser().getId().equals(currentUser.userId())) {
      return;
    }

    throw new ApplicationException(
        ErrorCode.ACCESS_DENIED,
        "You do not have permission to view this member's crop planning records.",
        HttpStatus.FORBIDDEN
    );
  }

  private void auditCrop(CurrentUser currentUser, CropCatalogEntity crop, AuditAction action) {
    auditEventService.record(
        crop.getTenant(),
        actor(currentUser),
        "FPO_CROP",
        crop.getId(),
        action,
        Map.of(
            "code", crop.getCode(),
            "status", crop.getStatus().name()
        )
    );
  }

  private void auditSeason(CurrentUser currentUser, CropSeasonEntity season, AuditAction action) {
    auditEventService.record(
        season.getTenant(),
        actor(currentUser),
        "FPO_SEASON",
        season.getId(),
        action,
        Map.of(
            "code", season.getCode(),
            "seasonYear", season.getSeasonYear(),
            "status", season.getStatus().name()
        )
    );
  }

  private void auditCropHistory(
      CurrentUser currentUser,
      FarmerCropHistoryEntity history,
      AuditAction action
  ) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("memberId", history.getMemberProfile().getId().toString());
    metadata.put("cropId", history.getCrop().getId().toString());
    metadata.put("cropYear", history.getCropYear());
    if (history.getSeason() != null) {
      metadata.put("seasonId", history.getSeason().getId().toString());
    }
    auditEventService.record(
        history.getTenant(),
        actor(currentUser),
        "FPO_CROP_HISTORY",
        history.getId(),
        action,
        metadata
    );
  }

  private void auditCropPlan(
      CurrentUser currentUser,
      SeasonalCropPlanEntity plan,
      AuditAction action
  ) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("memberId", plan.getMemberProfile().getId().toString());
    metadata.put("cropId", plan.getCrop().getId().toString());
    metadata.put("seasonId", plan.getSeason().getId().toString());
    metadata.put("status", plan.getStatus().name());
    if (plan.getPlot() != null) {
      metadata.put("plotId", plan.getPlot().getId().toString());
    }
    auditEventService.record(
        plan.getTenant(),
        actor(currentUser),
        "FPO_CROP_PLAN",
        plan.getId(),
        action,
        metadata
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private String normalizeCode(String value) {
    return value.trim().toUpperCase();
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
