package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.api.FpoAreaBreakdownResponse;
import com.activityplatform.backend.fpo.api.FpoDashboardSummaryResponse;
import com.activityplatform.backend.fpo.api.FpoInputDemandBreakdownResponse;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.InputDemandEstimateRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FpoDashboardSummaryService {
  private final AuditEventService auditEventService;
  private final FarmLandholdingRepository landholdingRepository;
  private final FarmPlotRepository plotRepository;
  private final FpoMemberProfileRepository memberRepository;
  private final InputDemandEstimateRepository demandEstimateRepository;
  private final SeasonalCropPlanRepository cropPlanRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public FpoDashboardSummaryService(
      AuditEventService auditEventService,
      FarmLandholdingRepository landholdingRepository,
      FarmPlotRepository plotRepository,
      FpoMemberProfileRepository memberRepository,
      InputDemandEstimateRepository demandEstimateRepository,
      SeasonalCropPlanRepository cropPlanRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.landholdingRepository = landholdingRepository;
    this.plotRepository = plotRepository;
    this.memberRepository = memberRepository;
    this.demandEstimateRepository = demandEstimateRepository;
    this.cropPlanRepository = cropPlanRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public FpoDashboardSummaryResponse summary(CurrentUser currentUser) {
    requireReportModule(currentUser);
    requireManager(currentUser);
    TenantEntity tenant = requireTenant(currentUser.tenantId());
    List<FpoMemberProfileEntity> members = memberRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId());
    List<FarmLandholdingEntity> landholdings = landholdingRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId());
    List<FarmPlotEntity> plots = plotRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId());
    List<SeasonalCropPlanEntity> cropPlans = cropPlanRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId());
    List<InputDemandEstimateEntity> demandEstimates = demandEstimateRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId());
    FpoDashboardSummaryResponse response = buildSummary(
        tenant.getId(),
        members,
        landholdings,
        plots,
        cropPlans,
        demandEstimates
    );

    auditEventService.record(
        tenant,
        actor(currentUser),
        "FPO_REPORT",
        tenant.getId(),
        AuditAction.FPO_REPORT_SUMMARY_VIEWED,
        Map.of(
            "totalMembers", response.totalMembers(),
            "confirmedCropPlanCount", response.confirmedCropPlanCount(),
            "demandEstimateCount", response.demandEstimateCount()
        )
    );

    return response;
  }

  FpoDashboardSummaryResponse buildSummary(
      UUID tenantId,
      List<FpoMemberProfileEntity> members,
      List<FarmLandholdingEntity> landholdings,
      List<FarmPlotEntity> plots,
      List<SeasonalCropPlanEntity> cropPlans,
      List<InputDemandEstimateEntity> demandEstimates
  ) {
    List<FarmLandholdingEntity> activeLandholdings = landholdings.stream()
        .filter(landholding -> landholding.getStatus() == FarmRecordStatus.ACTIVE)
        .toList();
    List<FarmPlotEntity> activePlots = plots.stream()
        .filter(plot -> plot.getStatus() == FarmRecordStatus.ACTIVE)
        .toList();
    List<SeasonalCropPlanEntity> confirmedPlans = cropPlans.stream()
        .filter(plan -> plan.getStatus() == CropPlanStatus.CONFIRMED)
        .toList();

    return new FpoDashboardSummaryResponse(
        tenantId,
        members.size(),
        countActiveMembers(members),
        landholdings.size(),
        activeLandholdings.size(),
        sumLandArea(landholdings),
        sumLandArea(activeLandholdings),
        sumCultivableArea(activeLandholdings),
        plots.size(),
        activePlots.size(),
        countGeoTaggedPlots(activePlots),
        sumPlotArea(plots),
        sumPlotArea(activePlots),
        cropPlans.size(),
        confirmedPlans.size(),
        sumPlanArea(confirmedPlans),
        demandEstimates.size(),
        cropBreakdowns(confirmedPlans),
        seasonBreakdowns(confirmedPlans),
        villageBreakdowns(confirmedPlans),
        inputDemandBreakdowns(demandEstimates)
    );
  }

  private void requireReportModule(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.REPORT_EXPORT);
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins and supervisors can view FPO dashboard summaries.",
          HttpStatus.FORBIDDEN
      );
    }
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

  private long countActiveMembers(List<FpoMemberProfileEntity> members) {
    return members.stream()
        .filter(member -> member.getStatus() == FpoMemberStatus.ACTIVE)
        .count();
  }

  private long countGeoTaggedPlots(List<FarmPlotEntity> plots) {
    return plots.stream()
        .filter(plot -> plot.getLatitude() != null && plot.getLongitude() != null)
        .count();
  }

  private List<FpoAreaBreakdownResponse> cropBreakdowns(
      List<SeasonalCropPlanEntity> plans
  ) {
    return areaBreakdowns(
        plans,
        plan -> plan.getCrop().getId(),
        plan -> plan.getCrop().getName()
    );
  }

  private List<FpoAreaBreakdownResponse> seasonBreakdowns(
      List<SeasonalCropPlanEntity> plans
  ) {
    return areaBreakdowns(
        plans,
        plan -> plan.getSeason().getId(),
        plan -> plan.getSeason().getName() + " " + plan.getSeason().getSeasonYear()
    );
  }

  private List<FpoAreaBreakdownResponse> villageBreakdowns(
      List<SeasonalCropPlanEntity> plans
  ) {
    return areaBreakdowns(
        plans,
        plan -> null,
        plan -> normalizeLabel(plan.getMemberProfile().getVillage(), "Unassigned village")
    );
  }

  private List<FpoAreaBreakdownResponse> areaBreakdowns(
      List<SeasonalCropPlanEntity> plans,
      Function<SeasonalCropPlanEntity, UUID> idResolver,
      Function<SeasonalCropPlanEntity, String> labelResolver
  ) {
    return plans.stream()
        .collect(Collectors.groupingBy(
            plan -> new AreaKey(idResolver.apply(plan), labelResolver.apply(plan)),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .entrySet()
        .stream()
        .map(entry -> {
          List<SeasonalCropPlanEntity> groupedPlans = entry.getValue();
          Set<UUID> memberIds = groupedPlans.stream()
              .map(plan -> plan.getMemberProfile().getId())
              .collect(Collectors.toSet());

          return new FpoAreaBreakdownResponse(
              entry.getKey().id(),
              entry.getKey().label(),
              sumPlanArea(groupedPlans),
              groupedPlans.size(),
              memberIds.size()
          );
        })
        .sorted(Comparator.comparing(FpoAreaBreakdownResponse::label))
        .toList();
  }

  private List<FpoInputDemandBreakdownResponse> inputDemandBreakdowns(
      List<InputDemandEstimateEntity> estimates
  ) {
    return estimates.stream()
        .collect(Collectors.groupingBy(
            estimate -> estimate.getInput().getId(),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .values()
        .stream()
        .map(group -> {
          InputDemandEstimateEntity first = group.getFirst();
          Set<UUID> planIds = group.stream()
              .map(estimate -> estimate.getCropPlan().getId())
              .collect(Collectors.toSet());
          Set<UUID> memberIds = group.stream()
              .map(estimate -> estimate.getCropPlan().getMemberProfile().getId())
              .collect(Collectors.toSet());

          return new FpoInputDemandBreakdownResponse(
              first.getInput().getId(),
              first.getInput().getCode(),
              first.getInput().getName(),
              first.getUnit(),
              group.stream()
                  .map(InputDemandEstimateEntity::getEstimatedQuantity)
                  .filter(Objects::nonNull)
                  .reduce(BigDecimal.ZERO, BigDecimal::add),
              planIds.size(),
              memberIds.size()
          );
        })
        .sorted(Comparator.comparing(FpoInputDemandBreakdownResponse::inputName))
        .toList();
  }

  private BigDecimal sumLandArea(List<FarmLandholdingEntity> landholdings) {
    return landholdings.stream()
        .map(FarmLandholdingEntity::getTotalAreaAcres)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal sumCultivableArea(List<FarmLandholdingEntity> landholdings) {
    return landholdings.stream()
        .map(FarmLandholdingEntity::getCultivableAreaAcres)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal sumPlotArea(List<FarmPlotEntity> plots) {
    return plots.stream()
        .map(FarmPlotEntity::getAreaAcres)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal sumPlanArea(List<SeasonalCropPlanEntity> plans) {
    return plans.stream()
        .map(SeasonalCropPlanEntity::getPlannedAreaAcres)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private String normalizeLabel(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private record AreaKey(UUID id, String label) {
  }
}
