package com.activityplatform.backend.notification.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.activityplatform.backend.notification.domain.NotificationChannel;
import com.activityplatform.backend.notification.domain.NotificationStatus;
import com.activityplatform.backend.notification.repository.NotificationEventRepository;
import com.activityplatform.backend.security.Role;
import java.util.Map;
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
class NotificationControllerIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private AuditEventRepository auditEventRepository;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private NotificationEventRepository notificationEventRepository;

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
  private UserEntity FIELD_COORDINATORUser;

  @BeforeEach
  void setup() {
    TenantEntity tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity FIELD_COORDINATORRole = roleRepository.save(TestDataFactory.role(tenant, Role.FIELD_COORDINATOR));

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

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    FIELD_COORDINATORToken = jwtService.issueTokens(FIELD_COORDINATORUser).accessToken();
  }

  @Test
  void testQueueListAndUpdateNotificationStatus() throws Exception {
    long auditCount = auditEventRepository.count();
    long notificationCount = notificationEventRepository.count();
    CreateNotificationRequest request = new CreateNotificationRequest(
        FIELD_COORDINATORUser.getId(),
        NotificationChannel.IN_APP,
        "EVIDENCE_REVIEWED",
        Map.of("status", "APPROVED")
    );

    String response = mockMvc.perform(post("/api/v1/notifications")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.recipientUserId").value(FIELD_COORDINATORUser.getId().toString()))
        .andExpect(jsonPath("$.data.channel").value("IN_APP"))
        .andExpect(jsonPath("$.data.templateCode").value("EVIDENCE_REVIEWED"))
        .andExpect(jsonPath("$.data.status").value("QUEUED"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String notificationId = jsonMapper.readTree(response).get("data").get("id").asText();

    mockMvc.perform(get("/api/v1/notifications?status=QUEUED")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].id").value(notificationId));

    mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new UpdateNotificationStatusRequest(
                NotificationStatus.SENT
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SENT"))
        .andExpect(jsonPath("$.data.sentAt").isString());

    assertThat(notificationEventRepository.count()).isEqualTo(notificationCount + 1);
    assertThat(auditEventRepository.count()).isEqualTo(auditCount + 2);
  }

  @Test
  void testRejectsOtherTenantRecipient() throws Exception {
    TenantEntity otherTenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity otherRole = roleRepository.save(TestDataFactory.role(otherTenant, Role.FIELD_COORDINATOR));
    UserEntity otherUser = userRepository.save(TestDataFactory.user(
        otherTenant,
        "other-" + UUID.randomUUID(),
        passwordEncoder.encode("FIELD_COORDINATOR123"),
        "Other User",
        otherRole
    ));

    mockMvc.perform(post("/api/v1/notifications")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(new CreateNotificationRequest(
                otherUser.getId(),
                NotificationChannel.IN_APP,
                "EVIDENCE_REVIEWED",
                Map.of()
            ))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void testFIELD_COORDINATORCannotManageNotifications() throws Exception {
    mockMvc.perform(get("/api/v1/notifications")
            .header("Authorization", "Bearer " + FIELD_COORDINATORToken))
        .andExpect(status().isForbidden());
  }
}
