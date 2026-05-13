package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.CreateFarmLandholdingRequest;
import com.activityplatform.backend.fpo.api.CreateFarmPlotRequest;
import com.activityplatform.backend.fpo.api.FarmLandholdingResponse;
import com.activityplatform.backend.fpo.api.FarmPlotResponse;
import com.activityplatform.backend.fpo.api.UpdateFarmLandholdingRequest;
import com.activityplatform.backend.fpo.api.UpdateFarmPlotRequest;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmAssetService {
  private final AuditEventService auditEventService;
  private final FarmLandholdingRepository landholdingRepository;
  private final FarmPlotRepository plotRepository;
  private final FpoMemberProfileRepository memberRepository;
  private final TenantModuleService tenantModuleService;
  private final UserRepository userRepository;

  public FarmAssetService(
      AuditEventService auditEventService,
      FarmLandholdingRepository landholdingRepository,
      FarmPlotRepository plotRepository,
      FpoMemberProfileRepository memberRepository,
      TenantModuleService tenantModuleService,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.landholdingRepository = landholdingRepository;
    this.plotRepository = plotRepository;
    this.memberRepository = memberRepository;
    this.tenantModuleService = tenantModuleService;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<FarmLandholdingResponse> listLandholdings(
      CurrentUser currentUser,
      UUID memberId
  ) {
    requireLandRecordsModule(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    requireManagerOrOwner(currentUser, member);
    return landholdingRepository
        .findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(currentUser.tenantId(), memberId)
        .stream()
        .map(FarmLandholdingResponse::from)
        .toList();
  }

  @Transactional
  public FarmLandholdingResponse createLandholding(
      CurrentUser currentUser,
      UUID memberId,
      CreateFarmLandholdingRequest request
  ) {
    requireLandRecordsModule(currentUser);
    requireManager(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    validateArea(request.totalAreaAcres(), request.cultivableAreaAcres());
    Instant now = Instant.now();

    FarmLandholdingEntity landholding = new FarmLandholdingEntity(
        UUID.randomUUID(),
        member.getTenant(),
        member,
        FarmAssetRules.normalizeRequiredText(request.surveyNumber(), "Survey number / Khasra number"),
        request.totalAreaAcres(),
        request.cultivableAreaAcres(),
        FarmAssetRules.normalizeOwnershipType(request.ownershipType()),
        FarmAssetRules.normalizeIrrigationSource(request.irrigationSource()),
        request.status() == null ? FarmRecordStatus.ACTIVE : request.status(),
        now
    );
    FarmLandholdingEntity saved = landholdingRepository.save(landholding);
    auditLandholding(currentUser, saved, AuditAction.FPO_LANDHOLDING_CREATED);
    return FarmLandholdingResponse.from(saved);
  }

  @Transactional
  public FarmLandholdingResponse updateLandholding(
      CurrentUser currentUser,
      UUID landholdingId,
      UpdateFarmLandholdingRequest request
  ) {
    requireLandRecordsModule(currentUser);
    requireManager(currentUser);
    FarmLandholdingEntity landholding = requireLandholding(currentUser, landholdingId);
    validateArea(request.totalAreaAcres(), request.cultivableAreaAcres());
    landholding.updateDetails(
        FarmAssetRules.normalizeRequiredText(request.surveyNumber(), "Survey number / Khasra number"),
        request.totalAreaAcres(),
        request.cultivableAreaAcres(),
        FarmAssetRules.normalizeOwnershipType(request.ownershipType()),
        FarmAssetRules.normalizeIrrigationSource(request.irrigationSource()),
        request.status(),
        Instant.now()
    );
    FarmLandholdingEntity saved = landholdingRepository.save(landholding);
    auditLandholding(currentUser, saved, AuditAction.FPO_LANDHOLDING_UPDATED);
    return FarmLandholdingResponse.from(saved);
  }

  @Transactional
  public FarmLandholdingResponse updateLandholdingStatus(
      CurrentUser currentUser,
      UUID landholdingId,
      FarmRecordStatus status
  ) {
    requireLandRecordsModule(currentUser);
    requireManager(currentUser);
    FarmLandholdingEntity landholding = requireLandholding(currentUser, landholdingId);
    landholding.updateStatus(status, Instant.now());
    FarmLandholdingEntity saved = landholdingRepository.save(landholding);
    auditLandholding(currentUser, saved, AuditAction.FPO_LANDHOLDING_STATUS_CHANGED);
    return FarmLandholdingResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<FarmPlotResponse> listPlots(CurrentUser currentUser, UUID memberId) {
    requireLandRecordsModule(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    requireManagerOrOwner(currentUser, member);
    return plotRepository
        .findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(currentUser.tenantId(), memberId)
        .stream()
        .map(FarmPlotResponse::from)
        .toList();
  }

  @Transactional
  public FarmPlotResponse createPlot(
      CurrentUser currentUser,
      UUID memberId,
      CreateFarmPlotRequest request
  ) {
    requireLandRecordsModule(currentUser);
    requireManager(currentUser);
    FpoMemberProfileEntity member = requireMember(currentUser, memberId);
    FarmLandholdingEntity landholding = resolveLandholding(
        currentUser,
        member,
        request.landholdingId()
    );
    FarmAssetRules.validateGpsPoint(request.latitude(), request.longitude());
    Instant now = Instant.now();
    FarmPlotEntity plot = new FarmPlotEntity(
        UUID.randomUUID(),
        member.getTenant(),
        member,
        landholding,
        request.plotName().trim(),
        request.areaAcres(),
        request.latitude(),
        request.longitude(),
        normalizeOptional(request.soilType()),
        request.status() == null ? FarmRecordStatus.ACTIVE : request.status(),
        now
    );
    FarmPlotEntity saved = plotRepository.save(plot);
    auditPlot(currentUser, saved, AuditAction.FPO_PLOT_CREATED);
    return FarmPlotResponse.from(saved);
  }

  @Transactional
  public FarmPlotResponse updatePlot(
      CurrentUser currentUser,
      UUID plotId,
      UpdateFarmPlotRequest request
  ) {
    requireLandRecordsModule(currentUser);
    requireManager(currentUser);
    FarmPlotEntity plot = requirePlot(currentUser, plotId);
    FarmLandholdingEntity landholding = resolveLandholding(
        currentUser,
        plot.getMemberProfile(),
        request.landholdingId()
    );
    FarmAssetRules.validateGpsPoint(request.latitude(), request.longitude());
    plot.updateDetails(
        landholding,
        request.plotName().trim(),
        request.areaAcres(),
        request.latitude(),
        request.longitude(),
        normalizeOptional(request.soilType()),
        request.status(),
        Instant.now()
    );
    FarmPlotEntity saved = plotRepository.save(plot);
    auditPlot(currentUser, saved, AuditAction.FPO_PLOT_UPDATED);
    return FarmPlotResponse.from(saved);
  }

  @Transactional
  public FarmPlotResponse updatePlotStatus(
      CurrentUser currentUser,
      UUID plotId,
      FarmRecordStatus status
  ) {
    requireLandRecordsModule(currentUser);
    requireManager(currentUser);
    FarmPlotEntity plot = requirePlot(currentUser, plotId);
    plot.updateStatus(status, Instant.now());
    FarmPlotEntity saved = plotRepository.save(plot);
    auditPlot(currentUser, saved, AuditAction.FPO_PLOT_STATUS_CHANGED);
    return FarmPlotResponse.from(saved);
  }

  private void requireLandRecordsModule(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.LAND_RECORDS);
  }

  private FpoMemberProfileEntity requireMember(CurrentUser currentUser, UUID memberId) {
    return memberRepository.findByIdAndTenantId(memberId, currentUser.tenantId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
  }

  private FarmLandholdingEntity requireLandholding(CurrentUser currentUser, UUID landholdingId) {
    return landholdingRepository.findByIdAndTenantId(landholdingId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Landholding not found."));
  }

  private FarmPlotEntity requirePlot(CurrentUser currentUser, UUID plotId) {
    return plotRepository.findByIdAndTenantId(plotId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Farm plot not found."));
  }

  private FarmLandholdingEntity resolveLandholding(
      CurrentUser currentUser,
      FpoMemberProfileEntity member,
      UUID landholdingId
  ) {
    if (landholdingId == null) {
      return null;
    }

    FarmLandholdingEntity landholding = requireLandholding(currentUser, landholdingId);
    if (!landholding.getMemberProfile().getId().equals(member.getId())) {
      throw validation("Landholding must belong to the selected FPO member.");
    }
    return landholding;
  }

  private void validateArea(BigDecimal totalAreaAcres, BigDecimal cultivableAreaAcres) {
    if (totalAreaAcres == null) {
      throw validation("Total area is required.");
    }

    if (cultivableAreaAcres != null && cultivableAreaAcres.compareTo(totalAreaAcres) > 0) {
      throw validation("Cultivable area cannot exceed total area.");
    }
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only Phase 1 staff can manage farm land records.",
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
        "You do not have permission to view this member's farm records.",
        HttpStatus.FORBIDDEN
    );
  }

  private void auditLandholding(
      CurrentUser currentUser,
      FarmLandholdingEntity landholding,
      AuditAction action
  ) {
    auditEventService.record(
        landholding.getTenant(),
        actor(currentUser),
        "FARM_LANDHOLDING",
        landholding.getId(),
        action,
        Map.of(
            "memberId", landholding.getMemberProfile().getId().toString(),
            "status", landholding.getStatus().name()
        )
    );
  }

  private void auditPlot(CurrentUser currentUser, FarmPlotEntity plot, AuditAction action) {
    auditEventService.record(
        plot.getTenant(),
        actor(currentUser),
        "FARM_PLOT",
        plot.getId(),
        action,
        Map.of(
            "memberId", plot.getMemberProfile().getId().toString(),
            "status", plot.getStatus().name()
        )
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
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
