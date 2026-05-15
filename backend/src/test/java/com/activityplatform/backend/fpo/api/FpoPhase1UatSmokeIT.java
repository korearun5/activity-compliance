package com.activityplatform.backend.fpo.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.activityplatform.backend.TestDataFactory;
import com.activityplatform.backend.TestcontainersConfiguration;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.auth.service.JwtService;
import com.activityplatform.backend.fpo.domain.AdvisoryCategory;
import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import com.activityplatform.backend.fpo.domain.AdvisoryTargetType;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoAdvisoryEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateStatus;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FpoAdvisoryRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.InputCatalogRepository;
import com.activityplatform.backend.fpo.repository.InputDemandEstimateRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.notification.domain.NotificationChannel;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.domain.PlatformModuleEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionStatus;
import com.activityplatform.backend.platform.repository.PlatformModuleRepository;
import com.activityplatform.backend.platform.repository.TenantModuleSubscriptionRepository;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FpoPhase1UatSmokeIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FpoMemberProfileRepository memberRepository;

  @Autowired
  private FarmLandholdingRepository landholdingRepository;

  @Autowired
  private FarmPlotRepository plotRepository;

  @Autowired
  private CropCatalogRepository cropRepository;

  @Autowired
  private CropSeasonRepository seasonRepository;

  @Autowired
  private SeasonalCropPlanRepository cropPlanRepository;

  @Autowired
  private InputCatalogRepository inputRepository;

  @Autowired
  private InputDemandEstimateRepository demandEstimateRepository;

  @Autowired
  private FpoAdvisoryRepository advisoryRepository;

  @Autowired
  private PlatformModuleRepository platformModuleRepository;

  @Autowired
  private TenantModuleSubscriptionRepository subscriptionRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private TenantEntity tenant;
  private UserEntity adminUser;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("uat-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));
    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "UAT Admin",
        adminRole
    ));
    enablePhase1Modules();
    seedPilotData(farmerRole);
    adminToken = jwtService.issueTokens(adminUser).accessToken();
  }

  @Test
  void testPhase1UatSmokeEndpointsReturnOkForPilotData() throws Exception {
    for (String path : List.of(
        "/api/v1/platform/modules/enabled",
        "/api/v1/fpo/members",
        "/api/v1/fpo/crops",
        "/api/v1/fpo/seasons",
        "/api/v1/fpo/crop-plans",
        "/api/v1/fpo/demand-estimates/summary",
        "/api/v1/fpo/advisories",
        "/api/v1/fpo/reports/summary"
    )) {
      mockMvc.perform(get(path).header("Authorization", "Bearer " + adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    mockMvc.perform(get("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(jsonPath("$.data.content.length()").value(5))
        .andExpect(jsonPath("$.data.content[0].village").value("Wagholi"));
    mockMvc.perform(get("/api/v1/fpo/reports/summary")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(jsonPath("$.data.totalMembers").value(5))
        .andExpect(jsonPath("$.data.confirmedCropPlanCount").value(1))
        .andExpect(jsonPath("$.data.demandEstimateCount").value(1));
  }

  @Test
  void testPhase1UatSmokeEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/fpo/members"))
        .andExpect(status().isUnauthorized());
  }

  private void seedPilotData(RoleEntity farmerRole) {
    List<FpoMemberProfileEntity> farmers = seedFarmers(farmerRole);
    CropCatalogEntity paddy = cropRepository.save(crop("PAD", "Paddy"));
    cropRepository.save(crop("WHT", "Wheat"));
    CropSeasonEntity kharif = seasonRepository.save(season("KHA", "Kharif", 2026, 6, 10));
    seasonRepository.save(season("RAB", "Rabi", 2026, 11, 2));
    FarmLandholdingEntity landholding = landholdingRepository.save(landholding(farmers.get(0)));
    plotRepository.save(plot(farmers.get(0), landholding));
    SeasonalCropPlanEntity plan = cropPlanRepository.save(cropPlan(farmers.get(0), paddy, kharif));
    InputCatalogEntity urea = inputRepository.save(input("UREA", "Urea"));
    demandEstimateRepository.save(demandEstimate(plan, urea));
    advisoryRepository.save(advisory(kharif));
  }

  private List<FpoMemberProfileEntity> seedFarmers(RoleEntity farmerRole) {
    return List.of(
        farmer("001", "Marginal", farmerRole),
        farmer("002", "Small", farmerRole),
        farmer("003", "Semi-medium", farmerRole),
        farmer("004", "Medium", farmerRole),
        farmer("005", "Large", farmerRole)
    );
  }

  private FpoMemberProfileEntity farmer(
      String suffix,
      String category,
      RoleEntity farmerRole
  ) {
    UserEntity user = userRepository.save(TestDataFactory.user(
        tenant,
        "uat-farmer-" + suffix + "-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "UAT Farmer " + suffix,
        farmerRole
    ));
    return memberRepository.save(new FpoMemberProfileEntity(
        UUID.randomUUID(),
        tenant,
        user,
        "UAT-" + suffix,
        "UAT Farmer " + suffix,
        "9000000" + suffix,
        null,
        null,
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        null,
        42,
        category.toUpperCase().replace('-', '_'),
        adminUser,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    ));
  }

  private CropCatalogEntity crop(String code, String name) {
    return new CropCatalogEntity(
        UUID.randomUUID(),
        tenant,
        code,
        name,
        "Cereals",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropSeasonEntity season(
      String code,
      String name,
      Integer seasonYear,
      Integer startMonth,
      Integer endMonth
  ) {
    return new CropSeasonEntity(
        UUID.randomUUID(),
        tenant,
        code,
        name,
        startMonth,
        endMonth,
        seasonYear,
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private FarmLandholdingEntity landholding(FpoMemberProfileEntity farmer) {
    return new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        farmer,
        "SUR-UAT-001",
        new BigDecimal("2.0000"),
        new BigDecimal("2.0000"),
        "Self-owned",
        "Canal",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private FarmPlotEntity plot(
      FpoMemberProfileEntity farmer,
      FarmLandholdingEntity landholding
  ) {
    return new FarmPlotEntity(
        UUID.randomUUID(),
        tenant,
        farmer,
        landholding,
        "Wagholi North",
        new BigDecimal("1.5000"),
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        "BLACK_SOIL",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private SeasonalCropPlanEntity cropPlan(
      FpoMemberProfileEntity farmer,
      CropCatalogEntity crop,
      CropSeasonEntity season
  ) {
    return new SeasonalCropPlanEntity(
        UUID.randomUUID(),
        tenant,
        farmer,
        null,
        crop,
        season,
        "2026-27",
        new BigDecimal("1.5000"),
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 10, 15),
        new BigDecimal("45.0000"),
        CropPlanStatus.CONFIRMED,
        Instant.now()
    );
  }

  private InputCatalogEntity input(String code, String name) {
    return new InputCatalogEntity(
        UUID.randomUUID(),
        tenant,
        code,
        name,
        "Fertilizer",
        "KG",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private InputDemandEstimateEntity demandEstimate(
      SeasonalCropPlanEntity plan,
      InputCatalogEntity input
  ) {
    return new InputDemandEstimateEntity(
        UUID.randomUUID(),
        tenant,
        plan,
        input,
        new BigDecimal("30.0000"),
        new BigDecimal("20.0000"),
        new BigDecimal("30.0000"),
        new BigDecimal("5.00"),
        new BigDecimal("1.5000"),
        new BigDecimal("32.0000"),
        input.getUnit(),
        InputDemandEstimateStatus.ESTIMATED,
        Instant.now()
    );
  }

  private FpoAdvisoryEntity advisory(CropSeasonEntity season) {
    return new FpoAdvisoryEntity(
        UUID.randomUUID(),
        tenant,
        null,
        season,
        AdvisoryTargetType.ALL_MEMBERS,
        AdvisoryCategory.AGRONOMY,
        "Kharif field advisory",
        "Scout fields weekly during the first month after sowing.",
        NotificationChannel.IN_APP,
        AdvisoryStatus.PUBLISHED,
        adminUser,
        Instant.now()
    );
  }

  private void enablePhase1Modules() {
    for (ModuleCode moduleCode : List.of(
        ModuleCode.MEMBER_DATA,
        ModuleCode.LAND_RECORDS,
        ModuleCode.CROP_PLANNING,
        ModuleCode.INPUT_DEMAND,
        ModuleCode.ADVISORY,
        ModuleCode.REPORT_EXPORT
    )) {
      PlatformModuleEntity module = platformModuleRepository.findByCode(moduleCode)
          .orElseThrow();
      Instant now = Instant.now();
      subscriptionRepository.save(new TenantModuleSubscriptionEntity(
          UUID.randomUUID(),
          tenant,
          module,
          TenantModuleSubscriptionStatus.ENABLED,
          now,
          null,
          null,
          now
      ));
    }
  }
}
