package com.activityplatform.backend.carbon.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.activityplatform.backend.TestDataFactory;
import com.activityplatform.backend.TestcontainersConfiguration;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.auth.service.JwtService;
import com.activityplatform.backend.carbon.domain.CarbonParticipantType;
import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import com.activityplatform.backend.carbon.repository.CarbonProfileRepository;
import com.activityplatform.backend.farmer.api.FarmerBankDetailsRequest;
import com.activityplatform.backend.farmer.api.FarmerBankDetailsResponse;
import com.activityplatform.backend.farmer.api.FarmerBankDetailsVerificationRequest;
import com.activityplatform.backend.farmer.domain.FarmerBankDetailsStatus;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import com.activityplatform.backend.farmer.repository.FarmerProfileRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.domain.PlatformModuleEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionStatus;
import com.activityplatform.backend.platform.repository.PlatformModuleRepository;
import com.activityplatform.backend.platform.repository.TenantModuleSubscriptionRepository;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CarbonProfileControllerIT {
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
  private CarbonProfileRepository profileRepository;

  @Autowired
  private FarmerProfileRepository farmerProfileRepository;

  @Autowired
  private PlatformModuleRepository platformModuleRepository;

  @Autowired
  private TenantModuleSubscriptionRepository subscriptionRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private TenantEntity tenant;
  private UserEntity adminUser;
  private UserEntity coordinatorUser;
  private UserEntity farmerUser;
  private String adminToken;
  private String coordinatorToken;
  private String farmerToken;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity coordinatorRole = roleRepository.save(
        TestDataFactory.role(tenant, Role.FIELD_COORDINATOR)
    );
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));

    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Admin User",
        adminRole
    ));
    coordinatorUser = userRepository.save(TestDataFactory.user(
        tenant,
        "coordinator-" + UUID.randomUUID(),
        passwordEncoder.encode("coordinator12345"),
        "Coordinator User",
        coordinatorRole
    ));
    farmerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "Carbon Farmer",
        farmerRole
    ));
    enableModule(tenant, ModuleCode.SUSTAINABILITY);

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    coordinatorToken = jwtService.issueTokens(coordinatorUser).accessToken();
    farmerToken = jwtService.issueTokens(farmerUser).accessToken();
  }

  @Test
  void testAdminCanManageCarbonProfilePlotAndSoilProfile() throws Exception {
    CarbonProfileResponse profile = createProfile(
        adminToken,
        "CAR-UAT-" + UUID.randomUUID().toString().substring(0, 8)
    );
    FarmerProfileEntity farmerProfile = farmerProfileRepository
        .findByTenantIdAndUserId(tenant.getId(), farmerUser.getId())
        .orElseThrow();
    CarbonProfileEntity savedProfile = profileRepository.findById(profile.id()).orElseThrow();
    assertThat(savedProfile.getFarmerProfileId()).isEqualTo(farmerProfile.getId());

    mockMvc.perform(get("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + adminToken)
            .param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id").value(profile.id().toString()));

    mockMvc.perform(get("/api/v1/carbon/profiles/me")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(profile.id().toString()));

    mockMvc.perform(get("/api/v1/farmer/profile/completion")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.carbonProfileId").value(profile.id().toString()))
        .andExpect(jsonPath("$.data.completionPercentage").value(67))
        .andExpect(jsonPath("$.data.completedRequiredSteps").value(2))
        .andExpect(jsonPath("$.data.totalRequiredSteps").value(3))
        .andExpect(jsonPath("$.data.steps[2].code").value("BANK_DETAILS"))
        .andExpect(jsonPath("$.data.steps[2].status").value("INCOMPLETE"))
        .andExpect(jsonPath("$.data.steps[2].required").value(true))
        .andExpect(jsonPath("$.data.steps[3].code").value("DOCUMENTS"))
        .andExpect(jsonPath("$.data.steps[3].status").value("COMING_SOON"));

    FarmerBankDetailsRequest bankDetailsRequest = new FarmerBankDetailsRequest(
        "Carbon Farmer",
        "123456789012",
        "hdfc0001234",
        "carbonfarmer@upi",
        "HDFC Bank"
    );
    String bankDetailsResponse = mockMvc.perform(post("/api/v1/farmer/bank-details")
            .header("Authorization", "Bearer " + farmerToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(bankDetailsRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.farmerProfileId").value(farmerProfile.getId().toString()))
        .andExpect(jsonPath("$.data.ifscCode").value("HDFC0001234"))
        .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    FarmerBankDetailsResponse bankDetails = readData(
        bankDetailsResponse,
        FarmerBankDetailsResponse.class
    );

    mockMvc.perform(get("/api/v1/farmer/bank-details")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(bankDetails.id().toString()))
        .andExpect(jsonPath("$.data.accountNumber").value("123456789012"));

    FarmerBankDetailsRequest updatedBankDetailsRequest = new FarmerBankDetailsRequest(
        "Updated Carbon Farmer",
        "998877665544",
        "ICIC0004567",
        null,
        "ICICI Bank"
    );
    mockMvc.perform(put("/api/v1/farmer/bank-details/" + bankDetails.id())
            .header("Authorization", "Bearer " + farmerToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updatedBankDetailsRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accountHolderName").value("Updated Carbon Farmer"))
        .andExpect(jsonPath("$.data.accountNumber").value("998877665544"))
        .andExpect(jsonPath("$.data.ifscCode").value("ICIC0004567"))
        .andExpect(jsonPath("$.data.upiId").doesNotExist())
        .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

    mockMvc.perform(get("/api/v1/admin/bank-details/pending")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(bankDetails.id().toString()))
        .andExpect(jsonPath("$.data[0].farmerName").value("Carbon Farmer"))
        .andExpect(jsonPath("$.data[0].farmerMobileNumber").value("9876543210"))
        .andExpect(jsonPath("$.data[0].accountNumber").value("998877665544"))
        .andExpect(jsonPath("$.data[0].status").value("PENDING_VERIFICATION"));

    FarmerBankDetailsVerificationRequest verificationRequest =
        new FarmerBankDetailsVerificationRequest(
            FarmerBankDetailsStatus.VERIFIED,
            "Verified for UAT."
        );
    mockMvc.perform(put("/api/v1/admin/bank-details/" + bankDetails.id() + "/verify")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(verificationRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("VERIFIED"))
        .andExpect(jsonPath("$.data.verifiedByUserId").value(adminUser.getId().toString()))
        .andExpect(jsonPath("$.data.verificationNotes").value("Verified for UAT."))
        .andExpect(jsonPath("$.data.verifiedAt").exists());

    mockMvc.perform(get("/api/v1/farmer/bank-details")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(bankDetails.id().toString()))
        .andExpect(jsonPath("$.data.status").value("VERIFIED"));

    mockMvc.perform(get("/api/v1/farmer/profile/completion")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.carbonProfileId").value(profile.id().toString()))
        .andExpect(jsonPath("$.data.completionPercentage").value(100))
        .andExpect(jsonPath("$.data.completedRequiredSteps").value(3))
        .andExpect(jsonPath("$.data.totalRequiredSteps").value(3))
        .andExpect(jsonPath("$.data.steps[2].code").value("BANK_DETAILS"))
        .andExpect(jsonPath("$.data.steps[2].status").value("COMPLETE"))
        .andExpect(jsonPath("$.data.steps[3].code").value("DOCUMENTS"))
        .andExpect(jsonPath("$.data.steps[3].status").value("COMING_SOON"));

    CarbonProfileRequest updateProfileRequest = profileRequest(
        profile.carbonIdentityId(),
        "Updated Carbon Farmer",
        farmerUser.getId(),
        coordinatorUser.getId(),
        new BigDecimal("2.7500")
    );
    mockMvc.perform(put("/api/v1/carbon/profiles/" + profile.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateProfileRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName").value("Updated Carbon Farmer"))
        .andExpect(jsonPath("$.data.totalLandHoldingAcres").value(2.7500));
    FarmerProfileEntity updatedFarmerProfile = farmerProfileRepository
        .findByTenantIdAndUserId(tenant.getId(), farmerUser.getId())
        .orElseThrow();
    assertThat(updatedFarmerProfile.getDisplayName()).isEqualTo("Updated Carbon Farmer");
    assertThat(updatedFarmerProfile.getMobileNumber()).isEqualTo("9876543210");
    assertThat(updatedFarmerProfile.getStatus()).isEqualTo(FarmerProfileStatus.ACTIVE);

    CarbonFarmPlotRequest plotRequest = new CarbonFarmPlotRequest(
        "Wagholi demo plot",
        "SUR-101",
        new BigDecimal("1.2500"),
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        "Drip",
        "Paddy",
        "Reduced tillage",
        CarbonRecordStatus.ACTIVE
    );
    String plotResponse = mockMvc.perform(post("/api/v1/carbon/profiles/" + profile.id() + "/plots")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(plotRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.carbonProfileId").value(profile.id().toString()))
        .andExpect(jsonPath("$.data.primaryCrop").value("Paddy"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonFarmPlotResponse plot = readData(plotResponse, CarbonFarmPlotResponse.class);

    CarbonFarmPlotRequest updatePlotRequest = new CarbonFarmPlotRequest(
        "Wagholi demo plot",
        "SUR-101",
        new BigDecimal("1.2500"),
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        "Drip",
        "Wheat",
        "Reduced tillage",
        CarbonRecordStatus.ACTIVE
    );
    mockMvc.perform(put("/api/v1/carbon/plots/" + plot.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updatePlotRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.primaryCrop").value("Wheat"));

    mockMvc.perform(get("/api/v1/carbon/activity-categories")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(8))
        .andExpect(jsonPath("$.data[0].code").value("LAND_PREPARATION"));

    UUID compostCategoryId = UUID.fromString("00000000-0000-0000-0000-000000000306");
    CarbonActivityRecordRequest activityRequest = new CarbonActivityRecordRequest(
        plot.id(),
        compostCategoryId,
        LocalDate.of(2026, 5, 16),
        "Paddy",
        "Farmyard compost",
        new BigDecimal("25.0000"),
        "kg",
        "Applied compost before sowing.",
        CarbonRecordStatus.ACTIVE
    );
    String activityResponse = mockMvc.perform(post("/api/v1/carbon/profiles/"
            + profile.id() + "/activities")
            .header("Authorization", "Bearer " + farmerToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(activityRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.categoryName").value("Compost Addition"))
        .andExpect(jsonPath("$.data.evidenceCount").value(0))
        .andExpect(jsonPath("$.data.verificationStatus").value("PENDING_EVIDENCE"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonActivityRecordResponse activity = readData(
        activityResponse,
        CarbonActivityRecordResponse.class
    );

    CarbonActivityRecordRequest updateActivityRequest = new CarbonActivityRecordRequest(
        plot.id(),
        compostCategoryId,
        LocalDate.of(2026, 5, 16),
        "Paddy",
        "Farmyard compost",
        new BigDecimal("30.0000"),
        "kg",
        "Updated after supervisor review.",
        CarbonRecordStatus.ACTIVE
    );
    mockMvc.perform(put("/api/v1/carbon/activities/" + activity.id())
            .header("Authorization", "Bearer " + coordinatorToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateActivityRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.quantityValue").value(30.0000))
        .andExpect(jsonPath("$.data.remarks").value("Updated after supervisor review."));

    mockMvc.perform(get("/api/v1/carbon/profiles/" + profile.id() + "/activities")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(activity.id().toString()));

    CarbonSoilProfileRequest soilRequest = new CarbonSoilProfileRequest(
        plot.id(),
        LocalDate.of(2026, 5, 15),
        "Pune Soil Lab",
        new BigDecimal("0.6800"),
        new BigDecimal("7.20"),
        null,
        new BigDecimal("180.0000"),
        new BigDecimal("22.5000"),
        new BigDecimal("132.0000"),
        null,
        "Clay loam",
        "wagholi-soil.pdf",
        "application/pdf",
        "carbon/soil/wagholi-soil.pdf",
        "https://storage.example.com/carbon/soil/wagholi-soil.pdf",
        CarbonRecordStatus.ACTIVE
    );
    String soilResponse = mockMvc.perform(post("/api/v1/carbon/profiles/" + profile.id()
            + "/soil-profiles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(soilRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.carbonProfileId").value(profile.id().toString()))
        .andExpect(jsonPath("$.data.carbonFarmPlotId").value(plot.id().toString()))
        .andExpect(jsonPath("$.data.soilOrganicCarbonPercent").value(0.6800))
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonSoilProfileResponse soilProfile = readData(soilResponse, CarbonSoilProfileResponse.class);

    CarbonSoilProfileRequest updateSoilRequest = new CarbonSoilProfileRequest(
        plot.id(),
        LocalDate.of(2026, 5, 15),
        "Pune Soil Lab",
        new BigDecimal("0.7200"),
        new BigDecimal("6.90"),
        null,
        new BigDecimal("180.0000"),
        new BigDecimal("22.5000"),
        new BigDecimal("132.0000"),
        null,
        "Clay loam",
        "wagholi-soil-updated.pdf",
        "application/pdf",
        "carbon/soil/wagholi-soil-updated.pdf",
        "https://storage.example.com/carbon/soil/wagholi-soil-updated.pdf",
        CarbonRecordStatus.ACTIVE
    );
    mockMvc.perform(put("/api/v1/carbon/soil-profiles/" + soilProfile.id())
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(updateSoilRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.soilOrganicCarbonPercent").value(0.7200))
        .andExpect(jsonPath("$.data.ph").value(6.90));

    MockMultipartFile soilReport = new MockMultipartFile(
        "file",
        "soil-lab-report.pdf",
        "application/pdf",
        "soil report content".getBytes(StandardCharsets.UTF_8)
    );
    mockMvc.perform(multipart("/api/v1/carbon/soil-profiles/" + soilProfile.id() + "/report")
            .file(soilReport)
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reportFileName").value("soil-lab-report.pdf"))
        .andExpect(jsonPath("$.data.reportContentType").value("application/pdf"))
        .andExpect(jsonPath("$.data.reportStorageKey").value(startsWith(
            tenant.getId() + "/carbon-soil-report/" + soilProfile.id() + "/"
        )));

    MockMultipartFile farmerUploadAttempt = new MockMultipartFile(
        "file",
        "farmer-soil-report.pdf",
        "application/pdf",
        "soil report content".getBytes(StandardCharsets.UTF_8)
    );
    mockMvc.perform(multipart("/api/v1/carbon/soil-profiles/" + soilProfile.id() + "/report")
            .file(farmerUploadAttempt)
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/carbon/profiles/" + profile.id() + "/soil-profiles")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(soilProfile.id().toString()))
        .andExpect(jsonPath("$.data[0].reportFileName").value("soil-lab-report.pdf"));

  }

  @Test
  void testCarbonFarmerEnrollmentCreatesFarmerLogin() throws Exception {
    String username = "carbon-farmer-" + UUID.randomUUID();
    String password = "farmerPass123";
    String carbonIdentityId = "CAR-LOGIN-" + UUID.randomUUID().toString().substring(0, 8);
    CarbonProfileRequest request = new CarbonProfileRequest(
        null,
        null,
        coordinatorUser.getId(),
        username,
        password,
        carbonIdentityId + "-MEMBER",
        carbonIdentityId,
        CarbonParticipantType.FARMER,
        "Carbon Login Farmer",
        "9876543211",
        "9876543212",
        "123412341234",
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "FEMALE",
        35,
        "SMALL",
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        new BigDecimal("2.5000"),
        "Paddy and wheat",
        2,
        "Reduced tillage",
        "Linked",
        "Provided",
        "Partial",
        CarbonRecordStatus.ACTIVE
    );

    String response = mockMvc.perform(post("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value(username))
        .andExpect(jsonPath("$.data.memberNumber").value(carbonIdentityId + "-MEMBER"))
        .andExpect(jsonPath("$.data.gender").value("FEMALE"))
        .andExpect(jsonPath("$.data.farmerCategory").value("SMALL"))
        .andExpect(jsonPath("$.data.userId").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonProfileResponse profile = readData(response, CarbonProfileResponse.class);

    UserEntity createdUser = userRepository
        .findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(tenant.getCode(), username)
        .orElseThrow();

    assertTrue(passwordEncoder.matches(password, createdUser.getPasswordHash()));
    assertTrue(createdUser.getRoles().stream().anyMatch(role -> Role.FARMER.name().equals(role.getCode())));
    FarmerProfileEntity farmerProfile = farmerProfileRepository
        .findByTenantIdAndUserId(tenant.getId(), createdUser.getId())
        .orElseThrow();
    CarbonProfileEntity savedProfile = profileRepository.findById(profile.id()).orElseThrow();
    assertThat(savedProfile.getFarmerProfileId()).isEqualTo(farmerProfile.getId());
    assertThat(farmerProfile.getDisplayName()).isEqualTo("Carbon Login Farmer");
  }

  @Test
  void testFieldCoordinatorCannotReadUnassignedProfile() throws Exception {
    CarbonProfileResponse assigned = createProfile(
        adminToken,
        "CAR-FC-" + UUID.randomUUID().toString().substring(0, 8)
    );

    mockMvc.perform(get("/api/v1/carbon/profiles/" + assigned.id())
            .header("Authorization", "Bearer " + coordinatorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(assigned.id().toString()));

    CarbonProfileRequest unassignedRequest = profileRequest(
        "CAR-OPEN-" + UUID.randomUUID().toString().substring(0, 8),
        "Unassigned Farmer",
        farmerUser.getId(),
        null,
        new BigDecimal("1.5000")
    );
    String response = mockMvc.perform(post("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(unassignedRequest)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    CarbonProfileResponse unassigned = readData(response, CarbonProfileResponse.class);

    mockMvc.perform(get("/api/v1/carbon/profiles/" + unassigned.id())
            .header("Authorization", "Bearer " + coordinatorToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  @Test
  void testCarbonProfileApisRequireSustainabilityModule() throws Exception {
    TenantEntity disabledTenant = tenantRepository.save(
        TestDataFactory.tenant("disabled-" + UUID.randomUUID())
    );
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(disabledTenant, Role.ADMIN));
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(disabledTenant, Role.FARMER));
    UserEntity disabledAdmin = userRepository.save(TestDataFactory.user(
        disabledTenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Disabled Admin",
        adminRole
    ));
    UserEntity disabledFarmer = userRepository.save(TestDataFactory.user(
        disabledTenant,
        "farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "Disabled Farmer",
        farmerRole
    ));
    farmerProfileRepository.save(new FarmerProfileEntity(
        UUID.randomUUID(),
        disabledTenant,
        disabledFarmer,
        "Disabled Farmer",
        "9876543210",
        null,
        null,
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        null,
        42,
        "SMALL",
        FarmerProfileStatus.ACTIVE,
        disabledAdmin,
        Instant.now()
    ));
    String disabledToken = jwtService.issueTokens(disabledAdmin).accessToken();
    String disabledFarmerToken = jwtService.issueTokens(disabledFarmer).accessToken();

    mockMvc.perform(get("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + disabledToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));

    mockMvc.perform(get("/api/v1/farmer/profile/completion")
            .header("Authorization", "Bearer " + disabledFarmerToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));

    mockMvc.perform(get("/api/v1/farmer/bank-details")
            .header("Authorization", "Bearer " + disabledFarmerToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));

    mockMvc.perform(get("/api/v1/admin/bank-details/pending")
            .header("Authorization", "Bearer " + disabledToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("MODULE_NOT_ENABLED"));
  }

  private CarbonProfileResponse createProfile(String token, String carbonIdentityId) throws Exception {
    String response = mockMvc.perform(post("/api/v1/carbon/profiles")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content(jsonMapper.writeValueAsString(profileRequest(
                carbonIdentityId,
                "Carbon Farmer",
                farmerUser.getId(),
                coordinatorUser.getId(),
                new BigDecimal("2.5000")
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(farmerUser.getId().toString()))
        .andExpect(jsonPath("$.data.coordinatorUserId").value(coordinatorUser.getId().toString()))
        .andExpect(jsonPath("$.data.carbonIdentityId").value(carbonIdentityId))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return readData(response, CarbonProfileResponse.class);
  }

  private CarbonProfileRequest profileRequest(
      String carbonIdentityId,
      String displayName,
      UUID userId,
      UUID coordinatorUserId,
      BigDecimal landHolding
  ) {
    return new CarbonProfileRequest(
        userId,
        null,
        coordinatorUserId,
        null,
        null,
        carbonIdentityId + "-MEMBER",
        carbonIdentityId,
        CarbonParticipantType.FARMER,
        displayName,
        "9876543210",
        null,
        null,
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        42,
        "SMALL",
        new BigDecimal("18.5800000"),
        new BigDecimal("73.9800000"),
        landHolding,
        "Paddy and wheat",
        2,
        "Reduced tillage",
        "Linked",
        "Optional not captured",
        "Partial",
        CarbonRecordStatus.ACTIVE
    );
  }

  private void enableModule(TenantEntity moduleTenant, ModuleCode moduleCode) {
    PlatformModuleEntity module = platformModuleRepository.findByCode(moduleCode)
        .orElseThrow();
    Instant now = Instant.now();
    subscriptionRepository.save(new TenantModuleSubscriptionEntity(
        UUID.randomUUID(),
        moduleTenant,
        module,
        TenantModuleSubscriptionStatus.ENABLED,
        now,
        null,
        null,
        now
    ));
  }

  private <T> T readData(String response, Class<T> responseType) throws Exception {
    return jsonMapper.readValue(jsonMapper.readTree(response).get("data").toString(), responseType);
  }
}
