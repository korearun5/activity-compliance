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
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.repository.WorkflowDefinitionRepository;
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
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String participantToken;
  private UUID otherParticipantId;
  private UUID workflowId;

  @BeforeEach
  void setup() {
    TenantEntity tenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity participantRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.PARTICIPANT)
    );
    UserEntity adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Admin User",
        adminRole
    ));
    UserEntity participantUser = userRepository.save(TestDataFactory.user(
        tenant,
        "testuser-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Test User",
        participantRole
    ));
    UserEntity otherParticipant = userRepository.save(TestDataFactory.user(
        tenant,
        "otheruser-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Other User",
        participantRole
    ));
    WorkflowDefinitionEntity workflow = workflowDefinitionRepository.save(
        TestDataFactory.workflow(
            tenant,
            "test-workflow-" + UUID.randomUUID(),
            WorkflowDefinitionStatus.ACTIVE
        )
    );

    workflowId = workflow.getId();
    otherParticipantId = otherParticipant.getId();
    adminToken = jwtService.issueTokens(adminUser).accessToken();
    participantToken = jwtService.issueTokens(participantUser).accessToken();
  }

  @Test
  void testListActivities() throws Exception {
    mockMvc.perform(get("/api/v1/activities")
            .header("Authorization", "Bearer " + participantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.page.totalElements").exists());
  }

  @Test
  void testGetActivityNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/activities/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + participantToken))
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
            .header("Authorization", "Bearer " + participantToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.unitName").value("Test Unit"))
        .andExpect(jsonPath("$.data.status").value("RUNNING"));
  }

  @Test
  void testAdminCanStartActivityForParticipant() throws Exception {
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        otherParticipantId,
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
        .andExpect(jsonPath("$.data.participantUserId").value(otherParticipantId.toString()))
        .andExpect(jsonPath("$.data.unitName").value("Assigned Unit"))
        .andExpect(jsonPath("$.data.tasks[0].status").value("NEXT"));
  }

  @Test
  void testParticipantCannotStartActivityForAnotherParticipant() throws Exception {
    StartActivityRequest request = new StartActivityRequest(
        workflowId,
        otherParticipantId,
        "Assigned Unit",
        "Assigned Location",
        LocalDate.now()
    );

    mockMvc.perform(post("/api/v1/activities")
            .header("Authorization", "Bearer " + participantToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void testUnauthorizedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/activities"))
        .andExpect(status().isUnauthorized());
  }
}
