package com.activityplatform.backend.fpo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.fpo.api.CropCatalogRequest;
import com.activityplatform.backend.fpo.api.CropCatalogResponse;
import com.activityplatform.backend.fpo.api.CropPlanRequest;
import com.activityplatform.backend.fpo.api.CropSeasonRequest;
import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.CropCatalogRepository;
import com.activityplatform.backend.fpo.repository.CropSeasonRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FarmerCropHistoryRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CropPlanningServiceTest {
  private final AuditEventService auditEventService = mock(AuditEventService.class);
  private final CropCatalogRepository cropRepository = mock(CropCatalogRepository.class);
  private final CropSeasonRepository seasonRepository = mock(CropSeasonRepository.class);
  private final FarmPlotRepository plotRepository = mock(FarmPlotRepository.class);
  private final FarmerCropHistoryRepository cropHistoryRepository =
      mock(FarmerCropHistoryRepository.class);
  private final FpoMemberProfileRepository memberRepository =
      mock(FpoMemberProfileRepository.class);
  private final SeasonalCropPlanRepository cropPlanRepository =
      mock(SeasonalCropPlanRepository.class);
  private final TenantModuleService tenantModuleService = mock(TenantModuleService.class);
  private final TenantRepository tenantRepository = mock(TenantRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);

  private final UUID tenantId = UUID.randomUUID();
  private final TenantEntity tenant = new TenantEntity(
      tenantId,
      "basecraft-fpo",
      "BaseCraft FPO",
      "ACTIVE",
      Instant.now()
  );
  private CropPlanningService service;

  @BeforeEach
  void setUp() {
    service = new CropPlanningService(
        auditEventService,
        cropRepository,
        seasonRepository,
        plotRepository,
        cropHistoryRepository,
        memberRepository,
        cropPlanRepository,
        tenantModuleService,
        tenantRepository,
        userRepository
    );
  }

  @Test
  void createCropNormalizesCodeAndDefaultsStatus() {
    CurrentUser admin = currentUser(Role.ADMIN);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(cropRepository.save(any(CropCatalogEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CropCatalogResponse response = service.createCrop(
        admin,
        new CropCatalogRequest(" tom ", " Tomato ", " Vegetable ", null)
    );

    assertThat(response.code()).isEqualTo("TOM");
    assertThat(response.name()).isEqualTo("Tomato");
    assertThat(response.category()).isEqualTo("Vegetable");
    assertThat(response.status()).isEqualTo(FarmRecordStatus.ACTIVE);
    verify(auditEventService).record(
        eq(tenant),
        eq(null),
        eq("FPO_CROP"),
        eq(response.id()),
        eq(AuditAction.FPO_CROP_CREATED),
        eq(Map.of("code", "TOM", "status", "ACTIVE"))
    );
  }

  @Test
  void createSeasonRejectsPartialMonthWindow() {
    CurrentUser admin = currentUser(Role.ADMIN);

    assertThatThrownBy(() -> service.createSeason(
        admin,
        new CropSeasonRequest(
            "KHA",
            "Kharif",
            6,
            null,
            2026,
            FarmRecordStatus.ACTIVE
        )
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("both start month and end month");
    verify(seasonRepository, never()).save(any());
  }

  @Test
  void createCropPlanRejectsPlannedAreaAboveSelectedPlotArea() {
    CurrentUser admin = currentUser(Role.ADMIN);
    UserEntity participant = user(UUID.randomUUID());
    FpoMemberProfileEntity member = member(UUID.randomUUID(), participant);
    CropCatalogEntity crop = crop(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    CropSeasonEntity season = season(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    FarmPlotEntity plot = plot(UUID.randomUUID(), member, new BigDecimal("1.0000"));
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));
    when(cropRepository.findByIdAndTenantId(crop.getId(), tenantId))
        .thenReturn(Optional.of(crop));
    when(seasonRepository.findByIdAndTenantId(season.getId(), tenantId))
        .thenReturn(Optional.of(season));
    when(plotRepository.findByIdAndTenantId(plot.getId(), tenantId))
        .thenReturn(Optional.of(plot));

    assertThatThrownBy(() -> service.createCropPlan(
        admin,
        new CropPlanRequest(
            member.getId(),
            plot.getId(),
            crop.getId(),
            season.getId(),
            "2026-27",
            new BigDecimal("1.5000"),
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 9, 30),
            null,
            CropPlanStatus.DRAFT
        )
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Planned acreage cannot exceed selected plot area");
    verify(cropPlanRepository, never()).save(any(SeasonalCropPlanEntity.class));
  }

  @Test
  void createCropPlanRejectsHarvestDateBeforeSowingDate() {
    CurrentUser admin = currentUser(Role.ADMIN);
    UserEntity participant = user(UUID.randomUUID());
    FpoMemberProfileEntity member = member(UUID.randomUUID(), participant);
    CropCatalogEntity crop = crop(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    CropSeasonEntity season = season(UUID.randomUUID(), FarmRecordStatus.ACTIVE);
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));
    when(cropRepository.findByIdAndTenantId(crop.getId(), tenantId))
        .thenReturn(Optional.of(crop));
    when(seasonRepository.findByIdAndTenantId(season.getId(), tenantId))
        .thenReturn(Optional.of(season));

    assertThatThrownBy(() -> service.createCropPlan(
        admin,
        new CropPlanRequest(
            member.getId(),
            null,
            crop.getId(),
            season.getId(),
            "2026-27",
            new BigDecimal("0.7500"),
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 7, 30),
            null,
            CropPlanStatus.DRAFT
        )
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Expected harvest date cannot be before planned sowing date");
    verify(cropPlanRepository, never()).save(any());
  }

  private CurrentUser currentUser(Role role) {
    return new CurrentUser(UUID.randomUUID(), tenantId, role.name().toLowerCase(), Set.of(role));
  }

  private UserEntity user(UUID userId) {
    return new UserEntity(
        userId,
        tenant,
        "user-" + userId,
        "hash",
        "FPO Member",
        "+919000000000",
        "Rampur",
        "North Block",
        "ACTIVE",
        Instant.now()
    );
  }

  private FpoMemberProfileEntity member(UUID memberId, UserEntity user) {
    return new FpoMemberProfileEntity(
        memberId,
        tenant,
        user,
        "MEM-" + memberId.toString().substring(0, 8),
        user.getDisplayName(),
        user.getPhone(),
        null,
        null,
        "Rampur",
        "North Block",
        "District",
        "Maharashtra",
        null,
        null,
        null,
        "SMALL",
        null,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    );
  }

  private CropCatalogEntity crop(UUID cropId, FarmRecordStatus status) {
    return new CropCatalogEntity(
        cropId,
        tenant,
        "TOM",
        "Tomato",
        "Vegetable",
        status,
        Instant.now()
    );
  }

  private CropSeasonEntity season(UUID seasonId, FarmRecordStatus status) {
    return new CropSeasonEntity(
        seasonId,
        tenant,
        "KHA",
        "Kharif",
        6,
        9,
        2026,
        status,
        Instant.now()
    );
  }

  private FarmPlotEntity plot(
      UUID plotId,
      FpoMemberProfileEntity member,
      BigDecimal areaAcres
  ) {
    return new FarmPlotEntity(
        plotId,
        tenant,
        member,
        null,
        "North plot",
        areaAcres,
        null,
        null,
        "Loam",
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }
}
