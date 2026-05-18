package com.activityplatform.backend.farmer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.activityplatform.backend.TestDataFactory;
import com.activityplatform.backend.TestcontainersConfiguration;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import com.activityplatform.backend.farmer.repository.FarmerProfileRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FarmerProfileServiceTest {
  @Autowired
  private FarmerService farmerService;

  @Autowired
  private FarmerProfileRepository farmerProfileRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  private TenantEntity tenant;
  private UserEntity adminUser;
  private UserEntity farmerUser;
  private CurrentUser currentAdmin;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));

    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        "hash",
        "Admin User",
        adminRole
    ));
    farmerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "farmer-" + UUID.randomUUID(),
        "hash",
        "Farmer User",
        farmerRole
    ));
    currentAdmin = new CurrentUser(
        adminUser.getId(),
        tenant.getId(),
        adminUser.getUsername(),
        Set.of(Role.ADMIN)
    );
  }

  @Test
  void testCreateFarmerProfileCreatesCanonicalRecordAndParticipant() {
    FarmerProfileEntity profile = farmerService.createFarmerProfile(
        currentAdmin,
        farmerUser,
        command("99999 00000", FarmerProfileStatus.ACTIVE)
    );

    assertThat(profile.getUser().getId()).isEqualTo(farmerUser.getId());
    assertThat(profile.getMobileNumber()).isEqualTo("9999900000");
    assertThat(profile.getGender()).isEqualTo("MALE");
    assertThat(profile.getFarmerCategory()).isEqualTo("MARGINAL");

    assertThat(farmerProfileRepository.findByTenantIdAndUserId(tenant.getId(), farmerUser.getId()))
        .isPresent();
    assertThat(farmerService.findParticipants(tenant.getId()))
        .extracting(FarmerParticipant::userId)
        .containsExactly(farmerUser.getId());
  }

  @Test
  void testEnsureFarmerProfileIsIdempotentForUser() {
    FarmerProfileEntity first = farmerService.ensureFarmerProfileForUser(
        currentAdmin,
        farmerUser,
        command("9999900000", FarmerProfileStatus.ACTIVE)
    );
    FarmerProfileEntity second = farmerService.ensureFarmerProfileForUser(
        currentAdmin,
        farmerUser,
        command("8888800000", FarmerProfileStatus.SUSPENDED)
    );

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(second.getMobileNumber()).isEqualTo("9999900000");
  }

  @Test
  void testCreateFarmerProfileRejectsStaffUser() {
    assertThatThrownBy(() -> farmerService.createFarmerProfile(
        currentAdmin,
        adminUser,
        command("9999900000", FarmerProfileStatus.ACTIVE)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("farmer-only");
  }

  private FarmerProfileCommand command(String mobileNumber, FarmerProfileStatus status) {
    return new FarmerProfileCommand(
        "Farmer User",
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
        "marginal",
        status
    );
  }
}
