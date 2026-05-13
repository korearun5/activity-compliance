package com.activityplatform.backend.workflow.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkflowControllerIT {
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
  private TenantEntity tenant;
  private String userToken;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );
    UserEntity adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin123"),
        "Admin User",
        adminRole
    ));
    UserEntity regularUser = userRepository.save(TestDataFactory.user(
        tenant,
        "user-" + UUID.randomUUID(),
        passwordEncoder.encode("user123"),
        "Regular User",
        FIELD_COORDINATORRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    userToken = jwtService.issueTokens(regularUser).accessToken();
  }

  @Test
  void testListWorkflows() throws Exception {
    mockMvc.perform(get("/api/v1/workflows")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.page.totalElements").exists());
  }

  @Test
  void testCreateWorkflowAsAdmin() throws Exception {
    WorkflowRequest request = new WorkflowRequest(
        "new-workflow",
        "New Workflow",
        "agriculture",
        45,
        1,
        WorkflowDefinitionStatus.ACTIVE,
        List.of(
            new WorkflowTaskRequest("task1", "Task 1", 10, 0, true),
            new WorkflowTaskRequest("task2", "Task 2", 20, 10, true)
        )
    );

    mockMvc.perform(post("/api/v1/workflows")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.name").value("New Workflow"));
  }

  @Test
  void testUpdateWorkflowStatusAsAdmin() throws Exception {
    WorkflowDefinitionEntity workflow = workflowDefinitionRepository.save(
        TestDataFactory.workflow(
            tenant,
            "status-workflow-" + UUID.randomUUID(),
            WorkflowDefinitionStatus.DRAFT
        )
    );

    mockMvc.perform(patch("/api/v1/workflows/" + workflow.getId() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("""
                {
                  "status": "ACTIVE"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
  }

  @Test
  void testCreateWorkflowForbiddenForRegularUser() throws Exception {
    WorkflowRequest request = new WorkflowRequest(
        "new-workflow",
        "New Workflow",
        "agriculture",
        45,
        1,
        WorkflowDefinitionStatus.ACTIVE,
        List.of(new WorkflowTaskRequest("task1", "Task 1", 10, 0, true))
    );

    mockMvc.perform(post("/api/v1/workflows")
            .header("Authorization", "Bearer " + userToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void testGetWorkflowNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/workflows/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void testUnauthorizedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/workflows"))
        .andExpect(status().isUnauthorized());
  }
}
