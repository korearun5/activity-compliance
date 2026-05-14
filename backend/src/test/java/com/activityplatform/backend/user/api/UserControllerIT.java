package com.activityplatform.backend.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.activityplatform.backend.TestDataFactory;
import com.activityplatform.backend.TestcontainersConfiguration;
import com.activityplatform.backend.audit.repository.AuditEventRepository;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.auth.service.JwtService;
import com.activityplatform.backend.security.Role;
import java.util.Locale;
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
class UserControllerIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private AuditEventRepository auditEventRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String FIELD_COORDINATORToken;
  private String fpoManagerToken;
  private TenantEntity tenant;
  private UserEntity FIELD_COORDINATORUser;
  private UserEntity otherTenantUser;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(TestDataFactory.role(tenant, Role.FIELD_COORDINATOR));
    RoleEntity fpoManagerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FPO_MANAGER));

    UserEntity adminUser = userRepository.save(TestDataFactory.user(
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
    UserEntity fpoManagerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "fpo-manager-" + UUID.randomUUID(),
        passwordEncoder.encode("fpoManager123"),
        "FPO Manager User",
        fpoManagerRole
    ));

    TenantEntity otherTenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity otherFIELD_COORDINATORRole = roleRepository.save(
        TestDataFactory.role(otherTenant, Role.FIELD_COORDINATOR)
    );
    otherTenantUser = userRepository.save(TestDataFactory.user(
        otherTenant,
        "other-" + UUID.randomUUID(),
        passwordEncoder.encode("FIELD_COORDINATOR123"),
        "Other Tenant User",
        otherFIELD_COORDINATORRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    FIELD_COORDINATORToken = jwtService.issueTokens(FIELD_COORDINATORUser).accessToken();
    fpoManagerToken = jwtService.issueTokens(fpoManagerUser).accessToken();
  }

  @Test
  void testCreateFIELD_COORDINATORAsAdmin() throws Exception {
    long auditCount = auditEventRepository.count();
    String username = "Farmer.Profile-" + UUID.randomUUID();
    CreateUserRequest request = new CreateUserRequest(
        username,
        "password123",
        "New Farmer",
        "  +91 99999 00000  ",
        "  Nashik  ",
        "  Plot 14  ",
        Role.FIELD_COORDINATOR
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.username").value(username.toLowerCase(Locale.ROOT)))
        .andExpect(jsonPath("$.data.displayName").value("New Farmer"))
        .andExpect(jsonPath("$.data.phone").value("+91 99999 00000"))
        .andExpect(jsonPath("$.data.locationName").value("Nashik"))
        .andExpect(jsonPath("$.data.siteName").value("Plot 14"))
        .andExpect(jsonPath("$.data.roles[0]").value("FIELD_COORDINATOR"))
        .andExpect(jsonPath("$.data.password").doesNotExist())
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

    UserEntity savedUser = userRepository
        .findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(tenant.getCode(), username)
        .orElseThrow();
    assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash())).isTrue();
    assertThat(savedUser.getRoles()).extracting(RoleEntity::getCode)
        .containsExactly(Role.FIELD_COORDINATOR.name());
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testCreateFpoManagerAsAdmin() throws Exception {
    String username = "fpo-manager-" + UUID.randomUUID();
    CreateUserRequest request = new CreateUserRequest(
        username,
        "password123",
        "Pilot FPO Manager",
        "9999900001",
        "Wagholi",
        "Pilot FPO",
        Role.FPO_MANAGER
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.username").value(username.toLowerCase(Locale.ROOT)))
        .andExpect(jsonPath("$.data.roles[0]").value("FPO_MANAGER"));

    UserEntity savedUser = userRepository
        .findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(tenant.getCode(), username)
        .orElseThrow();
    assertThat(savedUser.getRoles()).extracting(RoleEntity::getCode)
        .containsExactly(Role.FPO_MANAGER.name());
  }

  @Test
  void testGenericUserCreateRejectsFarmerRole() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "farmer-user-" + UUID.randomUUID(),
        "password123",
        "Farmer User",
        "9999900002",
        "Wagholi",
        "Pilot FPO",
        Role.FARMER
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testFpoManagerCanCreateFieldCoordinatorOnly() throws Exception {
    CreateUserRequest coordinatorRequest = new CreateUserRequest(
        "coordinator-" + UUID.randomUUID(),
        "password123",
        "Village Coordinator",
        "9999900003",
        "Wagholi",
        "Pilot FPO",
        Role.FIELD_COORDINATOR
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + fpoManagerToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(coordinatorRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.roles[0]").value("FIELD_COORDINATOR"));

    CreateUserRequest managerRequest = new CreateUserRequest(
        "blocked-manager-" + UUID.randomUUID(),
        "password123",
        "Blocked Manager",
        "9999900004",
        "Wagholi",
        "Pilot FPO",
        Role.FPO_MANAGER
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + fpoManagerToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(managerRequest)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  @Test
  void testCreateFIELD_COORDINATORRejectsDuplicateUsername() throws Exception {
    String username = "duplicate-" + UUID.randomUUID();
    CreateUserRequest firstRequest = new CreateUserRequest(
        username,
        "password123",
        "First User",
        null,
        null,
        null,
        Role.FIELD_COORDINATOR
    );
    CreateUserRequest duplicateRequest = new CreateUserRequest(
        username.toUpperCase(Locale.ROOT),
        "password123",
        "Duplicate User",
        null,
        null,
        null,
        Role.FIELD_COORDINATOR
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(duplicateRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
  }

  @Test
  void testCreateFIELD_COORDINATORForbiddenForFIELD_COORDINATOR() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "blocked-" + UUID.randomUUID(),
        "password123",
        "Blocked User",
        null,
        null,
        null,
        Role.FIELD_COORDINATOR
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  @Test
  void testCreateFIELD_COORDINATORValidatesRequest() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "-invalid",
        "short",
        "",
        null,
        null,
        null,
        Role.FIELD_COORDINATOR
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.error.details.username").isArray())
        .andExpect(jsonPath("$.error.details.password").isArray())
        .andExpect(jsonPath("$.error.details.displayName").isArray());
  }

  @Test
  void testListUsersIsTenantScoped() throws Exception {
    mockMvc.perform(get("/api/v1/users")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.page.totalElements").value(3));
  }

  @Test
  void testGetUser() throws Exception {
    mockMvc.perform(get("/api/v1/users/" + FIELD_COORDINATORUser.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(FIELD_COORDINATORUser.getId().toString()))
        .andExpect(jsonPath("$.data.displayName").value("FIELD_COORDINATOR User"));
  }

  @Test
  void testGetUserRejectsOtherTenantUser() throws Exception {
    mockMvc.perform(get("/api/v1/users/" + otherTenantUser.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void testGetCurrentUserProfileAsFIELD_COORDINATOR() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(FIELD_COORDINATORUser.getId().toString()))
        .andExpect(jsonPath("$.data.username").value(FIELD_COORDINATORUser.getUsername()))
        .andExpect(jsonPath("$.data.displayName").value("FIELD_COORDINATOR User"))
        .andExpect(jsonPath("$.data.phone").value("+91 00000 00000"))
        .andExpect(jsonPath("$.data.locationName").value("Test Location"))
        .andExpect(jsonPath("$.data.siteName").value("Test Site"))
        .andExpect(jsonPath("$.data.roles[0]").value("FIELD_COORDINATOR"));
  }

  @Test
  void testUpdateFIELD_COORDINATORAsAdmin() throws Exception {
    long auditCount = auditEventRepository.count();
    UpdateUserRequest request = new UpdateUserRequest(
        "Updated Farmer",
        "+91 88888 00000",
        "Updated Region",
        "Updated Plot"
    );

    mockMvc.perform(put("/api/v1/users/" + FIELD_COORDINATORUser.getId())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.displayName").value("Updated Farmer"))
        .andExpect(jsonPath("$.data.phone").value("+91 88888 00000"))
        .andExpect(jsonPath("$.data.locationName").value("Updated Region"))
        .andExpect(jsonPath("$.data.siteName").value("Updated Plot"));

    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testUpdateFIELD_COORDINATORStatusAsAdmin() throws Exception {
    long auditCount = auditEventRepository.count();
    UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.INACTIVE);

    mockMvc.perform(patch("/api/v1/users/" + FIELD_COORDINATORUser.getId() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("INACTIVE"));

    UserEntity savedUser = userRepository.findById(FIELD_COORDINATORUser.getId()).orElseThrow();
    assertThat(savedUser.getStatus()).isEqualTo("INACTIVE");
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testUnauthorizedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/users"))
        .andExpect(status().isUnauthorized());
  }
}
