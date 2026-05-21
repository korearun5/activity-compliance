package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.carbon.domain.CarbonActivityRecordEntity;
import com.activityplatform.backend.carbon.domain.CarbonFarmPlotEntity;
import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import com.activityplatform.backend.carbon.repository.CarbonActivityRecordRepository;
import com.activityplatform.backend.carbon.repository.CarbonFarmPlotRepository;
import com.activityplatform.backend.carbon.repository.CarbonProfileRepository;
import com.activityplatform.backend.carbon.repository.CarbonSoilProfileRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarbonReportDatasetService {
  private final CarbonActivityRecordRepository activityRecordRepository;
  private final CarbonFarmPlotRepository farmPlotRepository;
  private final CarbonProfileRepository profileRepository;
  private final CarbonSoilProfileRepository soilProfileRepository;

  public CarbonReportDatasetService(
      CarbonActivityRecordRepository activityRecordRepository,
      CarbonFarmPlotRepository farmPlotRepository,
      CarbonProfileRepository profileRepository,
      CarbonSoilProfileRepository soilProfileRepository
  ) {
    this.activityRecordRepository = activityRecordRepository;
    this.farmPlotRepository = farmPlotRepository;
    this.profileRepository = profileRepository;
    this.soilProfileRepository = soilProfileRepository;
  }

  @Transactional(readOnly = true)
  public CarbonReportDataset load(CurrentUser currentUser) {
    List<CarbonProfileEntity> profiles = scopedProfiles(currentUser);
    Set<UUID> profileIds = profiles.stream()
        .map(CarbonProfileEntity::getId)
        .collect(Collectors.toSet());

    List<CarbonFarmPlotEntity> plots = farmPlotRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId())
        .stream()
        .filter(plot -> profileIds.contains(plot.getCarbonProfile().getId()))
        .toList();
    List<CarbonSoilProfileEntity> soilProfiles = soilProfileRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId())
        .stream()
        .filter(soilProfile -> profileIds.contains(soilProfile.getCarbonProfile().getId()))
        .toList();
    List<CarbonActivityRecordEntity> activities = activityRecordRepository
        .findByTenantIdOrderByActivityDateDescCreatedAtDesc(currentUser.tenantId())
        .stream()
        .filter(activity -> profileIds.contains(activity.getCarbonProfile().getId()))
        .toList();

    return new CarbonReportDataset(profiles, plots, soilProfiles, activities);
  }

  private List<CarbonProfileEntity> scopedProfiles(CurrentUser currentUser) {
    if (isFieldCoordinatorOnly(currentUser)) {
      return profileRepository.findByTenantIdAndCoordinatorUserIdOrderByCreatedAtDesc(
          currentUser.tenantId(),
          currentUser.userId()
      );
    }

    return profileRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId());
  }

  private boolean isFieldCoordinatorOnly(CurrentUser currentUser) {
    return currentUser.hasAnyRole(Role.FIELD_COORDINATOR)
        && !currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER);
  }
}
