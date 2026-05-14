package com.activityplatform.backend.fpo.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.InputCatalogRepository;
import com.activityplatform.backend.fpo.repository.InputDemandEstimateRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.domain.PlatformModuleEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionStatus;
import com.activityplatform.backend.platform.repository.PlatformModuleRepository;
import com.activityplatform.backend.platform.repository.TenantModuleSubscriptionRepository;
import com.activityplatform.backend.reporting.domain.ReportExportEntity;
import com.activityplatform.backend.reporting.domain.ReportFormat;
import com.activityplatform.backend.reporting.repository.ReportExportRepository;
import com.activityplatform.backend.security.Role;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipFile;
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
class FpoReportControllerIT {
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
  private InputCatalogRepository inputRepository;

  @Autowired
  private InputDemandEstimateRepository demandEstimateRepository;

  @Autowired
  private ReportExportRepository reportExportRepository;

  @Autowired
  private SeasonalCropPlanRepository cropPlanRepository;

  @Autowired
  private PlatformModuleRepository platformModuleRepository;

  @Autowired
  private TenantModuleSubscriptionRepository subscriptionRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String disabledTenantAdminToken;
  private TenantEntity tenant;
  private UserEntity adminUser;
  private FpoMemberProfileEntity member;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );
    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Admin User",
        adminRole
    ));
    UserEntity FIELD_COORDINATORUser = userRepository.save(TestDataFactory.user(
        tenant,
        "FIELD_COORDINATOR-" + UUID.randomUUID(),
        passwordEncoder.encode("FIELD_COORDINATOR123"),
        "FIELD_COORDINATOR User",
        FIELD_COORDINATORRole
    ));
    member = memberRepository.save(member(tenant, FIELD_COORDINATORUser, adminUser, "MEM-1"));
    enableModule(tenant, ModuleCode.REPORT_EXPORT);

    TenantEntity disabledTenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity disabledTenantAdminRole = roleRepository.save(
        TestDataFactory.role(disabledTenant, Role.ADMIN)
    );
    UserEntity disabledTenantAdmin = userRepository.save(TestDataFactory.user(
        disabledTenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Disabled Tenant Admin",
        disabledTenantAdminRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    disabledTenantAdminToken = jwtService.issueTokens(disabledTenantAdmin).accessToken();
  }

  @Test
  void testAdminCanReadFpoDashboardSummary() throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("ONI", "Onion"));
    CropSeasonEntity season = seasonRepository.save(season("KHA", "Kharif", 2026));
    InputCatalogEntity input = inputRepository.save(input("NPK", "NPK 19"));
    landholdingRepository.save(landholding(new BigDecimal("3.0000"), FarmRecordStatus.ACTIVE));
    plotRepository.save(plot(new BigDecimal("1.5000"), FarmRecordStatus.ACTIVE));
    SeasonalCropPlanEntity plan = cropPlanRepository.save(
        plan(crop, season, new BigDecimal("1.5000"), CropPlanStatus.CONFIRMED)
    );
    demandEstimateRepository.save(estimate(plan, input, new BigDecimal("15.0000")));

    mockMvc.perform(get("/api/v1/fpo/reports/summary")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalMembers").value(1))
        .andExpect(jsonPath("$.data.activeMembers").value(1))
        .andExpect(jsonPath("$.data.activeLandholdings").value(1))
        .andExpect(jsonPath("$.data.activePlots").value(1))
        .andExpect(jsonPath("$.data.geoTaggedPlots").value(1))
        .andExpect(jsonPath("$.data.confirmedCropPlanCount").value(1))
        .andExpect(jsonPath("$.data.cropPlanAreaByCrop[0].label").value("Onion"))
        .andExpect(jsonPath("$.data.inputDemandByInput[0].inputName").value("NPK 19"))
        .andExpect(jsonPath("$.data.inputDemandByInput[0].estimatedQuantity").value(15.0));
  }

  @Test
  void testAdminCanExportFpoOperationsWorkbook() throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("ONI", "Onion"));
    CropSeasonEntity season = seasonRepository.save(season("KHA", "Kharif", 2026));
    InputCatalogEntity input = inputRepository.save(input("NPK", "NPK 19"));
    landholdingRepository.save(landholding(new BigDecimal("3.0000"), FarmRecordStatus.ACTIVE));
    plotRepository.save(plot(new BigDecimal("1.5000"), FarmRecordStatus.ACTIVE));
    SeasonalCropPlanEntity plan = cropPlanRepository.save(
        plan(crop, season, new BigDecimal("1.5000"), CropPlanStatus.CONFIRMED)
    );
    demandEstimateRepository.save(estimate(plan, input, new BigDecimal("15.0000")));

    mockMvc.perform(post("/api/v1/fpo/reports/export")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("""
                {
                  "filters": {
                    "season": "Kharif 2026"
                  }
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.reportType").value("FPO_OPERATIONS"))
        .andExpect(jsonPath("$.data.format").value("XLSX"))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.storageKey").isString())
        .andExpect(jsonPath("$.data.completedAt").isString());

    ReportExportEntity export = reportExportRepository.findAll().stream()
        .filter(item -> item.getFormat() == ReportFormat.XLSX)
        .filter(item -> "FPO_OPERATIONS".equals(item.getReportType()))
        .max(Comparator.comparing(ReportExportEntity::getRequestedAt))
        .orElseThrow();
    Path workbookPath = Path.of("target/test-storage").resolve(export.getStorageKey());
    assertFpoWorkbook(workbookPath);
  }

  @Test
  void testDisabledReportModuleBlocksFpoDashboardSummary() throws Exception {
    mockMvc.perform(get("/api/v1/fpo/reports/summary")
            .header("Authorization", "Bearer " + disabledTenantAdminToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));
  }

  @Test
  void testDisabledReportModuleBlocksFpoExport() throws Exception {
    mockMvc.perform(post("/api/v1/fpo/reports/export")
            .header("Authorization", "Bearer " + disabledTenantAdminToken)
            .contentType("application/json")
            .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));
  }

  private void assertFpoWorkbook(Path workbookPath) throws Exception {
    try (ZipFile workbook = new ZipFile(workbookPath.toFile())) {
      String workbookXml = new String(
          workbook.getInputStream(workbook.getEntry("xl/workbook.xml")).readAllBytes(),
          StandardCharsets.UTF_8
      );
      String demandSheet = new String(
          workbook.getInputStream(workbook.getEntry("xl/worksheets/sheet7.xml")).readAllBytes(),
          StandardCharsets.UTF_8
      );

      org.assertj.core.api.Assertions.assertThat(workbookXml)
          .contains("Farmer Master")
          .contains("Landholdings")
          .contains("Farm Plots")
          .contains("Crop History")
          .contains("Seasonal Crop Plans")
          .contains("Input Demand Summary")
          .contains("Farmer-wise Input Demand");
      org.assertj.core.api.Assertions.assertThat(demandSheet)
          .contains("MEM-1")
          .contains("Onion")
          .contains("NPK 19")
          .contains("15.0000");
    }
  }

  private FpoMemberProfileEntity member(
      TenantEntity memberTenant,
      UserEntity user,
      UserEntity coordinator,
      String memberNumber
  ) {
    Instant now = Instant.now();
    return new FpoMemberProfileEntity(
        UUID.randomUUID(),
        memberTenant,
        user,
        memberNumber,
        user.getDisplayName(),
        phoneFor(memberNumber),
        null,
        null,
        "Village",
        "Block",
        "District",
        "Maharashtra",
        "MALE",
        null,
        42,
        "MARGINAL",
        coordinator,
        FpoMemberStatus.ACTIVE,
        now
    );
  }

  private FarmLandholdingEntity landholding(BigDecimal area, FarmRecordStatus status) {
    return new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        member,
        "SUR-1",
        area,
        area,
        "Self-owned",
        "Canal",
        status,
        Instant.now()
    );
  }

  private FarmPlotEntity plot(BigDecimal area, FarmRecordStatus status) {
    return new FarmPlotEntity(
        UUID.randomUUID(),
        tenant,
        member,
        null,
        "North plot",
        area,
        new BigDecimal("19.8765432"),
        new BigDecimal("73.1234567"),
        "LOAM",
        status,
        Instant.now()
    );
  }

  private CropCatalogEntity crop(String code, String name) {
    return new CropCatalogEntity(
        UUID.randomUUID(),
        tenant,
        code,
        name,
        "Vegetable",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropSeasonEntity season(String code, String name, Integer seasonYear) {
    return new CropSeasonEntity(
        UUID.randomUUID(),
        tenant,
        code,
        name,
        6,
        9,
        seasonYear,
        FarmRecordStatus.ACTIVE,
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

  private SeasonalCropPlanEntity plan(
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

  private String phoneFor(String memberNumber) {
    long value = Math.abs((long) memberNumber.hashCode()) % 10_000_000_000L;
    return "+91" + String.format("%010d", value);
  }

  private void enableModule(TenantEntity moduleTenant, ModuleCode moduleCode) {
    PlatformModuleEntity module = platformModuleRepository.findByCode(moduleCode)
        .orElseThrow();
    Instant now = Instant.now();
    subscriptionRepository.save(new TenantModuleSubscriptionEntity(
        UUID.randomUUID(),
        moduleTenant,
        module,
        TenantModuleSubscriptionStatus.ENABLED,
        now,
        null,
        null,
        now
    ));
  }
}
