package com.activityplatform.backend.platform.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionStatus;
import com.activityplatform.backend.security.Role;
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
class PlatformControllerIT {
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
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String supervisorToken;

  @BeforeEach
  void setup() {
    TenantEntity tenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity supervisorRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.SUPERVISOR)
    );

    UserEntity adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Admin User",
        adminRole
    ));
    UserEntity supervisorUser = userRepository.save(TestDataFactory.user(
        tenant,
        "supervisor-" + UUID.randomUUID(),
        passwordEncoder.encode("supervisor123"),
        "Supervisor User",
        supervisorRole
    ));

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    supervisorToken = jwtService.issueTokens(supervisorUser).accessToken();
  }

  @Test
  void testCatalogIsPublicAndIncludesMemberData() throws Exception {
    mockMvc.perform(get("/api/v1/platform/modules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[?(@.code == 'MEMBER_DATA')]").isArray());
  }

  @Test
  void testEnabledModulesRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/platform/modules/enabled"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testAdminCanEnableAndListTenantModule() throws Exception {
    TenantModuleSubscriptionRequest request = new TenantModuleSubscriptionRequest(
        TenantModuleSubscriptionStatus.ENABLED,
        null
    );

    mockMvc.perform(put("/api/v1/platform/module-subscriptions/MEMBER_DATA")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.code").value("MEMBER_DATA"))
        .andExpect(jsonPath("$.data.status").value("ENABLED"))
        .andExpect(jsonPath("$.data.enabled").value(true));

    mockMvc.perform(get("/api/v1/platform/modules/enabled")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.modules[0]").value("MEMBER_DATA"));

    mockMvc.perform(get("/api/v1/platform/module-subscriptions")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.code == 'MEMBER_DATA')]").isArray());
  }

  @Test
  void testSupervisorCannotManageTenantModules() throws Exception {
    TenantModuleSubscriptionRequest request = new TenantModuleSubscriptionRequest(
        TenantModuleSubscriptionStatus.ENABLED,
        null
    );

    mockMvc.perform(put("/api/v1/platform/module-subscriptions/MEMBER_DATA")
            .header("Authorization", "Bearer " + supervisorToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }
}
