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
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.fpo.api.FpoSoilProfileRequest;
import com.activityplatform.backend.fpo.api.FpoSoilProfileResponse;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.domain.FpoSoilProfileEntity;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.FpoSoilProfileRepository;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FpoSoilProfileServiceTest {
  private final AuditEventService auditEventService = mock(AuditEventService.class);
  private final FpoMemberProfileRepository memberRepository =
      mock(FpoMemberProfileRepository.class);
  private final FpoSoilProfileRepository soilProfileRepository =
      mock(FpoSoilProfileRepository.class);
  private final TenantModuleService tenantModuleService = mock(TenantModuleService.class);
  private final UserRepository userRepository = mock(UserRepository.class);

  private final UUID tenantId = UUID.randomUUID();
  private final TenantEntity tenant = new TenantEntity(
      tenantId,
      "basecraft-fpo",
      "BaseCraft FPO",
      "ACTIVE",
      Instant.now()
  );
  private FpoSoilProfileService service;

  @BeforeEach
  void setUp() {
    service = new FpoSoilProfileService(
        auditEventService,
        memberRepository,
        soilProfileRepository,
        tenantModuleService,
        userRepository
    );
  }

  @Test
  void createPersistsOptionalLabValuesAndReportLink() {
    CurrentUser admin = currentUser(Role.ADMIN);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID()));
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));
    when(soilProfileRepository.save(any(FpoSoilProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    FpoSoilProfileResponse response = service.create(
        admin,
        member.getId(),
        new FpoSoilProfileRequest(
            new BigDecimal("0.7200"),
            new BigDecimal("6.80"),
            new BigDecimal("210.0000"),
            new BigDecimal("18.5000"),
            new BigDecimal("145.2500"),
            " soil-report.pdf ",
            " application/pdf ",
            " https://storage.example.com/reports/soil-report.pdf ",
            " Existing lab report "
        )
    );

    assertThat(response.soilOrganicCarbon()).isEqualByComparingTo("0.7200");
    assertThat(response.ph()).isEqualByComparingTo("6.80");
    assertThat(response.reportFileName()).isEqualTo("soil-report.pdf");
    assertThat(response.reportContentType()).isEqualTo("application/pdf");
    assertThat(response.reportUrl()).isEqualTo(
        "https://storage.example.com/reports/soil-report.pdf"
    );
    assertThat(response.notes()).isEqualTo("Existing lab report");
    verify(auditEventService).record(
        eq(tenant),
        eq(null),
        eq("FPO_SOIL_PROFILE"),
        eq(response.id()),
        eq(AuditAction.FPO_SOIL_PROFILE_CREATED),
        eq(Map.of("memberId", member.getId().toString()))
    );
  }

  @Test
  void createAllowsBlankSoilValuesWhenNoLabReportExists() {
    CurrentUser coordinator = currentUser(Role.FIELD_COORDINATOR);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID()));
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));
    when(soilProfileRepository.save(any(FpoSoilProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    FpoSoilProfileResponse response = service.create(
        coordinator,
        member.getId(),
        new FpoSoilProfileRequest(null, null, null, null, null, null, null, null, null)
    );

    assertThat(response.soilOrganicCarbon()).isNull();
    assertThat(response.ph()).isNull();
    assertThat(response.reportUrl()).isNull();
  }

  @Test
  void createRejectsNonHttpReportUrl() {
    CurrentUser admin = currentUser(Role.ADMIN);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID()));
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));

    assertThatThrownBy(() -> service.create(
        admin,
        member.getId(),
        new FpoSoilProfileRequest(null, null, null, null, null, null, null, "s3://bucket/key", null)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Soil report URL must start with http or https");
    verify(soilProfileRepository, never()).save(any());
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
        "9000000000",
        "Wagholi",
        "Haveli",
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
        "Wagholi",
        "Haveli",
        "Pune",
        "Maharashtra",
        "MALE",
        null,
        null,
        "SMALL",
        null,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    );
  }
}
