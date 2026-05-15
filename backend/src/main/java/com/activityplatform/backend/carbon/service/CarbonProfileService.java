package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.carbon.api.CarbonFarmPlotRequest;
import com.activityplatform.backend.carbon.api.CarbonFarmPlotResponse;
import com.activityplatform.backend.carbon.api.CarbonProfileRequest;
import com.activityplatform.backend.carbon.api.CarbonProfileResponse;
import com.activityplatform.backend.carbon.api.CarbonSoilProfileRequest;
import com.activityplatform.backend.carbon.api.CarbonSoilProfileResponse;
import com.activityplatform.backend.carbon.domain.CarbonFarmPlotEntity;
import com.activityplatform.backend.carbon.domain.CarbonParticipantType;
import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import com.activityplatform.backend.carbon.repository.CarbonFarmPlotRepository;
import com.activityplatform.backend.carbon.repository.CarbonProfileRepository;
import com.activityplatform.backend.carbon.repository.CarbonSoilProfileRepository;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarbonProfileService {
  private static final String CARBON_PREFIX = "CAR-";

  private final AuditEventService auditEventService;
  private final CarbonFarmPlotRepository farmPlotRepository;
  private final CarbonProfileRepository profileRepository;
  private final CarbonSoilProfileRepository soilProfileRepository;
  private final FpoMemberProfileRepository fpoMemberProfileRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public CarbonProfileService(
      AuditEventService auditEventService,
      CarbonFarmPlotRepository farmPlotRepository,
      CarbonProfileRepository profileRepository,
      CarbonSoilProfileRepository soilProfileRepository,
      FpoMemberProfileRepository fpoMemberProfileRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.farmPlotRepository = farmPlotRepository;
    this.profileRepository = profileRepository;
    this.soilProfileRepository = soilProfileRepository;
    this.fpoMemberProfileRepository = fpoMemberProfileRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public Page<CarbonProfileResponse> list(
      CurrentUser currentUser,
      CarbonRecordStatus status,
      Pageable pageable
  ) {
    requireCarbonModule(currentUser);
    CarbonAccessPolicy.requireCarbonStaff(
        currentUser,
        "Only Carbon staff can list Carbon profiles."
    );

    if (currentUser.hasAnyRole(Role.FIELD_COORDINATOR)) {
      if (status == null) {
        return profileRepository
            .findByTenantIdAndCoordinatorUserId(
                currentUser.tenantId(),
                currentUser.userId(),
                pageable
            )
            .map(CarbonProfileResponse::from);
      }

      return profileRepository
          .findByTenantIdAndCoordinatorUserIdAndStatus(
              currentUser.tenantId(),
              currentUser.userId(),
              status,
              pageable
          )
          .map(CarbonProfileResponse::from);
    }

    if (status == null) {
      return profileRepository.findByTenantId(currentUser.tenantId(), pageable)
          .map(CarbonProfileResponse::from);
    }

    return profileRepository.findByTenantIdAndStatus(currentUser.tenantId(), status, pageable)
        .map(CarbonProfileResponse::from);
  }

  @Transactional(readOnly = true)
  public CarbonProfileResponse get(CurrentUser currentUser, UUID profileId) {
    requireCarbonModule(currentUser);
    CarbonProfileEntity profile = requireProfile(currentUser, profileId);
    CarbonAccessPolicy.requireProfileAccess(
        currentUser,
        profile,
        "You do not have permission to view this Carbon profile."
    );
    return CarbonProfileResponse.from(profile);
  }

  @Transactional(readOnly = true)
  public CarbonProfileResponse me(CurrentUser currentUser) {
    requireCarbonModule(currentUser);
    CarbonProfileEntity profile = profileRepository
        .findByTenantIdAndUserId(currentUser.tenantId(), currentUser.userId())
        .orElseThrow(() -> notFound("Carbon profile not found."));
    CarbonAccessPolicy.requireProfileAccess(
        currentUser,
        profile,
        "You do not have permission to view this Carbon profile."
    );
    return CarbonProfileResponse.from(profile);
  }

  @Transactional
  public CarbonProfileResponse create(CurrentUser currentUser, CarbonProfileRequest request) {
    requireCarbonModule(currentUser);
    CarbonAccessPolicy.requireCarbonStaff(
        currentUser,
        "Only Carbon staff can create Carbon profiles."
    );

    TenantEntity tenant = requireTenant(currentUser.tenantId());
    FpoMemberProfileEntity member = resolveFpoMember(currentUser, request.fpoMemberProfileId());
    UserEntity user = resolveProfileUser(currentUser, request.userId(), member);
    UserEntity coordinator = resolveCoordinatorForCreate(currentUser, request.coordinatorUserId());
    CarbonParticipantType participantType = participantType(request.participantType());
    validateParticipantUser(participantType, user);
    String carbonIdentityId = normalizeCarbonIdentityId(
        currentUser.tenantId(),
        request.carbonIdentityId(),
        null
    );
    Instant now = Instant.now();

    CarbonProfileRules.validateOptionalGpsPair(request.gpsLatitude(), request.gpsLongitude());
    CarbonProfileEntity profile = new CarbonProfileEntity(
        UUID.randomUUID(),
        tenant,
        user,
        member,
        coordinator,
        carbonIdentityId,
        participantType,
        resolveDisplayName(request.displayName(), user, member),
        CarbonProfileRules.normalizeOptionalIndianMobile(request.mobileNumber()),
        CarbonProfileRules.normalizeLanguagePreference(request.languagePreference()),
        CarbonProfileRules.normalizeOptionalText(request.village()),
        CarbonProfileRules.normalizeOptionalText(request.taluka()),
        CarbonProfileRules.normalizeOptionalText(request.districtName()),
        CarbonProfileRules.normalizeOptionalText(request.stateName()),
        request.gpsLatitude(),
        request.gpsLongitude(),
        request.totalLandHoldingAcres(),
        CarbonProfileRules.normalizeOptionalText(request.croppingPattern()),
        request.livestockCount(),
        CarbonProfileRules.normalizeTillageStatus(request.tillageStatus()),
        CarbonProfileRules.normalizeBankStatus(request.bankStatus()),
        CarbonProfileRules.normalizeAadhaarStatus(request.aadhaarStatus()),
        CarbonProfileRules.normalizeDocumentStatus(request.documentStatus()),
        statusOrActive(request.status()),
        now
    );

    CarbonProfileEntity saved = saveProfile(profile);
    auditProfile(currentUser, saved, AuditAction.CARBON_PROFILE_CREATED);
    return CarbonProfileResponse.from(saved);
  }

  @Transactional
  public CarbonProfileResponse update(
      CurrentUser currentUser,
      UUID profileId,
      CarbonProfileRequest request
  ) {
    requireCarbonModule(currentUser);
    CarbonProfileEntity profile = requireProfile(currentUser, profileId);
    CarbonAccessPolicy.requireProfileMutationAccess(
        currentUser,
        profile,
        "You do not have permission to update this Carbon profile."
    );

    FpoMemberProfileEntity member = resolveFpoMember(currentUser, request.fpoMemberProfileId());
    UserEntity user = resolveProfileUser(currentUser, request.userId(), member);
    UserEntity coordinator = resolveCoordinatorForUpdate(currentUser, request.coordinatorUserId());
    CarbonParticipantType participantType = participantType(request.participantType());
    validateParticipantUser(participantType, user);
    String requestedIdentityId = CarbonProfileRules.normalizeOptionalText(request.carbonIdentityId());
    String carbonIdentityId = normalizeCarbonIdentityId(
        currentUser.tenantId(),
        requestedIdentityId == null ? profile.getCarbonIdentityId() : requestedIdentityId,
        profile.getId()
    );

    CarbonProfileRules.validateOptionalGpsPair(request.gpsLatitude(), request.gpsLongitude());
    profile.updateDetails(
        user,
        member,
        coordinator,
        carbonIdentityId,
        participantType,
        resolveDisplayName(request.displayName(), user, member),
        CarbonProfileRules.normalizeOptionalIndianMobile(request.mobileNumber()),
        CarbonProfileRules.normalizeLanguagePreference(request.languagePreference()),
        CarbonProfileRules.normalizeOptionalText(request.village()),
        CarbonProfileRules.normalizeOptionalText(request.taluka()),
        CarbonProfileRules.normalizeOptionalText(request.districtName()),
        CarbonProfileRules.normalizeOptionalText(request.stateName()),
        request.gpsLatitude(),
        request.gpsLongitude(),
        request.totalLandHoldingAcres(),
        CarbonProfileRules.normalizeOptionalText(request.croppingPattern()),
        request.livestockCount(),
        CarbonProfileRules.normalizeTillageStatus(request.tillageStatus()),
        CarbonProfileRules.normalizeBankStatus(request.bankStatus()),
        CarbonProfileRules.normalizeAadhaarStatus(request.aadhaarStatus()),
        CarbonProfileRules.normalizeDocumentStatus(request.documentStatus()),
        statusOrActive(request.status()),
        Instant.now()
    );

    CarbonProfileEntity saved = saveProfile(profile);
    auditProfile(currentUser, saved, AuditAction.CARBON_PROFILE_UPDATED);
    return CarbonProfileResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public java.util.List<CarbonFarmPlotResponse> listPlots(CurrentUser currentUser, UUID profileId) {
    requireCarbonModule(currentUser);
    CarbonProfileEntity profile = requireAccessibleProfile(currentUser, profileId);
    return farmPlotRepository
        .findByTenantIdAndCarbonProfileIdOrderByCreatedAtDesc(
            currentUser.tenantId(),
            profile.getId()
        )
        .stream()
        .map(CarbonFarmPlotResponse::from)
        .toList();
  }

  @Transactional
  public CarbonFarmPlotResponse createPlot(
      CurrentUser currentUser,
      UUID profileId,
      CarbonFarmPlotRequest request
  ) {
    requireCarbonModule(currentUser);
    CarbonProfileEntity profile = requireMutableProfile(currentUser, profileId);
    Instant now = Instant.now();
    CarbonFarmPlotEntity plot = new CarbonFarmPlotEntity(
        UUID.randomUUID(),
        profile.getTenant(),
        profile,
        CarbonProfileRules.normalizeRequiredText(request.farmName(), "Farm name"),
        CarbonProfileRules.normalizeOptionalText(request.surveyNumber()),
        request.areaAcres(),
        request.latitude(),
        request.longitude(),
        CarbonProfileRules.normalizeOptionalText(request.irrigationSource()),
        CarbonProfileRules.normalizeOptionalText(request.primaryCrop()),
        CarbonProfileRules.normalizeTillageStatus(request.tillageStatus()),
        statusOrActive(request.status()),
        now
    );
    CarbonFarmPlotEntity saved = farmPlotRepository.save(plot);
    auditPlot(currentUser, saved, AuditAction.CARBON_FARM_PLOT_CREATED);
    return CarbonFarmPlotResponse.from(saved);
  }

  @Transactional
  public CarbonFarmPlotResponse updatePlot(
      CurrentUser currentUser,
      UUID plotId,
      CarbonFarmPlotRequest request
  ) {
    requireCarbonModule(currentUser);
    CarbonFarmPlotEntity plot = requirePlot(currentUser, plotId);
    CarbonAccessPolicy.requireProfileMutationAccess(
        currentUser,
        plot.getCarbonProfile(),
        "You do not have permission to update this Carbon farm plot."
    );
    plot.updateDetails(
        CarbonProfileRules.normalizeRequiredText(request.farmName(), "Farm name"),
        CarbonProfileRules.normalizeOptionalText(request.surveyNumber()),
        request.areaAcres(),
        request.latitude(),
        request.longitude(),
        CarbonProfileRules.normalizeOptionalText(request.irrigationSource()),
        CarbonProfileRules.normalizeOptionalText(request.primaryCrop()),
        CarbonProfileRules.normalizeTillageStatus(request.tillageStatus()),
        statusOrActive(request.status()),
        Instant.now()
    );
    CarbonFarmPlotEntity saved = farmPlotRepository.save(plot);
    auditPlot(currentUser, saved, AuditAction.CARBON_FARM_PLOT_UPDATED);
    return CarbonFarmPlotResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public java.util.List<CarbonSoilProfileResponse> listSoilProfiles(
      CurrentUser currentUser,
      UUID profileId
  ) {
    requireCarbonModule(currentUser);
    CarbonProfileEntity profile = requireAccessibleProfile(currentUser, profileId);
    return soilProfileRepository
        .findByTenantIdAndCarbonProfileIdOrderByCreatedAtDesc(
            currentUser.tenantId(),
            profile.getId()
        )
        .stream()
        .map(CarbonSoilProfileResponse::from)
        .toList();
  }

  @Transactional
  public CarbonSoilProfileResponse createSoilProfile(
      CurrentUser currentUser,
      UUID profileId,
      CarbonSoilProfileRequest request
  ) {
    requireCarbonModule(currentUser);
    CarbonProfileEntity profile = requireMutableProfile(currentUser, profileId);
    CarbonFarmPlotEntity plot = resolvePlot(currentUser, profile, request.carbonFarmPlotId());
    Instant now = Instant.now();
    CarbonSoilProfileEntity soilProfile = new CarbonSoilProfileEntity(
        UUID.randomUUID(),
        profile.getTenant(),
        profile,
        plot,
        request.testDate(),
        CarbonProfileRules.normalizeOptionalText(request.labName()),
        request.soilOrganicCarbonPercent(),
        request.ph(),
        request.electricalConductivity(),
        request.nitrogenKgHa(),
        request.phosphorusKgHa(),
        request.potassiumKgHa(),
        request.bulkDensityGmCm3(),
        CarbonProfileRules.normalizeOptionalText(request.texture()),
        CarbonProfileRules.normalizeOptionalText(request.reportFileName()),
        CarbonProfileRules.normalizeReportContentType(request.reportContentType()),
        CarbonProfileRules.normalizeOptionalText(request.reportStorageKey()),
        CarbonProfileRules.normalizeReportUrl(request.reportUrl()),
        statusOrActive(request.status()),
        now
    );
    CarbonSoilProfileEntity saved = soilProfileRepository.save(soilProfile);
    auditSoilProfile(currentUser, saved, AuditAction.CARBON_SOIL_PROFILE_CREATED);
    return CarbonSoilProfileResponse.from(saved);
  }

  @Transactional
  public CarbonSoilProfileResponse updateSoilProfile(
      CurrentUser currentUser,
      UUID soilProfileId,
      CarbonSoilProfileRequest request
  ) {
    requireCarbonModule(currentUser);
    CarbonSoilProfileEntity soilProfile = requireSoilProfile(currentUser, soilProfileId);
    CarbonAccessPolicy.requireProfileMutationAccess(
        currentUser,
        soilProfile.getCarbonProfile(),
        "You do not have permission to update this Carbon soil profile."
    );
    CarbonFarmPlotEntity plot = resolvePlot(
        currentUser,
        soilProfile.getCarbonProfile(),
        request.carbonFarmPlotId()
    );
    soilProfile.updateDetails(
        plot,
        request.testDate(),
        CarbonProfileRules.normalizeOptionalText(request.labName()),
        request.soilOrganicCarbonPercent(),
        request.ph(),
        request.electricalConductivity(),
        request.nitrogenKgHa(),
        request.phosphorusKgHa(),
        request.potassiumKgHa(),
        request.bulkDensityGmCm3(),
        CarbonProfileRules.normalizeOptionalText(request.texture()),
        CarbonProfileRules.normalizeOptionalText(request.reportFileName()),
        CarbonProfileRules.normalizeReportContentType(request.reportContentType()),
        CarbonProfileRules.normalizeOptionalText(request.reportStorageKey()),
        CarbonProfileRules.normalizeReportUrl(request.reportUrl()),
        statusOrActive(request.status()),
        Instant.now()
    );
    CarbonSoilProfileEntity saved = soilProfileRepository.save(soilProfile);
    auditSoilProfile(currentUser, saved, AuditAction.CARBON_SOIL_PROFILE_UPDATED);
    return CarbonSoilProfileResponse.from(saved);
  }

  private void requireCarbonModule(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
  }

  private CarbonProfileEntity requireProfile(CurrentUser currentUser, UUID profileId) {
    return profileRepository.findByIdAndTenantId(profileId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Carbon profile not found."));
  }

  private CarbonProfileEntity requireAccessibleProfile(CurrentUser currentUser, UUID profileId) {
    CarbonProfileEntity profile = requireProfile(currentUser, profileId);
    CarbonAccessPolicy.requireProfileAccess(
        currentUser,
        profile,
        "You do not have permission to view this Carbon profile."
    );
    return profile;
  }

  private CarbonProfileEntity requireMutableProfile(CurrentUser currentUser, UUID profileId) {
    CarbonProfileEntity profile = requireProfile(currentUser, profileId);
    CarbonAccessPolicy.requireProfileMutationAccess(
        currentUser,
        profile,
        "You do not have permission to manage this Carbon profile."
    );
    return profile;
  }

  private CarbonFarmPlotEntity requirePlot(CurrentUser currentUser, UUID plotId) {
    return farmPlotRepository.findByIdAndTenantId(plotId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Carbon farm plot not found."));
  }

  private CarbonSoilProfileEntity requireSoilProfile(CurrentUser currentUser, UUID soilProfileId) {
    return soilProfileRepository.findByIdAndTenantId(soilProfileId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Carbon soil profile not found."));
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private UserEntity requireUser(CurrentUser currentUser, UUID userId) {
    return userRepository.findByIdAndTenantId(userId, currentUser.tenantId())
        .orElseThrow(() -> notFound("User not found."));
  }

  private FpoMemberProfileEntity resolveFpoMember(CurrentUser currentUser, UUID memberId) {
    if (memberId == null) {
      return null;
    }

    return fpoMemberProfileRepository.findByIdAndTenantId(memberId, currentUser.tenantId())
        .orElseThrow(() -> notFound("FPO member profile not found."));
  }

  private UserEntity resolveProfileUser(
      CurrentUser currentUser,
      UUID userId,
      FpoMemberProfileEntity member
  ) {
    if (userId == null) {
      return member == null ? null : member.getUser();
    }

    UserEntity user = requireUser(currentUser, userId);
    if (member != null && !member.getUser().getId().equals(user.getId())) {
      throw validation("Linked user must match the selected FPO member profile user.");
    }
    return user;
  }

  private UserEntity resolveCoordinatorForCreate(CurrentUser currentUser, UUID coordinatorUserId) {
    if (currentUser.hasAnyRole(Role.FIELD_COORDINATOR)) {
      if (coordinatorUserId != null && !coordinatorUserId.equals(currentUser.userId())) {
        throw accessDenied("Field coordinators can only assign Carbon profiles to themselves.");
      }
      return requireUser(currentUser, currentUser.userId());
    }

    return coordinatorUserId == null ? null : requireFieldCoordinator(currentUser, coordinatorUserId);
  }

  private UserEntity resolveCoordinatorForUpdate(CurrentUser currentUser, UUID coordinatorUserId) {
    if (currentUser.hasAnyRole(Role.FIELD_COORDINATOR)) {
      if (coordinatorUserId != null && !coordinatorUserId.equals(currentUser.userId())) {
        throw accessDenied("Field coordinators can only keep Carbon profiles assigned to themselves.");
      }
      return requireUser(currentUser, currentUser.userId());
    }

    return coordinatorUserId == null ? null : requireFieldCoordinator(currentUser, coordinatorUserId);
  }

  private UserEntity requireFieldCoordinator(CurrentUser currentUser, UUID coordinatorUserId) {
    UserEntity coordinator = requireUser(currentUser, coordinatorUserId);
    if (!hasRole(coordinator, Role.FIELD_COORDINATOR)) {
      throw validation("Coordinator must be a field coordinator user.");
    }
    return coordinator;
  }

  private CarbonFarmPlotEntity resolvePlot(
      CurrentUser currentUser,
      CarbonProfileEntity profile,
      UUID plotId
  ) {
    if (plotId == null) {
      return null;
    }

    CarbonFarmPlotEntity plot = requirePlot(currentUser, plotId);
    if (!plot.getCarbonProfile().getId().equals(profile.getId())) {
      throw validation("Carbon farm plot must belong to the selected Carbon profile.");
    }
    return plot;
  }

  private void validateParticipantUser(CarbonParticipantType participantType, UserEntity user) {
    if (participantType == CarbonParticipantType.FARMER
        && user != null
        && !hasRole(user, Role.FARMER)) {
      throw validation("Farmer Carbon profiles must be linked to farmer users.");
    }
  }

  private boolean hasRole(UserEntity user, Role role) {
    return user.getRoles().stream()
        .map(RoleEntity::getCode)
        .anyMatch(role.name()::equals);
  }

  private String normalizeCarbonIdentityId(UUID tenantId, String value, UUID currentProfileId) {
    String normalized = CarbonProfileRules.normalizeOptionalText(value);
    if (normalized == null) {
      normalized = CARBON_PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    String carbonIdentityId = normalized;
    profileRepository.findByTenantIdAndCarbonIdentityIdIgnoreCase(tenantId, carbonIdentityId)
        .filter(profile -> !profile.getId().equals(currentProfileId))
        .ifPresent(profile -> {
          throw conflict("Carbon identity ID already exists for this tenant.");
        });
    return carbonIdentityId;
  }

  private CarbonParticipantType participantType(CarbonParticipantType participantType) {
    return participantType == null ? CarbonParticipantType.FARMER : participantType;
  }

  private CarbonRecordStatus statusOrActive(CarbonRecordStatus status) {
    return status == null ? CarbonRecordStatus.ACTIVE : status;
  }

  private String resolveDisplayName(
      String requestedDisplayName,
      UserEntity user,
      FpoMemberProfileEntity member
  ) {
    String normalized = CarbonProfileRules.normalizeOptionalText(requestedDisplayName);
    if (normalized != null) {
      return normalized;
    }

    if (member != null) {
      return member.getDisplayName();
    }

    if (user != null) {
      return user.getDisplayName();
    }

    throw validation("Display name is required when no user or FPO member is linked.");
  }

  private CarbonProfileEntity saveProfile(CarbonProfileEntity profile) {
    try {
      return profileRepository.saveAndFlush(profile);
    } catch (DataIntegrityViolationException exception) {
      throw conflict("Carbon profile identity must be unique per tenant.");
    }
  }

  private void auditProfile(
      CurrentUser currentUser,
      CarbonProfileEntity profile,
      AuditAction action
  ) {
    auditEventService.record(
        profile.getTenant(),
        actor(currentUser),
        "CARBON_PROFILE",
        profile.getId(),
        action,
        Map.of(
            "carbonIdentityId", profile.getCarbonIdentityId(),
            "status", profile.getStatus().name()
        )
    );
  }

  private void auditPlot(CurrentUser currentUser, CarbonFarmPlotEntity plot, AuditAction action) {
    auditEventService.record(
        plot.getTenant(),
        actor(currentUser),
        "CARBON_FARM_PLOT",
        plot.getId(),
        action,
        Map.of(
            "profileId", plot.getCarbonProfile().getId().toString(),
            "status", plot.getStatus().name()
        )
    );
  }

  private void auditSoilProfile(
      CurrentUser currentUser,
      CarbonSoilProfileEntity profile,
      AuditAction action
  ) {
    auditEventService.record(
        profile.getTenant(),
        actor(currentUser),
        "CARBON_SOIL_PROFILE",
        profile.getId(),
        action,
        Map.of(
            "profileId", profile.getCarbonProfile().getId().toString(),
            "status", profile.getStatus().name()
        )
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
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

  private ApplicationException accessDenied(String message) {
    return new ApplicationException(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
  }
}
