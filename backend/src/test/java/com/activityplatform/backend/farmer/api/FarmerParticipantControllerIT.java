package com.activityplatform.backend.farmer.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FarmerParticipantControllerIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FarmerService farmerService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String farmerToken;
  private UUID activeFarmerUserId;

  @BeforeEach
  void setup() {
    TenantEntity tenant = tenantRepository.save(
        TestDataFactory.tenant("tenant-" + UUID.randomUUID())
    );
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));
    RoleEntity fieldCoordinatorRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );

    UserEntity adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Admin User",
        adminRole
    ));
    UserEntity activeFarmer = userRepository.save(TestDataFactory.user(
        tenant,
        "active-farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Active Farmer",
        farmerRole
    ));
    UserEntity inactiveFarmer = userRepository.save(TestDataFactory.user(
        tenant,
        "inactive-farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Inactive Farmer",
        farmerRole
    ));
    userRepository.save(TestDataFactory.user(
        tenant,
        "field-coordinator-" + UUID.randomUUID(),
        passwordEncoder.encode("password123"),
        "Field Coordinator",
        fieldCoordinatorRole
    ));

    CurrentUser currentAdmin = new CurrentUser(
        adminUser.getId(),
        tenant.getId(),
        adminUser.getUsername(),
        Set.of(Role.ADMIN)
    );
    farmerService.createFarmerProfile(
        currentAdmin,
        activeFarmer,
        command("Active Farmer", "9999900000", FarmerProfileStatus.ACTIVE)
    );
    farmerService.createFarmerProfile(
        currentAdmin,
        inactiveFarmer,
        command("Inactive Farmer", "8888800000", FarmerProfileStatus.INACTIVE)
    );

    activeFarmerUserId = activeFarmer.getId();
    adminToken = jwtService.issueTokens(adminUser).accessToken();
    farmerToken = jwtService.issueTokens(activeFarmer).accessToken();
  }

  @Test
  void testStaffCanListCanonicalActiveFarmerParticipants() throws Exception {
    mockMvc.perform(get("/api/v1/farmers/participants")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].userId").value(activeFarmerUserId.toString()))
        .andExpect(jsonPath("$.data[0].displayName").value("Active Farmer"))
        .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
  }

  @Test
  void testFarmerCannotListAllParticipants() throws Exception {
    mockMvc.perform(get("/api/v1/farmers/participants")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isForbidden());
  }

  private FarmerProfileCommand command(
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
