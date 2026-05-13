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
import com.activityplatform.backend.fpo.domain.CropInputRuleEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropInputRuleRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.InputCatalogRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
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
class InputDemandControllerIT {
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
  private CropInputRuleRepository ruleRepository;

  @Autowired
  private CropSeasonRepository seasonRepository;

  @Autowired
  private InputCatalogRepository inputRepository;

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
    enableModule(tenant, ModuleCode.INPUT_DEMAND);

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
  void testAdminCanCreateListUpdateAndArchiveInputCatalogRecord() throws Exception {
    String response = mockMvc.perform(post("/api/v1/fpo/inputs")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new InputCatalogRequest(
                " npk ",
                " NPK 19 ",
                "Fertilizer",
                " kg ",
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("NPK"))
        .andExpect(jsonPath("$.data.unit").value("KG"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    InputCatalogResponse created = readData(response, InputCatalogResponse.class);

    mockMvc.perform(get("/api/v1/fpo/inputs")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    mockMvc.perform(put("/api/v1/fpo/inputs/" + created.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new InputCatalogRequest(
                "NPK-2",
                "NPK Hybrid",
                "Fertilizer",
                "kg",
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("NPK-2"))
        .andExpect(jsonPath("$.data.name").value("NPK Hybrid"));

    mockMvc.perform(patch("/api/v1/fpo/inputs/" + created.id() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateFarmRecordStatusRequest(FarmRecordStatus.ARCHIVED)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
  }

  @Test
  void testAdminCanCreateListUpdateAndArchiveInputRule() throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("TOM", "Tomato"));
    InputCatalogEntity input = inputRepository.save(input("NPK", "NPK 19"));

    String response = mockMvc.perform(post("/api/v1/fpo/input-rules")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropInputRuleRequest(
                crop.getId(),
                input.getId(),
                new BigDecimal("2.5000"),
                "Basal",
                "First application",
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.cropName").value("Tomato"))
        .andExpect(jsonPath("$.data.inputName").value("NPK 19"))
        .andExpect(jsonPath("$.data.quantityPerAcre").value(2.5))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CropInputRuleResponse created = readData(response, CropInputRuleResponse.class);

    mockMvc.perform(get("/api/v1/fpo/input-rules")
            .header("Authorization", "Bearer " + adminToken)
            .queryParam("cropId", crop.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    mockMvc.perform(put("/api/v1/fpo/input-rules/" + created.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CropInputRuleRequest(
                crop.getId(),
                input.getId(),
                new BigDecimal("3.0000"),
                "Top dressing",
                null,
                FarmRecordStatus.ACTIVE
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.quantityPerAcre").value(3.0))
        .andExpect(jsonPath("$.data.applicationStage").value("Top dressing"));

    mockMvc.perform(patch("/api/v1/fpo/input-rules/" + created.id() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateFarmRecordStatusRequest(FarmRecordStatus.ARCHIVED)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
  }

  @Test
  void testAdminCanRunDemandAndReadSummary() throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("ONI", "Onion"));
    CropSeasonEntity season = seasonRepository.save(season("KHA", "Kharif", 2026));
    InputCatalogEntity input = inputRepository.save(input("NPK", "NPK 19"));
    cropPlanRepository.save(plan(member, crop, season, new BigDecimal("2.0000")));
    ruleRepository.save(rule(crop, input, new BigDecimal("4.0000"), "Basal"));
    ruleRepository.save(rule(crop, input, new BigDecimal("1.5000"), "Top"));

    mockMvc.perform(post("/api/v1/fpo/demand-estimates/run")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new InputDemandRunRequest(
                season.getId(),
                null,
                null,
                null
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.plansConsidered").value(1))
        .andExpect(jsonPath("$.data.estimatesGenerated").value(1))
        .andExpect(jsonPath("$.data.estimates[0].estimatedQuantity").value(11.0));

    mockMvc.perform(get("/api/v1/fpo/demand-estimates/summary")
            .header("Authorization", "Bearer " + adminToken)
            .queryParam("seasonId", season.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.planCount").value(1))
        .andExpect(jsonPath("$.data.memberCount").value(1))
        .andExpect(jsonPath("$.data.estimateCount").value(1))
        .andExpect(jsonPath("$.data.byInput[0].inputId").value(input.getId().toString()))
        .andExpect(jsonPath("$.data.byInput[0].estimatedQuantity").value(11.0))
        .andExpect(jsonPath("$.data.byCrop[0].cropName").value("Onion"))
        .andExpect(jsonPath("$.data.byVillage[0].village").value("Village"));

    mockMvc.perform(get("/api/v1/fpo/demand-estimates")
            .header("Authorization", "Bearer " + adminToken)
            .queryParam("seasonId", season.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].inputId").value(input.getId().toString()))
        .andExpect(jsonPath("$.data[0].memberId").value(member.getId().toString()));
  }

  @Test
  void testDisabledInputDemandModuleBlocksInputApi() throws Exception {
    mockMvc.perform(get("/api/v1/fpo/inputs")
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
      FpoMemberProfileEntity memberProfile,
      CropCatalogEntity crop,
      CropSeasonEntity season,
      BigDecimal areaAcres
  ) {
    return new SeasonalCropPlanEntity(
        UUID.randomUUID(),
        tenant,
        memberProfile,
        null,
        crop,
        season,
        areaAcres,
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 9, 30),
        CropPlanStatus.CONFIRMED,
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
