package com.activityplatform.backend.fpo.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.domain.PlatformModuleEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionStatus;
import com.activityplatform.backend.platform.repository.PlatformModuleRepository;
import com.activityplatform.backend.platform.repository.TenantModuleSubscriptionRepository;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FpoAdvisoryControllerIT {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FpoMemberProfileRepository memberRepository;

  @Autowired
  private CropCatalogRepository cropRepository;

  @Autowired
  private CropSeasonRepository seasonRepository;

  @Autowired
  private SeasonalCropPlanRepository cropPlanRepository;

  @Autowired
  private PlatformModuleRepository platformModuleRepository;

  @Autowired
  private TenantModuleSubscriptionRepository subscriptionRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  private String adminToken;
  private String farmerToken;
  private TenantEntity tenant;
  private UserEntity adminUser;
  private UserEntity farmerUser;
  private FpoMemberProfileEntity farmerProfile;

  @BeforeEach
  void setup() {
    tenant = tenantRepository.save(TestDataFactory.tenant("tenant-" + UUID.randomUUID()));
    RoleEntity adminRole = roleRepository.save(TestDataFactory.role(tenant, Role.ADMIN));
    RoleEntity farmerRole = roleRepository.save(TestDataFactory.role(tenant, Role.FARMER));

    adminUser = userRepository.save(TestDataFactory.user(
        tenant,
        "admin-" + UUID.randomUUID(),
        passwordEncoder.encode("admin12345"),
        "Admin User",
        adminRole
    ));
    farmerUser = userRepository.save(TestDataFactory.user(
        tenant,
        "farmer-" + UUID.randomUUID(),
        passwordEncoder.encode("farmer12345"),
        "Farmer User",
        farmerRole
    ));
    farmerProfile = memberRepository.save(member("MEM-1", farmerUser));
    enableModule(tenant, ModuleCode.ADVISORY);

    adminToken = jwtService.issueTokens(adminUser).accessToken();
    farmerToken = jwtService.issueTokens(farmerUser).accessToken();
  }

  @Test
  void testAdminCanCreateCropTargetAdvisoryWithImages() throws Exception {
    CropCatalogEntity crop = cropRepository.save(crop("PAD", "Paddy"));
    CropSeasonEntity season = seasonRepository.save(season("KHA", "Kharif", 2026));

    mockMvc.perform(post("/api/v1/fpo/advisories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("""
                {
                  "category": "PEST_DISEASE_MANAGEMENT",
                  "targetType": "CROP",
                  "cropId": "%s",
                  "seasonId": "%s",
                  "title": "Paddy pest watch",
                  "message": "Scout paddy fields for early pest symptoms.",
                  "channel": "IN_APP",
                  "status": "PUBLISHED",
                  "images": [
                    {
                      "imageUrl": "https://storage.example.com/advisories/paddy-pest.jpg",
                      "originalFilename": "paddy-pest.jpg",
                      "contentType": "image/jpeg"
                    }
                  ]
                }
                """.formatted(crop.getId(), season.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.category").value("PEST_DISEASE_MANAGEMENT"))
        .andExpect(jsonPath("$.data.targetType").value("CROP"))
        .andExpect(jsonPath("$.data.cropId").value(crop.getId().toString()))
        .andExpect(jsonPath("$.data.channel").value("IN_APP"))
        .andExpect(jsonPath("$.data.images[0].imageUrl")
            .value("https://storage.example.com/advisories/paddy-pest.jpg"))
        .andExpect(jsonPath("$.data.images[0].contentType").value("image/jpeg"));
  }

  @Test
  void testFarmerSeesOnlyPublishedCropAdvisoriesForConfirmedPlans() throws Exception {
    CropCatalogEntity paddy = cropRepository.save(crop("PAD", "Paddy"));
    CropCatalogEntity wheat = cropRepository.save(crop("WHT", "Wheat"));
    CropSeasonEntity season = seasonRepository.save(season("KHA", "Kharif", 2026));
    cropPlanRepository.save(plan(paddy, season, CropPlanStatus.CONFIRMED));

    createPublishedCropAdvisory(paddy, season, "Paddy irrigation alert");
    createPublishedCropAdvisory(wheat, season, "Wheat irrigation alert");

    mockMvc.perform(get("/api/v1/fpo/advisories?status=PUBLISHED")
            .header("Authorization", "Bearer " + farmerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].title").value("Paddy irrigation alert"))
        .andExpect(jsonPath("$.data[0].targetType").value("CROP"))
        .andExpect(jsonPath("$.data[0].cropName").value("Paddy"));
  }

  @Test
  void testPhase1RejectsNonInAppAdvisoryChannel() throws Exception {
    mockMvc.perform(post("/api/v1/fpo/advisories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("""
                {
                  "category": "AGRONOMY",
                  "targetType": "ALL_MEMBERS",
                  "title": "SMS should wait",
                  "message": "Phase 1 advisory is in-app only.",
                  "channel": "SMS",
                  "status": "DRAFT"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  private void createPublishedCropAdvisory(
      CropCatalogEntity crop,
      CropSeasonEntity season,
      String title
  ) throws Exception {
    mockMvc.perform(post("/api/v1/fpo/advisories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("""
                {
                  "category": "AGRONOMY",
                  "targetType": "CROP",
                  "cropId": "%s",
                  "seasonId": "%s",
                  "title": "%s",
                  "message": "Use the advisory dashboard for field coordination.",
                  "channel": "IN_APP",
                  "status": "PUBLISHED"
                }
                """.formatted(crop.getId(), season.getId(), title)))
        .andExpect(status().isOk());
  }

  private FpoMemberProfileEntity member(String memberNumber, UserEntity user) {
    return new FpoMemberProfileEntity(
        UUID.randomUUID(),
        tenant,
        user,
        memberNumber,
        user.getDisplayName(),
        "+919999900000",
        null,
        null,
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        null,
        42,
        "MARGINAL",
        adminUser,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropCatalogEntity crop(String code, String name) {
    return new CropCatalogEntity(
        UUID.randomUUID(),
        tenant,
        code,
        name,
        "Cereals",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropSeasonEntity season(String code, String name, Integer seasonYear) {
    return new CropSeasonEntity(
        UUID.randomUUID(),
        tenant,
        code,
        name,
        6,
        10,
        seasonYear,
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }

  private SeasonalCropPlanEntity plan(
      CropCatalogEntity crop,
      CropSeasonEntity season,
      CropPlanStatus status
  ) {
    return new SeasonalCropPlanEntity(
        UUID.randomUUID(),
        tenant,
        farmerProfile,
        null,
        crop,
        season,
        "2026-27",
        new BigDecimal("1.5000"),
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 10, 15),
        null,
        status,
        Instant.now()
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
}
