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
import com.activityplatform.backend.fpo.api.CreateFarmLandholdingRequest;
import com.activityplatform.backend.fpo.api.CreateFarmPlotRequest;
import com.activityplatform.backend.fpo.api.FarmLandholdingResponse;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
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

class FarmAssetServiceTest {
  private final AuditEventService auditEventService = mock(AuditEventService.class);
  private final FarmLandholdingRepository landholdingRepository =
      mock(FarmLandholdingRepository.class);
  private final FarmPlotRepository plotRepository = mock(FarmPlotRepository.class);
  private final FpoMemberProfileRepository memberRepository =
      mock(FpoMemberProfileRepository.class);
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
  private FarmAssetService service;

  @BeforeEach
  void setUp() {
    service = new FarmAssetService(
        auditEventService,
        landholdingRepository,
        plotRepository,
        memberRepository,
        tenantModuleService,
        userRepository
    );
  }

  @Test
  void createLandholdingPersistsTrimmedDetailsWithActiveDefault() {
    CurrentUser admin = currentUser(Role.ADMIN);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID()));
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));
    when(landholdingRepository.save(any(FarmLandholdingEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    FarmLandholdingResponse response = service.createLandholding(
        admin,
        member.getId(),
        new CreateFarmLandholdingRequest(
            " SUR-101 ",
            new BigDecimal("3.5000"),
            new BigDecimal("2.7500"),
            " Owned ",
            " Canal ",
            null
        )
    );

    assertThat(response.surveyNumber()).isEqualTo("SUR-101");
    assertThat(response.ownershipType()).isEqualTo("Owned");
    assertThat(response.irrigationSource()).isEqualTo("Canal");
    assertThat(response.status()).isEqualTo(FarmRecordStatus.ACTIVE);
    verify(auditEventService).record(
        eq(tenant),
        eq(null),
        eq("FARM_LANDHOLDING"),
        eq(response.id()),
        eq(AuditAction.FPO_LANDHOLDING_CREATED),
        eq(Map.of("memberId", member.getId().toString(), "status", "ACTIVE"))
    );
  }

  @Test
  void createLandholdingRejectsCultivableAreaAboveTotalArea() {
    CurrentUser admin = currentUser(Role.ADMIN);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID()));
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));

    assertThatThrownBy(() -> service.createLandholding(
        admin,
        member.getId(),
        new CreateFarmLandholdingRequest(
            "SUR-102",
            new BigDecimal("1.0000"),
            new BigDecimal("1.5000"),
            "Owned",
            null,
            FarmRecordStatus.ACTIVE
        )
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Cultivable area cannot exceed total area");
    verify(landholdingRepository, never()).save(any());
  }

  @Test
  void listLandholdingsRejectsParticipantWhoDoesNotOwnMemberProfile() {
    CurrentUser participant = currentUser(Role.PARTICIPANT);
    FpoMemberProfileEntity member = member(UUID.randomUUID(), user(UUID.randomUUID()));
    when(memberRepository.findByIdAndTenantId(member.getId(), tenantId))
        .thenReturn(Optional.of(member));

    assertThatThrownBy(() -> service.listLandholdings(participant, member.getId()))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("permission to view this member's farm records");
  }

  @Test
  void createPlotRejectsLandholdingOwnedByAnotherMember() {
    CurrentUser admin = currentUser(Role.ADMIN);
    FpoMemberProfileEntity selectedMember = member(UUID.randomUUID(), user(UUID.randomUUID()));
    FpoMemberProfileEntity otherMember = member(UUID.randomUUID(), user(UUID.randomUUID()));
    FarmLandholdingEntity otherLandholding = landholding(otherMember);
    when(memberRepository.findByIdAndTenantId(selectedMember.getId(), tenantId))
        .thenReturn(Optional.of(selectedMember));
    when(landholdingRepository.findByIdAndTenantId(otherLandholding.getId(), tenantId))
        .thenReturn(Optional.of(otherLandholding));

    assertThatThrownBy(() -> service.createPlot(
        admin,
        selectedMember.getId(),
        new CreateFarmPlotRequest(
            otherLandholding.getId(),
            "North plot",
            new BigDecimal("0.7500"),
            null,
            null,
            "Loam",
            FarmRecordStatus.ACTIVE
        )
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Landholding must belong to the selected FPO member");
    verify(plotRepository, never()).save(any());
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
        "Rampur",
        "North Block",
        "District",
        null,
        null,
        null,
        "SMALL",
        null,
        FpoMemberStatus.ACTIVE,
        Instant.now()
    );
  }

  private FarmLandholdingEntity landholding(FpoMemberProfileEntity member) {
    return new FarmLandholdingEntity(
        UUID.randomUUID(),
        tenant,
        member,
        "SUR-200",
        new BigDecimal("2.0000"),
        new BigDecimal("1.7500"),
        "Owned",
        null,
        FarmRecordStatus.ACTIVE,
        Instant.now()
    );
  }
}
