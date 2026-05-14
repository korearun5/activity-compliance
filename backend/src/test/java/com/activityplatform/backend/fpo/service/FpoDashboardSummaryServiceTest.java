package com.activityplatform.backend.fpo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.fpo.api.FpoDashboardSummaryResponse;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateStatus;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FpoDashboardSummaryServiceTest {
  private final AuditEventService auditEventService = mock(AuditEventService.class);
  private final FarmLandholdingRepository landholdingRepository =
      mock(FarmLandholdingRepository.class);
  private final FarmPlotRepository plotRepository = mock(FarmPlotRepository.class);
  private final FpoMemberProfileRepository memberRepository =
      mock(FpoMemberProfileRepository.class);
  private final InputDemandEstimateRepository demandEstimateRepository =
      mock(InputDemandEstimateRepository.class);
  private final SeasonalCropPlanRepository cropPlanRepository =
      mock(SeasonalCropPlanRepository.class);
  private final TenantModuleService tenantModuleService = mock(TenantModuleService.class);
  private final TenantRepository tenantRepository = mock(TenantRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);

  private final UUID tenantId = UUID.randomUUID();
  private final UUID adminUserId = UUID.randomUUID();
  private final TenantEntity tenant = new TenantEntity(
      tenantId,
      "basecraft-fpo",
      "BaseCraft FPO",
      "ACTIVE",
      Instant.now()
  );
  private final UserEntity adminUser = user(adminUserId, "Admin User");
  private FpoDashboardSummaryService service;

  @BeforeEach
  void setUp() {
    service = new FpoDashboardSummaryService(
        auditEventService,
        landholdingRepository,
        plotRepository,
        memberRepository,
        demandEstimateRepository,
        cropPlanRepository,
        tenantModuleService,
        tenantRepository,
        userRepository
    );
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
  }

  @Test
  void summaryAggregatesMemberLandPlotPlanAndDemandMetrics() {
    CurrentUser admin = currentUser(Role.ADMIN);
    FpoMemberProfileEntity activeMember = member(
        UUID.randomUUID(),
        user(UUID.randomUUID(), "Farmer One"),
        "Rampur",
        FpoMemberStatus.ACTIVE
    );
    FpoMemberProfileEntity inactiveMember = member(
        UUID.randomUUID(),
        user(UUID.randomUUID(), "Farmer Two"),
        "Rampur",
        FpoMemberStatus.INACTIVE
    );
    FarmLandholdingEntity activeLandholding = landholding(
        activeMember,
        new BigDecimal("3.0000"),
        new BigDecimal("2.5000"),
        FarmRecordStatus.ACTIVE
    );
    FarmLandholdingEntity archivedLandholding = landholding(
        inactiveMember,
        new BigDecimal("5.0000"),
        new BigDecimal("4.0000"),
        FarmRecordStatus.ARCHIVED
    );
    FarmPlotEntity geoTaggedPlot = plot(
        activeMember,
        new BigDecimal("1.2500"),
        new BigDecimal("19.8765432"),
        new BigDecimal("73.1234567"),
        FarmRecordStatus.ACTIVE
    );
    FarmPlotEntity secondGeoTaggedPlot = plot(
        activeMember,
        new BigDecimal("0.7500"),
        new BigDecimal("19.8765000"),
        new BigDecimal("73.1234000"),
        FarmRecordStatus.ACTIVE
    );
    FarmPlotEntity archivedPlot = plot(
        inactiveMember,
        new BigDecimal("2.0000"),
        new BigDecimal("18.5204000"),
        new BigDecimal("73.8567000"),
        FarmRecordStatus.ARCHIVED
    );
    CropCatalogEntity onion = crop(UUID.randomUUID(), "ONI", "Onion");
    CropCatalogEntity tomato = crop(UUID.randomUUID(), "TOM", "Tomato");
    CropSeasonEntity season = season(UUID.randomUUID(), "KHA", "Kharif");
    SeasonalCropPlanEntity onionPlan = plan(
        activeMember,
        onion,
        season,
        new BigDecimal("1.5000"),
        CropPlanStatus.CONFIRMED
    );
    SeasonalCropPlanEntity tomatoPlan = plan(
        activeMember,
        tomato,
        season,
        new BigDecimal("2.0000"),
        CropPlanStatus.CONFIRMED
    );
    SeasonalCropPlanEntity draftPlan = plan(
        inactiveMember,
        tomato,
        season,
        new BigDecimal("9.0000"),
        CropPlanStatus.DRAFT
    );
    InputCatalogEntity input = input(UUID.randomUUID(), "NPK", "NPK 19");
    InputDemandEstimateEntity onionDemand = estimate(
        onionPlan,
        input,
        new BigDecimal("15.0000")
    );
    InputDemandEstimateEntity tomatoDemand = estimate(
        tomatoPlan,
        input,
        new BigDecimal("20.0000")
    );
    when(memberRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(activeMember, inactiveMember));
    when(landholdingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(activeLandholding, archivedLandholding));
    when(plotRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(geoTaggedPlot, secondGeoTaggedPlot, archivedPlot));
    when(cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(onionPlan, tomatoPlan, draftPlan));
    when(demandEstimateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(onionDemand, tomatoDemand));

    FpoDashboardSummaryResponse response = service.summary(admin);

    assertThat(response.totalMembers()).isEqualTo(2);
    assertThat(response.activeMembers()).isEqualTo(1);
    assertThat(response.totalLandholdings()).isEqualTo(2);
    assertThat(response.activeLandholdings()).isEqualTo(1);
    assertThat(response.totalLandAreaAcres()).isEqualByComparingTo("8.0000");
    assertThat(response.activeLandAreaAcres()).isEqualByComparingTo("3.0000");
    assertThat(response.totalCultivableAreaAcres()).isEqualByComparingTo("2.5000");
    assertThat(response.totalPlots()).isEqualTo(3);
    assertThat(response.activePlots()).isEqualTo(2);
    assertThat(response.geoTaggedPlots()).isEqualTo(2);
    assertThat(response.totalPlotAreaAcres()).isEqualByComparingTo("4.0000");
    assertThat(response.activePlotAreaAcres()).isEqualByComparingTo("2.0000");
    assertThat(response.cropPlanCount()).isEqualTo(3);
    assertThat(response.confirmedCropPlanCount()).isEqualTo(2);
    assertThat(response.confirmedPlannedAreaAcres()).isEqualByComparingTo("3.5000");
    assertThat(response.demandEstimateCount()).isEqualTo(2);
    assertThat(response.cropPlanAreaByCrop())
        .extracting(item -> item.label())
        .containsExactly("Onion", "Tomato");
    assertThat(response.cropPlanAreaByVillage().getFirst().areaAcres())
        .isEqualByComparingTo("3.5000");
    assertThat(response.inputDemandByInput().getFirst().estimatedQuantity())
        .isEqualByComparingTo("35.0000");
    assertThat(response.inputDemandByInput().getFirst().memberCount()).isEqualTo(1);
    verify(tenantModuleService).requireEnabled(tenantId, ModuleCode.REPORT_EXPORT);
    verify(auditEventService).record(
        eq(tenant),
        eq(adminUser),
        eq("FPO_REPORT"),
        eq(tenantId),
        eq(AuditAction.FPO_REPORT_SUMMARY_VIEWED),
        eq(Map.of(
            "totalMembers", 2L,
            "confirmedCropPlanCount", 2L,
            "demandEstimateCount", 2L
        ))
    );
  }

  @Test
  void summaryAllowsFIELD_COORDINATORRoleForPhaseOneAssignedScope() {
    CurrentUser FIELD_COORDINATOR = currentUser(Role.FIELD_COORDINATOR);
    when(memberRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of());
    when(landholdingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of());
    when(plotRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of());
    when(cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of());
    when(demandEstimateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of());

    assertThat(service.summary(FIELD_COORDINATOR).totalMembers()).isZero();
  }

  private CurrentUser currentUser(Role role) {
    return new CurrentUser(adminUserId, tenantId, role.name().toLowerCase(), Set.of(role));
  }

  private UserEntity user(UUID userId, String displayName) {
    return new UserEntity(
        userId,
        tenant,
        "user-" + userId,
        "hash",
        displayName,
        "+919000000000",
        "Rampur",
        "North Block",
        "ACTIVE",
        Instant.now()
    );
  }

  private FpoMemberProfileEntity member(
      UUID memberId,
      UserEntity user,
      String village,
      FpoMemberStatus status
  ) {
    return new FpoMemberProfileEntity(
        memberId,
        tenant,
        user,
        "MEM-" + memberId.toString().substring(0, 8),
        user.getDisplayName(),
        user.getPhone(),
        null,
        null,
        village,
        "North Block",
        "District",
        "Maharashtra",
        null,
        null,
        null,
        "SMALL",
        null,
        status,
        Instant.now()
    );
  }

  private FarmLandholdingEntity landholding(
      FpoMemberProfileEntity member,
      BigDecimal totalArea,
      BigDecimal cultivableArea,
      FarmRecordStatus status
  ) {
    return new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        member,
        "SUR-1",
        totalArea,
        cultivableArea,
        "Self-owned",
        "Canal",
        status,
        Instant.now()
    );
  }

  private FarmPlotEntity plot(
      FpoMemberProfileEntity member,
      BigDecimal area,
      BigDecimal latitude,
      BigDecimal longitude,
      FarmRecordStatus status
  ) {
    return new FarmPlotEntity(
        UUID.randomUUID(),
        tenant,
        member,
        null,
        "North plot",
        area,
        latitude,
        longitude,
        "LOAM",
        status,
        Instant.now()
    );
  }

  private CropCatalogEntity crop(UUID cropId, String code, String name) {
    return new CropCatalogEntity(
        cropId,
        tenant,
        code,
        name,
        "Vegetable",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropSeasonEntity season(UUID seasonId, String code, String name) {
    return new CropSeasonEntity(
        seasonId,
        tenant,
        code,
        name,
        6,
        9,
        2026,
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private SeasonalCropPlanEntity plan(
      FpoMemberProfileEntity member,
      CropCatalogEntity crop,
      CropSeasonEntity season,
      BigDecimal plannedArea,
      CropPlanStatus status
  ) {
    return new SeasonalCropPlanEntity(
        UUID.randomUUID(),
        tenant,
        member,
        null,
        crop,
        season,
        "2026-27",
        plannedArea,
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 9, 30),
        null,
        status,
        Instant.now()
    );
  }

  private InputCatalogEntity input(UUID inputId, String code, String name) {
    return new InputCatalogEntity(
        inputId,
        tenant,
        code,
        name,
        "Fertilizer",
        "KG",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private InputDemandEstimateEntity estimate(
      SeasonalCropPlanEntity plan,
      InputCatalogEntity input,
      BigDecimal quantity
  ) {
    return new InputDemandEstimateEntity(
        UUID.randomUUID(),
        tenant,
        plan,
        input,
        quantity,
        input.getUnit(),
        InputDemandEstimateStatus.ESTIMATED,
        Instant.now()
    );
  }
}
