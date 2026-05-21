package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.carbon.domain.CarbonActivityRecordEntity;
import com.activityplatform.backend.carbon.domain.CarbonFarmPlotEntity;
import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import java.util.List;

record CarbonReportDataset(
    List<CarbonProfileEntity> profiles,
    List<CarbonFarmPlotEntity> plots,
    List<CarbonSoilProfileEntity> soilProfiles,
    List<CarbonActivityRecordEntity> activities
) {
  CarbonReportDataset {
    profiles = profiles == null ? List.of() : List.copyOf(profiles);
    plots = plots == null ? List.of() : List.copyOf(plots);
    soilProfiles = soilProfiles == null ? List.of() : List.copyOf(soilProfiles);
    activities = activities == null ? List.of() : List.copyOf(activities);
  }
}
