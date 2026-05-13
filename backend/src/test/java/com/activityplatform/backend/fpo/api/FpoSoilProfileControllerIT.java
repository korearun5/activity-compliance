package com.activityplatform.backend.fpo.api;

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
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
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
class FpoSoilProfileControllerIT {
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
  private FpoMemberProfileEntity member;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));

    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Admin User",
        adminRole
    ));
    member = memberRepository.save(member(tenant, adminUser, adminUser, "MEM-1"));
    enableModule(tenant, ModuleCode.MEMBER_DATA);

    adminToken = jwtService.issueTokens(adminUser).accessToken();
  }

  @Test
  void testAdminCanCreateListAndUpdateSoilProfile() throws Exception {
    FpoSoilProfileRequest createRequest = new FpoSoilProfileRequest(
        new BigDecimal("0.6800"),
        new BigDecimal("7.20"),
        new BigDecimal("180.0000"),
        new BigDecimal("22.5000"),
        new BigDecimal("132.0000"),
        "wagholi-soil.pdf",
        "application/pdf",
        "https://storage.example.com/wagholi-soil.pdf",
        "Existing lab report"
    );

    String response = mockMvc.perform(post("/api/v1/fpo/members/" + member.getId()
            + "/soil-profiles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(createRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.memberId").value(member.getId().toString()))
        .andExpect(jsonPath("$.data.soilOrganicCarbon").value(0.6800))
        .andExpect(jsonPath("$.data.ph").value(7.20))
        .andExpect(jsonPath("$.data.reportFileName").value("wagholi-soil.pdf"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    FpoSoilProfileResponse created = readData(response, FpoSoilProfileResponse.class);

    mockMvc.perform(get("/api/v1/fpo/members/" + member.getId() + "/soil-profiles")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(created.id().toString()));

    FpoSoilProfileRequest updateRequest = new FpoSoilProfileRequest(
        new BigDecimal("0.7400"),
        new BigDecimal("6.90"),
        null,
        null,
        null,
        "updated-soil.pdf",
        "application/pdf",
        "https://storage.example.com/updated-soil.pdf",
        null
    );

    mockMvc.perform(put("/api/v1/fpo/soil-profiles/" + created.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.soilOrganicCarbon").value(0.7400))
        .andExpect(jsonPath("$.data.ph").value(6.90))
        .andExpect(jsonPath("$.data.reportFileName").value("updated-soil.pdf"));
  }

  @Test
  void testInvalidSoilProfileReturnsValidationError() throws Exception {
    FpoSoilProfileRequest request = new FpoSoilProfileRequest(
        null,
        new BigDecimal("15.00"),
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    mockMvc.perform(post("/api/v1/fpo/members/" + member.getId() + "/soil-profiles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
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
        "Wagholi",
        "Haveli",
        "Pune",
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
    return String.format("%010d", value);
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
