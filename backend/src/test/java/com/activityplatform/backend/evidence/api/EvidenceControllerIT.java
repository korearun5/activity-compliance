package com.activityplatform.backend.evidence.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EvidenceControllerIT {
  @Autowired
  private MockMvc mockMvc;

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

  private String accessToken;

  @BeforeEach
  void setup() {
    TenantEntity tenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );
    UserEntity user = userRepository.save(TestDataFactory.user(
        tenant,
        "testuser-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Test User",
        FIELD_COORDINATORRole
    ));

    accessToken = jwtService.issueTokens(user).accessToken();
  }

  @Test
  void testListEvidenceEmpty() throws Exception {
    mockMvc.perform(get("/api/v1/evidence")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void testGetEvidenceNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/evidence/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void testUnauthorizedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/evidence"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testUploadEvidenceRequiresValidActivityAndTask() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "evidence.jpg",
        "image/jpeg",
        "fake image content".getBytes()
    );

    mockMvc.perform(multipart("/api/v1/evidence")
            .file(file)
            .param("activityId", UUID.randomUUID().toString())
            .param("activityTaskId", UUID.randomUUID().toString())
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound());
  }
}
