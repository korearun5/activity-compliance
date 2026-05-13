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
  private String FIELD_COORDINATORToken;
  private String FPO_MANAGERToken;
  private UserEntity adminUser;
  private UserEntity FIELD_COORDINATORUser;

  @BeforeEach
  void setup() {
    TenantEntity tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(TestDataFactory.role(tenant, Role.FIELD_COORDINATOR));
    RoleEntity FPO_MANAGERRole = roleRepository.save(TestDataFactory.role(tenant, Role.FPO_MANAGER));
    roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));

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
    UserEntity FPO_MANAGERUser = userRepository.save(TestDataFactory.user(
        tenant,
        "FPO_MANAGER-" + UUID.randomUUID(),
        passwordEncoder.encode("FPO_MANAGER123"),
        "FPO_MANAGER User",
        FPO_MANAGERRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    FIELD_COORDINATORToken = jwtService.issueTokens(FIELD_COORDINATORUser).accessToken();
    FPO_MANAGERToken = jwtService.issueTokens(FPO_MANAGERUser).accessToken();
  }

  @Test
  void testManagersCanListTenantRoles() throws Exception {
    mockMvc.perform(get("/api/v1/roles")
            .header("Authorization", "Bearer " + FPO_MANAGERToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[*].code").isArray())
        .andExpect(jsonPath("$.data[?(@.code == 'ADMIN')]").exists())
        .andExpect(jsonPath("$.data[?(@.code == 'FPO_MANAGER')]").exists())
        .andExpect(jsonPath("$.data[?(@.code == 'FIELD_COORDINATOR')]").exists())
        .andExpect(jsonPath("$.data[?(@.code == 'FARMER')]").exists());
  }

  @Test
  void testAdminCanUpdateUserRoles() throws Exception {
    long auditCount = auditEventRepository.count();

    mockMvc.perform(put("/api/v1/users/" + FIELD_COORDINATORUser.getId() + "/roles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateUserRolesRequest(
                Set.of(Role.FPO_MANAGER)
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(FIELD_COORDINATORUser.getId().toString()))
        .andExpect(jsonPath("$.data.roles[0]").value("FPO_MANAGER"));

    UserEntity savedUser = userRepository.findById(FIELD_COORDINATORUser.getId()).orElseThrow();
    assertThat(savedUser.getRoles()).extracting(RoleEntity::getCode)
        .containsExactly(Role.FPO_MANAGER.name());
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 1);
  }

  @Test
  void testAdminCannotChangeOwnRoles() throws Exception {
    mockMvc.perform(put("/api/v1/users/" + adminUser.getId() + "/roles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateUserRolesRequest(
                Set.of(Role.FPO_MANAGER)
            ))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testAdminCannotCombineFarmerWithStaffRoles() throws Exception {
    mockMvc.perform(put("/api/v1/users/" + FIELD_COORDINATORUser.getId() + "/roles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateUserRolesRequest(
                Set.of(Role.FARMER, Role.FIELD_COORDINATOR)
            ))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void testFPO_MANAGERCannotUpdateRoles() throws Exception {
    mockMvc.perform(put("/api/v1/users/" + FIELD_COORDINATORUser.getId() + "/roles")
            .header("Authorization", "Bearer " + FPO_MANAGERToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateUserRolesRequest(
                Set.of(Role.FPO_MANAGER)
            ))))
        .andExpect(status().isForbidden());
  }

  @Test
  void testFIELD_COORDINATORCannotListRoles() throws Exception {
    mockMvc.perform(get("/api/v1/roles")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isForbidden());
  }
}
