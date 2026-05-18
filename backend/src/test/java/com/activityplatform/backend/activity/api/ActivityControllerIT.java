package com.activityplatform.backend.activity.api;

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
import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import com.activityplatform.backend.farmer.service.FarmerProfileCommand;
import com.activityplatform.backend.farmer.service.FarmerService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.repository.WorkflowDefinitionRepository;
import java.time.LocalDate;
import java.util.Set;
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
class ActivityControllerIT {
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
  private WorkflowDefinitionRepository workflowDefinitionRepository;

  @Autowired
  private FarmerService farmerService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String FIELD_COORDINATORToken;
  private String farmerToken;
  private RoleEntity farmerRole;
  private TenantEntity tenant;
  private UUID farmerUserId;
  private UUID otherFIELD_COORDINATORId;
  private UUID workflowId;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );
    farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));
    UserEntity adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Admin User",
        adminRole
    ));
    UserEntity FIELD_COORDINATORUser = userRepository.save(TestDataFactory.user(
        tenant,
        "testuser-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Test User",
        FIELD_COORDINATORRole
    ));
    UserEntity otherFIELD_COORDINATOR = userRepository.save(TestDataFactory.user(
        tenant,
        "otheruser-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Other User",
        FIELD_COORDINATORRole
    ));
    UserEntity farmerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Farmer User",
        farmerRole
    ));
    farmerService.createFarmerProfile(
        new CurrentUser(
            adminUser.getId(),
            tenant.getId(),
            adminUser.getUsername(),
            Set.of(Role.ADMIN)
        ),
        farmerUser,
        farmerCommand("Farmer User", "9999900000", FarmerProfileStatus.ACTIVE)
    );
    WorkflowDefinitionEntity workflow = workflowDefinitionRepository.save(
        TestDataFactory.workflow(
            tenant,
            "test-workflow-" + UUID.randomUUID(),
            WorkflowDefinitionStatus.ACTIVE
        )
    );

    workflowId = workflow.getId();
    farmerUserId = farmerUser.getId();
    otherFIELD_COORDINATORId = otherFIELD_COORDINATOR.getId();
    adminToken = jwtService.issueTokens(adminUser).accessToken();
    FIELD_COORDINATORToken = jwtService.issueTokens(FIELD_COORDINATORUser).accessToken();
    farmerToken = jwtService.issueTokens(farmerUser).accessToken();
  }

  @Test
  void testListActivities() throws Exception {
    mockMvc.perform(get("/api/v1/activities")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.page.totalElements").exists());
  }

  @Test
  void testGetActivityNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/activities/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void testStartActivity() throws Exception {
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        null,
        "Test Unit",
        "Test Location",
        LocalDate.now()
    );

    mockMvc.perform(post("/api/v1/activities")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.unitName").value("Test Unit"))
        .andExpect(jsonPath("$.data.status").value("RUNNING"));
  }

  @Test
  void testFarmerCanStartOwnActivity() throws Exception {
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        null,
        "Farmer Plot",
        "Wagholi",
        LocalDate.now()
    );

    mockMvc.perform(post("/api/v1/activities")
            .header("Authorization", "Bearer " + farmerToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.unitName").value("Farmer Plot"))
        .andExpect(jsonPath("$.data.status").value("RUNNING"));
  }

  @Test
  void testAdminCanStartActivityForFIELD_COORDINATOR() throws Exception {
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        otherFIELD_COORDINATORId,
        "Assigned Unit",
        "Assigned Location",
        LocalDate.now()
    );

    mockMvc.perform(post("/api/v1/activities")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.participantUserId").value(otherFIELD_COORDINATORId.toString()))
        .andExpect(jsonPath("$.data.unitName").value("Assigned Unit"))
        .andExpect(jsonPath("$.data.tasks[0].status").value("NEXT"));
  }

  @Test
  void testAdminCanAssignCanonicalFarmerAndFarmerCanListActivity() throws Exception {
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        farmerUserId,
        "Farmer Plot",
        "Wagholi",
        LocalDate.now()
    );

    mockMvc.perform(post("/api/v1/activities")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.participantUserId").value(farmerUserId.toString()))
        .andExpect(jsonPath("$.data.unitName").value("Farmer Plot"));

    mockMvc.perform(get("/api/v1/activities")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].participantUserId")
            .value(farmerUserId.toString()))
        .andExpect(jsonPath("$.data.content[0].unitName").value("Farmer Plot"));
  }

  @Test
  void testAdminCannotAssignFarmerWithoutCanonicalProfile() throws Exception {
    UserEntity unlinkedFarmer = userRepository.save(TestDataFactory.user(
        tenant,
        "unlinked-farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Unlinked Farmer",
        farmerRole
    ));
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        unlinkedFarmer.getId(),
        "Unlinked Plot",
        "Wagholi",
        LocalDate.now()
    );

    mockMvc.perform(post("/api/v1/activities")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testFIELD_COORDINATORCannotStartActivityForAnotherFIELD_COORDINATOR() throws Exception {
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        otherFIELD_COORDINATORId,
        "Assigned Unit",
        "Assigned Location",
        LocalDate.now()
    );

    mockMvc.perform(post("/api/v1/activities")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void testUnauthorizedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/activities"))
        .andExpect(status().isUnauthorized());
  }

  private FarmerProfileCommand farmerCommand(
      String displayName,
      String mobileNumber,
      FarmerProfileStatus status
  ) {
    return new FarmerProfileCommand(
        displayName,
        mobileNumber,
        null,
        null,
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "male",
        LocalDate.of(1990, 1, 1),
        36,
        "small",
        status
    );
  }
}
