package com.activityplatform.backend.fpo.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import com.activityplatform.backend.farmer.repository.FarmerProfileRepository;
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
class FpoMemberControllerIT {
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
  private FarmerProfileRepository farmerProfileRepository;

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
  private String farmerToken;
  private String disabledTenantAdminToken;
  private TenantEntity tenant;
  private TenantEntity disabledTenant;
  private UserEntity adminUser;
  private UserEntity FIELD_COORDINATORUser;
  private UserEntity farmerUser;

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
    farmerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "Farmer User",
        farmerRole
    ));
    enableMemberData(tenant);

    disabledTenant = tenantRepository.save(
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
    farmerToken = jwtService.issueTokens(farmerUser).accessToken();
    disabledTenantAdminToken = jwtService.issueTokens(disabledTenantAdmin).accessToken();
  }

  @Test
  void testAdminCanCreateMemberWithNewFIELD_COORDINATORUser() throws Exception {
    String memberNumber = "MEM-" + UUID.randomUUID();
    String username = "farmer-" + UUID.randomUUID();
    CreateFpoMemberRequest request = createRequest(memberNumber, username);

    mockMvc.perform(post("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.memberNumber").value(memberNumber))
        .andExpect(jsonPath("$.data.username").value(username.toLowerCase()))
        .andExpect(jsonPath("$.data.mobileNumber").value("9999900000"))
        .andExpect(jsonPath("$.data.aadhaarNumber").value("123456789012"))
        .andExpect(jsonPath("$.data.taluka").value("Haveli"))
        .andExpect(jsonPath("$.data.districtName").value("Pune"))
        .andExpect(jsonPath("$.data.stateName").value("Maharashtra"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

    UserEntity createdUser = userRepository
        .findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(tenant.getCode(), username)
        .orElseThrow();
    assertThat(passwordEncoder.matches("password123", createdUser.getPasswordHash()))
        .isTrue();
    assertThat(memberRepository.existsByTenantIdAndUserId(tenant.getId(), createdUser.getId()))
        .isTrue();
    assertThat(createdUser.getRoles()).extracting(RoleEntity::getCode)
        .containsExactly(Role.FARMER.name());
    FarmerProfileEntity farmerProfile = farmerProfileRepository
        .findByTenantIdAndUserId(tenant.getId(), createdUser.getId())
        .orElseThrow();
    FpoMemberProfileEntity member = memberRepository
        .findByTenantIdAndUserId(tenant.getId(), createdUser.getId())
        .orElseThrow();
    assertThat(member.getFarmerProfileId()).isEqualTo(farmerProfile.getId());
    assertThat(farmerProfile.getMobileNumber()).isEqualTo("9999900000");
  }

  @Test
  void testAdminCanLinkExistingFarmerAndFarmerCanReadOwnProfile()
      throws Exception {
    CreateFpoMemberRequest request = createRequestWithExistingUser(
        "MEM-" + UUID.randomUUID(),
        farmerUser.getId()
    );

    String response = mockMvc.perform(post("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
        .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(farmerUser.getId().toString()))
        .andReturn()
        .getResponse()
        .getContentAsString();
    FpoMemberResponse member = jsonMapper.readValue(
        jsonMapper.readTree(response).get("data").toString(),
        FpoMemberResponse.class
    );
    FarmerProfileEntity farmerProfile = farmerProfileRepository
        .findByTenantIdAndUserId(tenant.getId(), farmerUser.getId())
        .orElseThrow();
    FpoMemberProfileEntity savedMember = memberRepository.findById(member.id()).orElseThrow();
    assertThat(savedMember.getFarmerProfileId()).isEqualTo(farmerProfile.getId());

    mockMvc.perform(get("/api/v1/fpo/members/me")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(member.id().toString()))
        .andExpect(jsonPath("$.data.userId").value(farmerUser.getId().toString()));
  }

  @Test
  void testListUpdateAndStatusChangeAreTenantScoped() throws Exception {
    CreateFpoMemberRequest createRequest = createRequestWithExistingUser(
        "MEM-" + UUID.randomUUID(),
        farmerUser.getId()
    );
    FpoMemberProfileEntity member = createMember(createRequest);

    mockMvc.perform(get("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id").value(member.getId().toString()));

    UpdateFpoMemberRequest updateRequest = new UpdateFpoMemberRequest(
        member.getMemberNumber(),
        "Updated Farmer",
        "+91 88888 00000",
        null,
        null,
        "Updated Village",
        "Updated Taluka",
        "Updated District",
        "Maharashtra",
        "FEMALE",
        null,
        34,
        "SMALL",
        FIELD_COORDINATORUser.getId(),
        FpoMemberStatus.ACTIVE
    );

    mockMvc.perform(put("/api/v1/fpo/members/" + member.getId())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName").value("Updated Farmer"))
        .andExpect(jsonPath("$.data.mobileNumber").value("8888800000"))
        .andExpect(jsonPath("$.data.taluka").value("Updated Taluka"))
        .andExpect(jsonPath("$.data.stateName").value("Maharashtra"))
        .andExpect(jsonPath("$.data.coordinatorUserId").value(FIELD_COORDINATORUser.getId().toString()));
    FarmerProfileEntity updatedFarmerProfile = farmerProfileRepository
        .findByTenantIdAndUserId(tenant.getId(), farmerUser.getId())
        .orElseThrow();
    assertThat(updatedFarmerProfile.getDisplayName()).isEqualTo("Updated Farmer");
    assertThat(updatedFarmerProfile.getMobileNumber()).isEqualTo("8888800000");
    assertThat(updatedFarmerProfile.getStatus()).isEqualTo(FarmerProfileStatus.ACTIVE);

    mockMvc.perform(patch("/api/v1/fpo/members/" + member.getId() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                new UpdateFpoMemberStatusRequest(FpoMemberStatus.SUSPENDED)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    FarmerProfileEntity suspendedFarmerProfile = farmerProfileRepository
        .findByTenantIdAndUserId(tenant.getId(), farmerUser.getId())
        .orElseThrow();
    assertThat(suspendedFarmerProfile.getStatus()).isEqualTo(FarmerProfileStatus.SUSPENDED);

    mockMvc.perform(get("/api/v1/fpo/members/" + member.getId())
            .header("Authorization", "Bearer " + disabledTenantAdminToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));
  }

  @Test
  void testDuplicateMemberNumberReturnsConflict() throws Exception {
    String memberNumber = "MEM-" + UUID.randomUUID();
    createMember(createRequest(memberNumber, "farmer-" + UUID.randomUUID()));

    mockMvc.perform(post("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                createRequest(memberNumber.toLowerCase(), "farmer-" + UUID.randomUUID())
            )))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
  }

  @Test
  void testApprovedFarmerProfileValidationRejectsInvalidCategory() throws Exception {
    CreateFpoMemberRequest request = new CreateFpoMemberRequest(
        null,
        "farmer-" + UUID.randomUUID(),
        "password123",
        "MEM-" + UUID.randomUUID(),
        "Invalid Category Farmer",
        "+91 99999 00000",
        null,
        null,
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        null,
        42,
        "UNAPPROVED",
        adminUser.getId(),
        FpoMemberStatus.ACTIVE
    );

    mockMvc.perform(post("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testFIELD_COORDINATORCanCreateAssignedFarmerMember() throws Exception {
    mockMvc.perform(post("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                createRequest("MEM-" + UUID.randomUUID(), "farmer-" + UUID.randomUUID())
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.coordinatorUserId").value(FIELD_COORDINATORUser.getId().toString()));
  }

  @Test
  void testDisabledModuleBlocksMemberApi() throws Exception {
    mockMvc.perform(post("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + disabledTenantAdminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(
                createRequest("MEM-" + UUID.randomUUID(), "farmer-" + UUID.randomUUID())
            )))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));
  }

  private FpoMemberProfileEntity createMember(CreateFpoMemberRequest request) throws Exception {
    String response = mockMvc.perform(post("/api/v1/fpo/members")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    FpoMemberResponse created = jsonMapper.readValue(
        jsonMapper.readTree(response).get("data").toString(),
        FpoMemberResponse.class
    );
    return memberRepository.findById(created.id()).orElseThrow();
  }

  private CreateFpoMemberRequest createRequest(String memberNumber, String username) {
    return new CreateFpoMemberRequest(
        null,
        username,
        "password123",
        memberNumber,
        "New Farmer",
        "+91 99999 00000",
        null,
        "123456789012",
        "Nashik Village",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        null,
        42,
        "MARGINAL",
        FIELD_COORDINATORUser.getId(),
        FpoMemberStatus.ACTIVE
    );
  }

  private CreateFpoMemberRequest createRequestWithExistingUser(
      String memberNumber,
      UUID userId
  ) {
    return new CreateFpoMemberRequest(
        userId,
        null,
        null,
        memberNumber,
        "Linked Farmer",
        "+91 99999 00000",
        null,
        null,
        "Linked Village",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        null,
        42,
        "MARGINAL",
        FIELD_COORDINATORUser.getId(),
        FpoMemberStatus.ACTIVE
    );
  }

  private void enableMemberData(TenantEntity moduleTenant) {
    PlatformModuleEntity module = platformModuleRepository.findByCode(ModuleCode.MEMBER_DATA)
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
