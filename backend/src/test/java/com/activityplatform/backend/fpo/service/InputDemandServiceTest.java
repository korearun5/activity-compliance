package com.activityplatform.backend.fpo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.fpo.api.CropInputRuleRequest;
import com.activityplatform.backend.fpo.api.InputCatalogRequest;
import com.activityplatform.backend.fpo.api.InputCatalogResponse;
import com.activityplatform.backend.fpo.api.InputDemandRunRequest;
import com.activityplatform.backend.fpo.api.InputDemandRunResponse;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropInputRuleEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropInputRuleRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.InputCatalogRepository;
import com.activityplatform.backend.fpo.repository.InputDemandEstimateRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
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

class InputDemandServiceTest {
  private final AuditEventService auditEventService = mock(AuditEventService.class);
  private final CropCatalogRepository cropRepository = mock(CropCatalogRepository.class);
  private final CropInputRuleRepository ruleRepository = mock(CropInputRuleRepository.class);
  private final CropSeasonRepository seasonRepository = mock(CropSeasonRepository.class);
  private final InputCatalogRepository inputRepository = mock(InputCatalogRepository.class);
  private final InputDemandEstimateRepository estimateRepository =
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
  private InputDemandService service;

  @BeforeEach
  void setUp() {
    service = new InputDemandService(
        auditEventService,
        cropRepository,
        ruleRepository,
        seasonRepository,
        inputRepository,
        estimateRepository,
        cropPlanRepository,
        tenantModuleService,
        tenantRepository,
        userRepository
    );
    when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
  }

