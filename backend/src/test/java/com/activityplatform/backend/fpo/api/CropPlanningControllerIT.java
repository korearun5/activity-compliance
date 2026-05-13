package com.activityplatform.backend.fpo.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CropPlanningControllerIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FpoMemberProfileRepository memberRepository;

  @Autowired
  private CropCatalogRepository cropRepository;

  @Autowired
  private CropSeasonRepository seasonRepository;

  @Autowired
  private FarmPlotRepository plotRepository;

  @Autowired
  private PlatformModuleRepository platformModuleRepository;

  @Autowired
  private TenantModuleSubscriptionRepository subscriptionRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String FIELD_COORDINATORToken;
  private String disabledTenantAdminToken;
  private TenantEntity tenant;
  private UserEntity adminUser;
  private UserEntity FIELD_COORDINATORUser;
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
    FIELD_COORDINATORUser = userRepository.save(TestDataFactory.user(
        tenant,
        "FIELD_COORDINATOR-" + UUID.randomUUID(),
        passwordEncoder.encode("FIELD_COORDINATOR123"),
        "FIELD_COORDINATOR User",
        FIELD_COORDINATORRole
    ));
    member = memberRepository.save(member(tenant, FIELD_COORDINATORUser, adminUser, "MEM-1"));
    enableModule(tenant, ModuleCode.CROP_PLANNING);

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
    FIELD_COORDINATORToken = jwtService.issueTokens(FIELD_COORDINATORUser).accessToken();
    disabledTenantAdminToken = jwtService.issueTokens(disabledTenantAdmin).accessToken();
  }

  @Test
  void testAdminCanCreateListUpdateAndArchiveCropCatalogRecord() throws Exception {
    String response = mockMvc.perform(post("/api/v1/fpo/crops")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropCatalogRequest(
                " tom ",
                " Tomato ",
                "Vegetable",
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("TOM"))
        .andExpect(jsonPath("$.data.name").value("Tomato"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CropCatalogResponse created = readData(response, CropCatalogResponse.class);

    mockMvc.perform(get("/api/v1/fpo/crops")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    mockMvc.perform(put("/api/v1/fpo/crops/" + created.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropCatalogRequest(
                "TOM-2",
                "Tomato Hybrid",
                "Vegetable",
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("TOM-2"))
        .andExpect(jsonPath("$.data.name").value("Tomato Hybrid"));

    mockMvc.perform(patch("/api/v1/fpo/crops/" + created.id() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateFarmRecordStatusRequest(FarmRecordStatus.ARCHIVED)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
  }

  @Test
  void testAdminCanCreateListUpdateAndArchiveSeason() throws Exception {
    String response = mockMvc.perform(post("/api/v1/fpo/seasons")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropSeasonRequest(
                "kha",
                "Kharif",
                6,
                9,
                2026,
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("KHA"))
        .andExpect(jsonPath("$.data.startMonth").value(6))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CropSeasonResponse created = readData(response, CropSeasonResponse.class);

    mockMvc.perform(get("/api/v1/fpo/seasons")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    mockMvc.perform(put("/api/v1/fpo/seasons/" + created.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropSeasonRequest(
                "KHA",
                "Kharif Updated",
                6,
                10,
                2026,
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Kharif Updated"))
        .andExpect(jsonPath("$.data.endMonth").value(10));

    mockMvc.perform(patch("/api/v1/fpo/seasons/" + created.id() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateFarmRecordStatusRequest(FarmRecordStatus.INACTIVE)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("INACTIVE"));
  }

  @Test
  void testAdminCanCreateCropHistoryAndFIELD_COORDINATORCanReadOwnHistory()
      throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("TOM", "Tomato"));
    CropSeasonEntity season = seasonRepository.save(season("RAB", "Rabi", 2025));

    String response = mockMvc.perform(post(
            "/api/v1/fpo/members/" + member.getId() + "/crop-history")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropHistoryRequest(
                crop.getId(),
                season.getId(),
                2025,
                new BigDecimal("1.2500"),
                new BigDecimal("18.5000"),
                "QUINTAL",
                "Good yield"
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.memberId").value(member.getId().toString()))
        .andExpect(jsonPath("$.data.cropName").value("Tomato"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CropHistoryResponse created = readData(response, CropHistoryResponse.class);

    mockMvc.perform(get("/api/v1/fpo/members/" + member.getId() + "/crop-history")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));
  }

  @Test
  void testAdminCanCreateListGetAndConfirmCropPlan() throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("ONI", "Onion"));
    CropSeasonEntity season = seasonRepository.save(season("KHA", "Kharif", 2026));
    FarmPlotEntity plot = plotRepository.save(plot(member, new BigDecimal("2.0000")));

    String response = mockMvc.perform(post("/api/v1/fpo/crop-plans")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropPlanRequest(
                member.getId(),
                plot.getId(),
                crop.getId(),
                season.getId(),
                new BigDecimal("1.5000"),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 9, 30),
                CropPlanStatus.DRAFT
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.memberId").value(member.getId().toString()))
        .andExpect(jsonPath("$.data.plotId").value(plot.getId().toString()))
        .andExpect(jsonPath("$.data.status").value("DRAFT"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CropPlanResponse created = readData(response, CropPlanResponse.class);

    mockMvc.perform(get("/api/v1/fpo/crop-plans")
            .header("Authorization", "Bearer " + adminToken)
            .queryParam("seasonId", season.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    mockMvc.perform(get("/api/v1/fpo/crop-plans/" + created.id())
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(created.id().toString()));

    mockMvc.perform(patch("/api/v1/fpo/crop-plans/" + created.id() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateCropPlanStatusRequest(CropPlanStatus.CONFIRMED)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
  }

  @Test
  void testCropPlanRejectsAreaAboveSelectedPlotArea() throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("CHI", "Chilli"));
    CropSeasonEntity season = seasonRepository.save(season("SUM", "Summer", 2026));
    FarmPlotEntity plot = plotRepository.save(plot(member, new BigDecimal("1.0000")));

    mockMvc.perform(post("/api/v1/fpo/crop-plans")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropPlanRequest(
                member.getId(),
                plot.getId(),
                crop.getId(),
                season.getId(),
                new BigDecimal("1.2500"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 5, 30),
                CropPlanStatus.DRAFT
            ))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testDisabledCropPlanningModuleBlocksCropApi() throws Exception {
    mockMvc.perform(get("/api/v1/fpo/crops")
            .header("Authorization", "Bearer " + disabledTenantAdminToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));
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

  private FarmPlotEntity plot(FpoMemberProfileEntity memberProfile, BigDecimal areaAcres) {
    return new FarmPlotEntity(
        UUID.randomUUID(),
        tenant,
        memberProfile,
        null,
        "North Plot",
        areaAcres,
        new BigDecimal("19.8765432"),
        new BigDecimal("73.1234567"),
        "BLACK_SOIL",
        FarmRecordStatus.ACTIVE,
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

  private <T> T readData(String response, Class<T> responseType) throws Exception {
    return jsonMapper.readValue(jsonMapper.readTree(response).get("data").toString(), responseType);
  }
}
