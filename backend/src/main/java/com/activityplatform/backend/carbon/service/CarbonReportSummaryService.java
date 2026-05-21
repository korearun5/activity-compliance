package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.carbon.api.CarbonActivityReportBreakdownResponse;
import com.activityplatform.backend.carbon.api.CarbonReportBreakdownResponse;
import com.activityplatform.backend.carbon.api.CarbonReportSummaryResponse;
import com.activityplatform.backend.carbon.domain.CarbonActivityRecordEntity;
import com.activityplatform.backend.carbon.domain.CarbonActivityVerificationStatus;
import com.activityplatform.backend.carbon.domain.CarbonFarmPlotEntity;
import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonVerificationStatus;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarbonReportSummaryService {
  private final AuditEventService auditEventService;
  private final CarbonReportDatasetService datasetService;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public CarbonReportSummaryService(
      AuditEventService auditEventService,
      CarbonReportDatasetService datasetService,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.datasetService = datasetService;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public CarbonReportSummaryResponse summary(CurrentUser currentUser) {
    requireCarbonAccess(currentUser);
    TenantEntity tenant = requireTenant(currentUser.tenantId());
    CarbonReportSummaryResponse response = buildSummary(
        currentUser.tenantId(),
        datasetService.load(currentUser)
    );

    auditEventService.record(
        tenant,
        actor(currentUser),
        "CARBON_REPORT",
        tenant.getId(),
        AuditAction.REPORT_SUMMARY_VIEWED,
        Map.of(
            "totalProfiles", response.totalProfiles(),
            "soilProfileCount", response.soilProfileCount(),
            "activityCount", response.activityCount()
        )
    );

    return response;
  }

  CarbonReportSummaryResponse buildSummary(UUID tenantId, CarbonReportDataset dataset) {
    List<CarbonProfileEntity> activeProfiles = dataset.profiles().stream()
        .filter(profile -> profile.getStatus() == CarbonRecordStatus.ACTIVE)
        .toList();
    List<CarbonFarmPlotEntity> activePlots = dataset.plots().stream()
        .filter(plot -> plot.getStatus() == CarbonRecordStatus.ACTIVE)
        .toList();
    List<CarbonSoilProfileEntity> verifiedSoilProfiles = dataset.soilProfiles().stream()
        .filter(soilProfile ->
            soilProfile.getVerificationStatus() == CarbonVerificationStatus.VERIFIED)
        .toList();
    List<CarbonSoilProfileEntity> pendingSoilProfiles = dataset.soilProfiles().stream()
        .filter(soilProfile ->
            soilProfile.getVerificationStatus() == CarbonVerificationStatus.PENDING_VERIFICATION)
        .toList();
    List<CarbonActivityRecordEntity> verifiedActivities = dataset.activities().stream()
        .filter(activity ->
            activity.getVerificationStatus() == CarbonActivityVerificationStatus.VERIFIED)
        .toList();

    return new CarbonReportSummaryResponse(
        tenantId,
        dataset.profiles().size(),
        activeProfiles.size(),
        dataset.profiles().stream().filter(profile -> profile.getUser() != null).count(),
        sumProfileArea(dataset.profiles()),
        dataset.plots().size(),
        activePlots.size(),
        sumPlotArea(dataset.plots()),
        dataset.soilProfiles().size(),
        verifiedSoilProfiles.size(),
        pendingSoilProfiles.size(),
        averageSoc(dataset.soilProfiles()),
        dataset.activities().size(),
        verifiedActivities.size(),
        dataset.activities().size() - verifiedActivities.size(),
        dataset.activities().stream().mapToLong(CarbonActivityRecordEntity::getEvidenceCount).sum(),
        villageBreakdowns(dataset),
        activityBreakdowns(dataset.activities())
    );
  }

  private List<CarbonReportBreakdownResponse> villageBreakdowns(
      CarbonReportDataset dataset
  ) {
    Map<UUID, List<CarbonFarmPlotEntity>> plotsByProfile = dataset.plots().stream()
        .collect(Collectors.groupingBy(plot -> plot.getCarbonProfile().getId()));
    Map<UUID, List<CarbonSoilProfileEntity>> soilByProfile = dataset.soilProfiles().stream()
        .collect(Collectors.groupingBy(soil -> soil.getCarbonProfile().getId()));
    Map<UUID, List<CarbonActivityRecordEntity>> activitiesByProfile = dataset.activities().stream()
        .collect(Collectors.groupingBy(activity -> activity.getCarbonProfile().getId()));

    return dataset.profiles().stream()
        .collect(Collectors.groupingBy(
            profile -> label(profile.getVillage(), "Unassigned village"),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .entrySet()
        .stream()
        .map(entry -> villageBreakdown(
            entry.getKey(),
            entry.getValue(),
            plotsByProfile,
            soilByProfile,
            activitiesByProfile
        ))
        .sorted(Comparator.comparing(CarbonReportBreakdownResponse::label))
        .toList();
  }

  private CarbonReportBreakdownResponse villageBreakdown(
      String village,
      List<CarbonProfileEntity> profiles,
      Map<UUID, List<CarbonFarmPlotEntity>> plotsByProfile,
      Map<UUID, List<CarbonSoilProfileEntity>> soilByProfile,
      Map<UUID, List<CarbonActivityRecordEntity>> activitiesByProfile
  ) {
    List<UUID> profileIds = profiles.stream().map(CarbonProfileEntity::getId).toList();
    List<CarbonFarmPlotEntity> plots = profileIds.stream()
        .flatMap(profileId -> plotsByProfile.getOrDefault(profileId, List.of()).stream())
        .toList();
    List<CarbonSoilProfileEntity> soilProfiles = profileIds.stream()
        .flatMap(profileId -> soilByProfile.getOrDefault(profileId, List.of()).stream())
        .toList();
    List<CarbonActivityRecordEntity> activities = profileIds.stream()
        .flatMap(profileId -> activitiesByProfile.getOrDefault(profileId, List.of()).stream())
        .toList();

    long pendingSoil = soilProfiles.stream()
        .filter(soil -> soil.getVerificationStatus() == CarbonVerificationStatus.PENDING_VERIFICATION)
        .count();
    long pendingActivities = activities.stream()
        .filter(activity ->
            activity.getVerificationStatus() != CarbonActivityVerificationStatus.VERIFIED)
        .count();

    return new CarbonReportBreakdownResponse(
        village,
        profiles.size(),
        plots.size(),
        sumPlotArea(plots),
        soilProfiles.size(),
        activities.size(),
        pendingSoil + pendingActivities
    );
  }

  private List<CarbonActivityReportBreakdownResponse> activityBreakdowns(
      List<CarbonActivityRecordEntity> activities
  ) {
    return activities.stream()
        .collect(Collectors.groupingBy(
            activity -> new ActivityGroup(
                activity.getCategory().getCode(),
                activity.getCategory().getName()
            ),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .entrySet()
        .stream()
        .map(entry -> activityBreakdown(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(CarbonActivityReportBreakdownResponse::categoryName))
        .toList();
  }

  private CarbonActivityReportBreakdownResponse activityBreakdown(
      ActivityGroup group,
      List<CarbonActivityRecordEntity> activities
  ) {
    long verifiedCount = activities.stream()
        .filter(activity ->
            activity.getVerificationStatus() == CarbonActivityVerificationStatus.VERIFIED)
        .count();

    return new CarbonActivityReportBreakdownResponse(
        group.code(),
        group.name(),
        activities.size(),
        verifiedCount,
        activities.size() - verifiedCount,
        activities.stream().mapToLong(CarbonActivityRecordEntity::getEvidenceCount).sum()
    );
  }

  private BigDecimal sumProfileArea(List<CarbonProfileEntity> profiles) {
    return profiles.stream()
        .map(CarbonProfileEntity::getTotalLandHoldingAcres)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal sumPlotArea(List<CarbonFarmPlotEntity> plots) {
    return plots.stream()
        .map(CarbonFarmPlotEntity::getAreaAcres)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal averageSoc(List<CarbonSoilProfileEntity> soilProfiles) {
    List<BigDecimal> values = soilProfiles.stream()
        .map(CarbonSoilProfileEntity::getSoilOrganicCarbonPercent)
        .filter(Objects::nonNull)
        .toList();

    if (values.isEmpty()) {
      return BigDecimal.ZERO;
    }

    return values.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
  }

  private void requireCarbonAccess(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    CarbonAccessPolicy.requireCarbonStaff(
        currentUser,
        "Only Carbon staff can view Carbon report summaries."
    );
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> new ApplicationException(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Tenant not found.",
            HttpStatus.NOT_FOUND
        ));
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private String label(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private record ActivityGroup(String code, String name) {
  }
}
