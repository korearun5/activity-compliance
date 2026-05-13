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
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
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
class FarmAssetControllerIT {
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
  private FarmLandholdingRepository landholdingRepository;

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
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));

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
    UserEntity farmerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "Farmer User",
        farmerRole
    ));
    member = memberRepository.save(member(tenant, farmerUser, FIELD_COORDINATORUser, "MEM-1"));
    enableModule(tenant, ModuleCode.LAND_RECORDS);

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
  void testAdminCanCreateListUpdateAndArchiveLandholding() throws Exception {
    CreateFarmLandholdingRequest createRequest = new CreateFarmLandholdingRequest(
        "S-100",
        new BigDecimal("5.5000"),
        new BigDecimal("4.5000"),
        "Self-owned",
        "Canal",
        FarmRecordStatus.ACTIVE
    );

    String response = mockMvc.perform(post("/api/v1/fpo/members/" + member.getId() + "/landholdings")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(createRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.memberId").value(member.getId().toString()))
        .andExpect(jsonPath("$.data.surveyNumber").value("S-100"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    FarmLandholdingResponse created = readData(response, FarmLandholdingResponse.class);

    mockMvc.perform(get("/api/v1/fpo/members/" + member.getId() + "/landholdings")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    UpdateFarmLandholdingRequest updateRequest = new UpdateFarmLandholdingRequest(
        "S-101",
        new BigDecimal("6.0000"),
        new BigDecimal("5.2500"),
        "Leased-in",
        "Borewell",
        FarmRecordStatus.ACTIVE
    );

    mockMvc.perform(put("/api/v1/fpo/landholdings/" + created.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.surveyNumber").value("S-101"))
        .andExpect(jsonPath("$.data.ownershipType").value("Leased-in"));

    mockMvc.perform(patch("/api/v1/fpo/landholdings/" + created.id() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateFarmRecordStatusRequest(FarmRecordStatus.ARCHIVED)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
  }

  @Test
  void testAdminCanCreatePlotAndFIELD_COORDINATORCanReadOwnPlots() throws Exception {
    FarmLandholdingEntity landholding = landholdingRepository.save(new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        member,
        "P-12",
        new BigDecimal("3.0000"),
        new BigDecimal("2.5000"),
        "Self-owned",
        "Rainfed",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    ));
    CreateFarmPlotRequest createRequest = new CreateFarmPlotRequest(
        landholding.getId(),
        "North Plot",
        new BigDecimal("1.2500"),
        new BigDecimal("19.8765432"),
        new BigDecimal("73.1234567"),
        "BLACK_SOIL",
        FarmRecordStatus.ACTIVE
    );

    String response = mockMvc.perform(post("/api/v1/fpo/members/" + member.getId() + "/plots")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(createRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.landholdingId").value(landholding.getId().toString()))
        .andExpect(jsonPath("$.data.plotName").value("North Plot"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    FarmPlotResponse created = readData(response, FarmPlotResponse.class);

    mockMvc.perform(get("/api/v1/fpo/members/" + member.getId() + "/plots")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    UpdateFarmPlotRequest updateRequest = new UpdateFarmPlotRequest(
        landholding.getId(),
        "North Plot A",
        new BigDecimal("1.5000"),
        new BigDecimal("19.8765000"),
        new BigDecimal("73.1234000"),
        "LOAM",
        FarmRecordStatus.ACTIVE
    );

    mockMvc.perform(put("/api/v1/fpo/plots/" + created.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.plotName").value("North Plot A"))
        .andExpect(jsonPath("$.data.areaAcres").value(1.5000));

    mockMvc.perform(patch("/api/v1/fpo/plots/" + created.id() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateFarmRecordStatusRequest(FarmRecordStatus.INACTIVE)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("INACTIVE"));
  }

  @Test
  void testInvalidLandholdingAreaReturnsValidationError() throws Exception {
    CreateFarmLandholdingRequest request = new CreateFarmLandholdingRequest(
        "S-200",
        new BigDecimal("2.0000"),
        new BigDecimal("3.0000"),
        "Self-owned",
        "Canal",
        FarmRecordStatus.ACTIVE
    );

    mockMvc.perform(post("/api/v1/fpo/members/" + member.getId() + "/landholdings")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testPlotCannotUseAnotherMembersLandholding() throws Exception {
    UserEntity otherFIELD_COORDINATOR = userRepository.save(TestDataFactory.user(
        tenant,
        "other-" + UUID.randomUUID(),
        passwordEncoder.encode("FIELD_COORDINATOR123"),
        "Other FIELD_COORDINATOR",
        roleRepository.findByTenantIdAndCodeIgnoreCase(tenant.getId(), Role.FIELD_COORDINATOR.name())
            .orElseThrow()
    ));
    UserEntity otherFarmer = userRepository.save(TestDataFactory.user(
        tenant,
        "other-farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "Other Farmer",
        roleRepository.findByTenantIdAndCodeIgnoreCase(tenant.getId(), Role.FARMER.name())
            .orElseThrow()
    ));
    FpoMemberProfileEntity otherMember = memberRepository.save(
        member(tenant, otherFarmer, otherFIELD_COORDINATOR, "MEM-2")
    );
    FarmLandholdingEntity otherLandholding = landholdingRepository.save(new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        otherMember,
        "OTHER",
        new BigDecimal("2.0000"),
        new BigDecimal("2.0000"),
        "Self-owned",
        "Rainfed",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    ));
    CreateFarmPlotRequest request = new CreateFarmPlotRequest(
        otherLandholding.getId(),
        "Wrong Plot",
        new BigDecimal("1.0000"),
        new BigDecimal("19.8765432"),
        new BigDecimal("73.1234567"),
        null,
        FarmRecordStatus.ACTIVE
    );

    mockMvc.perform(post("/api/v1/fpo/members/" + member.getId() + "/plots")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testDisabledLandRecordsModuleBlocksFarmAssetApi() throws Exception {
    mockMvc.perform(get("/api/v1/fpo/members/" + UUID.randomUUID() + "/landholdings")
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
