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
  private String participantToken;
  private TenantEntity tenant;
  private UserEntity participantUser;
  private UserEntity otherTenantUser;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity participantRole = roleRepository.save(TestDataFactory.role(tenant, Role.PARTICIPANT));

    UserEntity adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Admin User",
        adminRole
    ));
    participantUser = userRepository.save(TestDataFactory.user(
        tenant,
        "participant-" + UUID.randomUUID(),
        passwordEncoder.encode("participant123"),
        "Participant User",
        participantRole
    ));

    TenantEntity otherTenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity otherParticipantRole = roleRepository.save(
        TestDataFactory.role(otherTenant, Role.PARTICIPANT)
    );
    otherTenantUser = userRepository.save(TestDataFactory.user(
        otherTenant,
        "other-" + UUID.randomUUID(),
        passwordEncoder.encode("participant123"),
        "Other Tenant User",
        otherParticipantRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    participantToken = jwtService.issueTokens(participantUser).accessToken();
  }

  @Test
  void testCreateParticipantAsAdmin() throws Exception {
    long auditCount = auditEventRepository.count();
    String username = "Farmer.Profile-" + UUID.randomUUID();
    CreateUserRequest request = new CreateUserRequest(
        username,
        "password123",
        "New Farmer",
        "  +91 99999 00000  ",
        "  Nashik  ",
        "  Plot 14  "
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
        .andExpect(jsonPath("$.data.roles[0]").value("PARTICIPANT"))
        .andExpect(jsonPath("$.data.password").doesNotExist())
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

    UserEntity savedUser = userRepository
        .findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(tenant.getCode(), username)
        .orElseThrow();
    assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash())).isTrue();
    assertThat(savedUser.getRoles()).extracting(RoleEntity::getCode)
        .containsExactly(Role.PARTICIPANT.name());
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testCreateParticipantRejectsDuplicateUsername() throws Exception {
    String username = "duplicate-" + UUID.randomUUID();
    CreateUserRequest firstRequest = new CreateUserRequest(
        username,
        "password123",
        "First User",
        null,
        null,
        null
    );
    CreateUserRequest duplicateRequest = new CreateUserRequest(
        username.toUpperCase(Locale.ROOT),
        "password123",
        "Duplicate User",
        null,
        null,
        null
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
  void testCreateParticipantForbiddenForParticipant() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "blocked-" + UUID.randomUUID(),
        "password123",
        "Blocked User",
        null,
        null,
        null
    );

    mockMvc.perform(post("/api/v1/users")
            .header("Authorization", "Bearer " + participantToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  @Test
  void testCreateParticipantValidatesRequest() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "-invalid",
        "short",
        "",
        null,
        null,
        null
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
        .andExpect(jsonPath("$.data.page.totalElements").value(2));
  }

  @Test
  void testGetUser() throws Exception {
    mockMvc.perform(get("/api/v1/users/" + participantUser.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(participantUser.getId().toString()))
        .andExpect(jsonPath("$.data.displayName").value("Participant User"));
  }

  @Test
  void testGetUserRejectsOtherTenantUser() throws Exception {
    mockMvc.perform(get("/api/v1/users/" + otherTenantUser.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void testGetCurrentUserProfileAsParticipant() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", "Bearer " + participantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(participantUser.getId().toString()))
        .andExpect(jsonPath("$.data.username").value(participantUser.getUsername()))
        .andExpect(jsonPath("$.data.displayName").value("Participant User"))
        .andExpect(jsonPath("$.data.phone").value("+91 00000 00000"))
        .andExpect(jsonPath("$.data.locationName").value("Test Location"))
        .andExpect(jsonPath("$.data.siteName").value("Test Site"))
        .andExpect(jsonPath("$.data.roles[0]").value("PARTICIPANT"));
  }

  @Test
  void testUpdateParticipantAsAdmin() throws Exception {
    long auditCount = auditEventRepository.count();
    UpdateUserRequest request = new UpdateUserRequest(
        "Updated Farmer",
        "+91 88888 00000",
        "Updated Region",
        "Updated Plot"
    );

    mockMvc.perform(put("/api/v1/users/" + participantUser.getId())
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
  void testUpdateParticipantStatusAsAdmin() throws Exception {
    long auditCount = auditEventRepository.count();
    UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.INACTIVE);

    mockMvc.perform(patch("/api/v1/users/" + participantUser.getId() + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("INACTIVE"));

    UserEntity savedUser = userRepository.findById(participantUser.getId()).orElseThrow();
    assertThat(savedUser.getStatus()).isEqualTo("INACTIVE");
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testUnauthorizedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/users"))
        .andExpect(status().isUnauthorized());
  }
}
