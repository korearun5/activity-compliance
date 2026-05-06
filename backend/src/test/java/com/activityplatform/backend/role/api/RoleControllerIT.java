package com.activityplatform.backend.role.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class RoleControllerIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private AuditEventRepository auditEventRepository;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private UserRepository userRepository;

  private String adminToken;
  private String participantToken;
  private String supervisorToken;
  private UserEntity adminUser;
  private UserEntity participantUser;

  @BeforeEach
  void setup() {
    TenantEntity tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity participantRole = roleRepository.save(TestDataFactory.role(tenant, Role.PARTICIPANT));
    RoleEntity supervisorRole = roleRepository.save(TestDataFactory.role(tenant, Role.SUPERVISOR));

    adminUser = userRepository.save(TestDataFactory.user(
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
    UserEntity supervisorUser = userRepository.save(TestDataFactory.user(
        tenant,
        "supervisor-" + UUID.randomUUID(),
        passwordEncoder.encode("supervisor123"),
        "Supervisor User",
        supervisorRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    participantToken = jwtService.issueTokens(participantUser).accessToken();
    supervisorToken = jwtService.issueTokens(supervisorUser).accessToken();
  }

  @Test
  void testManagersCanListTenantRoles() throws Exception {
    mockMvc.perform(get("/api/v1/roles")
            .header("Authorization", "Bearer " + supervisorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[*].code").isArray())
        .andExpect(jsonPath("$.data[?(@.code == 'ADMIN')]").exists())
        .andExpect(jsonPath("$.data[?(@.code == 'SUPERVISOR')]").exists())
        .andExpect(jsonPath("$.data[?(@.code == 'PARTICIPANT')]").exists());
  }

  @Test
  void testAdminCanUpdateUserRoles() throws Exception {
    long auditCount = auditEventRepository.count();

    mockMvc.perform(put("/api/v1/users/" + participantUser.getId() + "/roles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateUserRolesRequest(
                Set.of(Role.SUPERVISOR)
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(participantUser.getId().toString()))
        .andExpect(jsonPath("$.data.roles[0]").value("SUPERVISOR"));

    UserEntity savedUser = userRepository.findById(participantUser.getId()).orElseThrow();
    assertThat(savedUser.getRoles()).extracting(RoleEntity::getCode)
        .containsExactly(Role.SUPERVISOR.name());
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testAdminCannotChangeOwnRoles() throws Exception {
    mockMvc.perform(put("/api/v1/users/" + adminUser.getId() + "/roles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateUserRolesRequest(
                Set.of(Role.SUPERVISOR)
            ))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testSupervisorCannotUpdateRoles() throws Exception {
    mockMvc.perform(put("/api/v1/users/" + participantUser.getId() + "/roles")
            .header("Authorization", "Bearer " + supervisorToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateUserRolesRequest(
                Set.of(Role.SUPERVISOR)
            ))))
        .andExpect(status().isForbidden());
  }

  @Test
  void testParticipantCannotListRoles() throws Exception {
    mockMvc.perform(get("/api/v1/roles")
            .header("Authorization", "Bearer " + participantToken))
        .andExpect(status().isForbidden());
  }
}
