package com.activityplatform.backend.carbon.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.activityplatform.backend.carbon.domain.CarbonParticipantType;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
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
class CarbonProfileControllerIT {
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
  private PlatformModuleRepository platformModuleRepository;

  @Autowired
  private TenantModuleSubscriptionRepository subscriptionRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private TenantEntity tenant;
  private UserEntity adminUser;
  private UserEntity coordinatorUser;
  private UserEntity farmerUser;
  private String adminToken;
  private String coordinatorToken;
  private String farmerToken;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity coordinatorRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));

    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Admin User",
        adminRole
    ));
    coordinatorUser = userRepository.save(TestDataFactory.user(
        tenant,
        "coordinator-" + UUID.randomUUID(),
        passwordEncoder.encode("coordinator12345"),
        "Coordinator User",
        coordinatorRole
    ));
    farmerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "Carbon Farmer",
        farmerRole
    ));
    enableModule(tenant, ModuleCode.SUSTAINABILITY);

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    coordinatorToken = jwtService.issueTokens(coordinatorUser).accessToken();
    farmerToken = jwtService.issueTokens(farmerUser).accessToken();
  }

  @Test
  void testAdminCanManageCarbonProfilePlotAndSoilProfile() throws Exception {
    CarbonProfileResponse profile = createProfile(
        adminToken,
        "CAR-UAT-" + UUID.randomUUID().toString().substring(0, 8)
    );

    mockMvc.perform(get("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + adminToken)
            .param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id").value(profile.id().toString()));

    mockMvc.perform(get("/api/v1/carbon/profiles/me")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(profile.id().toString()));

    CarbonProfileRequest updateProfileRequest = profileRequest(
        profile.carbonIdentityId(),
        "Updated Carbon Farmer",
        farmerUser.getId(),
        coordinatorUser.getId(),
        new BigDecimal("2.7500")
    );
    mockMvc.perform(put("/api/v1/carbon/profiles/" + profile.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateProfileRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName").value("Updated Carbon Farmer"))
        .andExpect(jsonPath("$.data.totalLandHoldingAcres").value(2.7500));

    CarbonFarmPlotRequest plotRequest = new CarbonFarmPlotRequest(
        "Wagholi demo plot",
        "SUR-101",
        new BigDecimal("1.2500"),
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        "Drip",
        "Paddy",
        "Reduced tillage",
        CarbonRecordStatus.ACTIVE
    );
    String plotResponse = mockMvc.perform(post("/api/v1/carbon/profiles/" + profile.id() + "/plots")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(plotRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.carbonProfileId").value(profile.id().toString()))
        .andExpect(jsonPath("$.data.primaryCrop").value("Paddy"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonFarmPlotResponse plot = readData(plotResponse, CarbonFarmPlotResponse.class);

    CarbonFarmPlotRequest updatePlotRequest = new CarbonFarmPlotRequest(
        "Wagholi demo plot",
        "SUR-101",
        new BigDecimal("1.2500"),
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        "Drip",
        "Wheat",
        "Reduced tillage",
        CarbonRecordStatus.ACTIVE
    );
    mockMvc.perform(put("/api/v1/carbon/plots/" + plot.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updatePlotRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.primaryCrop").value("Wheat"));

    CarbonSoilProfileRequest soilRequest = new CarbonSoilProfileRequest(
        plot.id(),
        LocalDate.of(2026, 5, 15),
        "Pune Soil Lab",
        new BigDecimal("0.6800"),
        new BigDecimal("7.20"),
        null,
        new BigDecimal("180.0000"),
        new BigDecimal("22.5000"),
        new BigDecimal("132.0000"),
        null,
        "Clay loam",
        "wagholi-soil.pdf",
        "application/pdf",
        "carbon/soil/wagholi-soil.pdf",
        "https://storage.example.com/carbon/soil/wagholi-soil.pdf",
        CarbonRecordStatus.ACTIVE
    );
    String soilResponse = mockMvc.perform(post("/api/v1/carbon/profiles/" + profile.id()
            + "/soil-profiles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(soilRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.carbonProfileId").value(profile.id().toString()))
        .andExpect(jsonPath("$.data.carbonFarmPlotId").value(plot.id().toString()))
        .andExpect(jsonPath("$.data.soilOrganicCarbonPercent").value(0.6800))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonSoilProfileResponse soilProfile = readData(soilResponse, CarbonSoilProfileResponse.class);

    CarbonSoilProfileRequest updateSoilRequest = new CarbonSoilProfileRequest(
        plot.id(),
        LocalDate.of(2026, 5, 15),
        "Pune Soil Lab",
        new BigDecimal("0.7200"),
        new BigDecimal("6.90"),
        null,
        new BigDecimal("180.0000"),
        new BigDecimal("22.5000"),
        new BigDecimal("132.0000"),
        null,
        "Clay loam",
        "wagholi-soil-updated.pdf",
        "application/pdf",
        "carbon/soil/wagholi-soil-updated.pdf",
        "https://storage.example.com/carbon/soil/wagholi-soil-updated.pdf",
        CarbonRecordStatus.ACTIVE
    );
    mockMvc.perform(put("/api/v1/carbon/soil-profiles/" + soilProfile.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateSoilRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.soilOrganicCarbonPercent").value(0.7200))
        .andExpect(jsonPath("$.data.ph").value(6.90));

    mockMvc.perform(get("/api/v1/carbon/profiles/" + profile.id() + "/soil-profiles")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(soilProfile.id().toString()));
  }

  @Test
  void testFieldCoordinatorCannotReadUnassignedProfile() throws Exception {
    CarbonProfileResponse assigned = createProfile(
        adminToken,
        "CAR-FC-" + UUID.randomUUID().toString().substring(0, 8)
    );

    mockMvc.perform(get("/api/v1/carbon/profiles/" + assigned.id())
            .header("Authorization", "Bearer " + coordinatorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(assigned.id().toString()));

    CarbonProfileRequest unassignedRequest = profileRequest(
        "CAR-OPEN-" + UUID.randomUUID().toString().substring(0, 8),
        "Unassigned Farmer",
        farmerUser.getId(),
        null,
        new BigDecimal("1.5000")
    );
    String response = mockMvc.perform(post("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(unassignedRequest)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonProfileResponse unassigned = readData(response, CarbonProfileResponse.class);

    mockMvc.perform(get("/api/v1/carbon/profiles/" + unassigned.id())
            .header("Authorization", "Bearer " + coordinatorToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  @Test
  void testCarbonProfileApisRequireSustainabilityModule() throws Exception {
    TenantEntity disabledTenant = tenantRepository.save(
        TestDataFactory.tenant("disabled-" + UUID.randomUUID())
    );
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(disabledTenant, Role.ADMIN));
    UserEntity disabledAdmin = userRepository.save(TestDataFactory.user(
        disabledTenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Disabled Admin",
        adminRole
    ));
    String disabledToken = jwtService.issueTokens(disabledAdmin).accessToken();

    mockMvc.perform(get("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + disabledToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));
  }

  private CarbonProfileResponse createProfile(String token, String carbonIdentityId) throws Exception {
    String response = mockMvc.perform(post("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(profileRequest(
                carbonIdentityId,
                "Carbon Farmer",
                farmerUser.getId(),
                coordinatorUser.getId(),
                new BigDecimal("2.5000")
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(farmerUser.getId().toString()))
        .andExpect(jsonPath("$.data.coordinatorUserId").value(coordinatorUser.getId().toString()))
        .andExpect(jsonPath("$.data.carbonIdentityId").value(carbonIdentityId))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return readData(response, CarbonProfileResponse.class);
  }

  private CarbonProfileRequest profileRequest(
      String carbonIdentityId,
      String displayName,
      UUID userId,
      UUID coordinatorUserId,
      BigDecimal landHolding
  ) {
    return new CarbonProfileRequest(
        userId,
        null,
        coordinatorUserId,
        carbonIdentityId,
        CarbonParticipantType.FARMER,
        displayName,
        "9876543210",
        "English",
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        landHolding,
        "Paddy and wheat",
        2,
        "Reduced tillage",
        "Linked",
        "Optional not captured",
        "Partial",
        CarbonRecordStatus.ACTIVE
    );
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