  @Test
  void createInputNormalizesCodeAndUnitAndDefaultsStatus() {
    CurrentUser admin = currentUser(Role.ADMIN);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(inputRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "SEED"))
        .thenReturn(Optional.empty());
    when(inputRepository.save(any(InputCatalogEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InputCatalogResponse response = service.createInput(
        admin,
        new InputCatalogRequest(" seed ", " Seed ", " Fertilizer ", " kg ", null)
    );

    assertThat(response.code()).isEqualTo("SEED");
    assertThat(response.name()).isEqualTo("Seed");
    assertThat(response.category()).isEqualTo("Fertilizer");
    assertThat(response.unit()).isEqualTo("KG");
    assertThat(response.status()).isEqualTo(FarmRecordStatus.ACTIVE);
    verify(auditEventService).record(
        eq(tenant),
        eq(adminUser),
        eq("FPO_INPUT"),
        eq(response.id()),
        eq(AuditAction.FPO_INPUT_CREATED),
        eq(Map.of("code", "SEED", "unit", "KG", "status", "ACTIVE"))
    );
  }

  @Test
  void createRuleRejectsInactiveInput() {
    CurrentUser admin = currentUser(Role.ADMIN);
    CropCatalogEntity crop = crop(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    InputCatalogEntity input = input(UUID.randomUUID(), "NPK", FarmRecordStatus.INACTIVE);
    when(cropRepository.findByIdAndTenantId(crop.getId(), tenantId))
        .thenReturn(Optional.of(crop));
    when(inputRepository.findByIdAndTenantId(input.getId(), tenantId))
        .thenReturn(Optional.of(input));

    assertThatThrownBy(() -> service.createRule(
        admin,
        new CropInputRuleRequest(
            crop.getId(),
            input.getId(),
            new BigDecimal("2.5000"),
            "Basal",
            null,
            null
        )
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Input must be active");
    verify(ruleRepository, never()).save(any());
  }

  @Test
  void runDemandEstimateSumsMultipleStagesForSameInput() {
    CurrentUser admin = currentUser(Role.ADMIN);
    CropCatalogEntity crop = crop(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    CropSeasonEntity season = season(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    InputCatalogEntity input = input(UUID.randomUUID(), "NPK", FarmRecordStatus.ACTIVE);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID(), "Farmer"));
    SeasonalCropPlanEntity plan = plan(member, crop, season, new BigDecimal("2.0000"));
    CropInputRuleEntity basalRule = rule(crop, input, new BigDecimal("4.0000"), "Basal");
    CropInputRuleEntity topRule = rule(crop, input, new BigDecimal("1.5000"), "Top");
    when(seasonRepository.findByIdAndTenantId(season.getId(), tenantId))
        .thenReturn(Optional.of(season));
    when(cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(plan));
    when(ruleRepository.findByTenantIdAndCropIdAndStatus(
        tenantId,
        crop.getId(),
        FarmRecordStatus.ACTIVE
    ))
        .thenReturn(List.of(basalRule, topRule));
    when(estimateRepository.findByTenantIdAndCropPlanIdAndInputId(
        tenantId,
        plan.getId(),
        input.getId()
    ))
        .thenReturn(Optional.empty());
    when(estimateRepository.save(any(InputDemandEstimateEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InputDemandRunResponse response = service.runDemandEstimate(
        admin,
        new InputDemandRunRequest(season.getId(), null, null, null)
    );

    assertThat(response.plansConsidered()).isEqualTo(1);
    assertThat(response.missingRulePlanCount()).isZero();
    assertThat(response.estimatesGenerated()).isEqualTo(1);
    assertThat(response.estimates()).hasSize(1);
    assertThat(response.estimates().getFirst().estimatedQuantity())
        .isEqualByComparingTo("11.0000");
    verify(estimateRepository).save(any(InputDemandEstimateEntity.class));
    verify(auditEventService).record(
        eq(tenant),
        eq(adminUser),
        eq("FPO_INPUT_DEMAND"),
        eq(season.getId()),
        eq(AuditAction.FPO_INPUT_DEMAND_CALCULATED),
        eq(Map.of(
            "plansConsidered", 1,
            "estimatesGenerated", 1,
            "missingRulePlanCount", 0
        ))
    );
  }

  @Test
  void runDemandEstimateCountsPlansWithoutRules() {
    CurrentUser admin = currentUser(Role.ADMIN);
    CropCatalogEntity crop = crop(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    CropSeasonEntity season = season(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID(), "Farmer"));
    SeasonalCropPlanEntity plan = plan(member, crop, season, new BigDecimal("1.2500"));
    when(seasonRepository.findByIdAndTenantId(season.getId(), tenantId))
        .thenReturn(Optional.of(season));
    when(cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(plan));
    when(ruleRepository.findByTenantIdAndCropIdAndStatus(
        tenantId,
        crop.getId(),
        FarmRecordStatus.ACTIVE
    ))
        .thenReturn(List.of());

    InputDemandRunResponse response = service.runDemandEstimate(
        admin,
        new InputDemandRunRequest(season.getId(), null, null, null)
    );

    assertThat(response.plansConsidered()).isEqualTo(1);
    assertThat(response.missingRulePlanCount()).isEqualTo(1);
    assertThat(response.estimatesGenerated()).isZero();
    assertThat(response.estimates()).isEmpty();
    verify(estimateRepository, never()).save(any());
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

  private FpoMemberProfileEntity member(UUID memberId, UserEntity user) {
    return new FpoMemberProfileEntity(
        memberId,
        tenant,
        user,
        "MEM-" + memberId.toString().substring(0, 8),
        user.getDisplayName(),
        user.getPhone(),
        null,
        "Rampur",
        "North Block",
        "District",
        null,
        null,
        null,
        "SMALL",
        null,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropCatalogEntity crop(UUID cropId, FarmRecordStatus status) {
    return new CropCatalogEntity(
        cropId,
        tenant,
        "TOM",
        "Tomato",
        "Vegetable",
        status,
        Instant.now()
    );
  }

  private CropSeasonEntity season(UUID seasonId, FarmRecordStatus status) {
    return new CropSeasonEntity(
        seasonId,
        tenant,
        "KHA",
        "Kharif",
        6,
        9,
        2026,
        status,
        Instant.now()
    );
  }

  private InputCatalogEntity input(UUID inputId, String code, FarmRecordStatus status) {
    return new InputCatalogEntity(
        inputId,
        tenant,
        code,
        code + " Input",
        "Fertilizer",
        "KG",
        status,
        Instant.now()
    );
  }

  private CropInputRuleEntity rule(
      CropCatalogEntity crop,
      InputCatalogEntity input,
      BigDecimal quantityPerAcre,
      String stage
  ) {
    return new CropInputRuleEntity(
        UUID.randomUUID(),
        tenant,
        crop,
        input,
        quantityPerAcre,
        stage,
        null,
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private SeasonalCropPlanEntity plan(
      FpoMemberProfileEntity member,
      CropCatalogEntity crop,
      CropSeasonEntity season,
      BigDecimal plannedArea
  ) {
    return new SeasonalCropPlanEntity(
        UUID.randomUUID(),
        tenant,
        member,
        null,
        crop,
        season,
        plannedArea,
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 9, 30),
        CropPlanStatus.CONFIRMED,
        Instant.now()
    );
  }
}
