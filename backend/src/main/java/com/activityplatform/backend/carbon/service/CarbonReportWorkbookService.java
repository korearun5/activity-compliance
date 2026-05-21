package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.carbon.api.CarbonActivityReportBreakdownResponse;
import com.activityplatform.backend.carbon.api.CarbonReportBreakdownResponse;
import com.activityplatform.backend.carbon.api.CarbonReportSummaryResponse;
import com.activityplatform.backend.carbon.domain.CarbonActivityRecordEntity;
import com.activityplatform.backend.carbon.domain.CarbonFarmPlotEntity;
import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import com.activityplatform.backend.reporting.service.SimpleXlsxWorkbookBuilder;
import com.activityplatform.backend.security.CurrentUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarbonReportWorkbookService {
  private static final String REPORT_HEADER = "&RCarbon Farming Platform - Carbon Operations";
  private static final String REPORT_FOOTER =
      "&CConfidential - Provisional operational report, not verified credit issuance";

  private final CarbonReportDatasetService datasetService;
  private final CarbonReportSummaryService summaryService;

  public CarbonReportWorkbookService(
      CarbonReportDatasetService datasetService,
      CarbonReportSummaryService summaryService
  ) {
    this.datasetService = datasetService;
    this.summaryService = summaryService;
  }

  @Transactional(readOnly = true)
  public byte[] buildWorkbook(CurrentUser currentUser, Map<String, Object> filters) {
    CarbonReportDataset filteredDataset = applyFilters(
        datasetService.load(currentUser),
        CarbonReportFilters.from(filters)
    );
    CarbonReportSummaryResponse summary = summaryService.buildSummary(
        currentUser.tenantId(),
        filteredDataset
    );

    return new SimpleXlsxWorkbookBuilder().build(List.of(
        sheet("Carbon Summary", summaryRows(summary)),
        sheet("Farmer Profiles", profileRows(filteredDataset.profiles())),
        sheet("Farm Plots", plotRows(filteredDataset.plots())),
        sheet("Soil Profiles", soilRows(filteredDataset.soilProfiles())),
        sheet("Activity Records", activityRows(filteredDataset.activities())),
        sheet("Village Breakdown", villageRows(summary.villageBreakdowns())),
        sheet("Activity Breakdown", activityBreakdownRows(summary.activityBreakdowns()))
    ));
  }

  private CarbonReportDataset applyFilters(
      CarbonReportDataset dataset,
      CarbonReportFilters filters
  ) {
    Map<UUID, List<CarbonFarmPlotEntity>> plotsByProfile = dataset.plots().stream()
        .collect(Collectors.groupingBy(plot -> plot.getCarbonProfile().getId()));
    Map<UUID, List<CarbonSoilProfileEntity>> soilByProfile = dataset.soilProfiles().stream()
        .collect(Collectors.groupingBy(soil -> soil.getCarbonProfile().getId()));
    Map<UUID, List<CarbonActivityRecordEntity>> activitiesByProfile = dataset.activities().stream()
        .collect(Collectors.groupingBy(activity -> activity.getCarbonProfile().getId()));

    List<CarbonProfileEntity> profiles = dataset.profiles().stream()
        .filter(profile -> matchesProfile(
            profile,
            filters,
            plotsByProfile.getOrDefault(profile.getId(), List.of()),
            soilByProfile.getOrDefault(profile.getId(), List.of()),
            activitiesByProfile.getOrDefault(profile.getId(), List.of())
        ))
        .toList();
    Set<UUID> profileIds = profiles.stream().map(CarbonProfileEntity::getId).collect(Collectors.toSet());

    List<CarbonFarmPlotEntity> plots = dataset.plots().stream()
        .filter(plot -> profileIds.contains(plot.getCarbonProfile().getId()))
        .filter(plot -> matchesPlot(plot, filters))
        .toList();
    List<CarbonSoilProfileEntity> soilProfiles = dataset.soilProfiles().stream()
        .filter(soil -> profileIds.contains(soil.getCarbonProfile().getId()))
        .filter(soil -> matchesSoil(soil, filters))
        .toList();
    List<CarbonActivityRecordEntity> activities = dataset.activities().stream()
        .filter(activity -> profileIds.contains(activity.getCarbonProfile().getId()))
        .filter(activity -> matchesActivity(activity, filters))
        .toList();

    return new CarbonReportDataset(profiles, plots, soilProfiles, activities);
  }

  private boolean matchesProfile(
      CarbonProfileEntity profile,
      CarbonReportFilters filters,
      List<CarbonFarmPlotEntity> plots,
      List<CarbonSoilProfileEntity> soilProfiles,
      List<CarbonActivityRecordEntity> activities
  ) {
    if (!filters.matchesVillage(profile.getVillage())) {
      return false;
    }
    boolean profileDateMatches = filters.matchesDate(
        null,
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
    boolean relatedRecordMatches = plots.stream().anyMatch(plot -> matchesPlot(plot, filters))
        || soilProfiles.stream().anyMatch(soil -> matchesSoil(soil, filters))
        || activities.stream().anyMatch(activity -> matchesActivity(activity, filters));

    if (!profileDateMatches && !relatedRecordMatches) {
      return false;
    }
    if (!filters.matchesCrop(profileCropCandidates(profile, plots, activities))) {
      return false;
    }
    if (!filters.hasVerificationStatus()) {
      return true;
    }

    return soilProfiles.stream().anyMatch(soil -> matchesSoil(soil, filters))
        || activities.stream().anyMatch(activity -> matchesActivity(activity, filters));
  }

  private boolean matchesPlot(CarbonFarmPlotEntity plot, CarbonReportFilters filters) {
    return filters.matchesCrop(Arrays.asList(plot.getPrimaryCrop(), plot.getVariety()))
        && filters.matchesDate(null, plot.getCreatedAt(), plot.getUpdatedAt());
  }

  private boolean matchesSoil(CarbonSoilProfileEntity soil, CarbonReportFilters filters) {
    return filters.matchesVerificationStatus(soil.getVerificationStatus().name())
        && filters.matchesDate(soil.getTestDate(), soil.getCreatedAt(), soil.getUpdatedAt());
  }

  private boolean matchesActivity(
      CarbonActivityRecordEntity activity,
      CarbonReportFilters filters
  ) {
    return filters.matchesCrop(Arrays.asList(activity.getCropName(), activity.getCategory().getName()))
        && filters.matchesActivityStatus(activity.getVerificationStatus().name())
        && filters.matchesVerificationStatus(activity.getVerificationStatus().name())
        && filters.matchesDate(
            activity.getActivityDate(),
            activity.getCreatedAt(),
            activity.getUpdatedAt()
        );
  }

  private List<String> profileCropCandidates(
      CarbonProfileEntity profile,
      List<CarbonFarmPlotEntity> plots,
      List<CarbonActivityRecordEntity> activities
  ) {
    List<String> candidates = new ArrayList<>();
    candidates.add(profile.getCroppingPattern());
    plots.forEach(plot -> {
      candidates.add(plot.getPrimaryCrop());
      candidates.add(plot.getVariety());
    });
    activities.forEach(activity -> candidates.add(activity.getCropName()));
    return candidates;
  }

  private SimpleXlsxWorkbookBuilder.Sheet sheet(String name, List<List<String>> rows) {
    return new SimpleXlsxWorkbookBuilder.Sheet(name, rows, REPORT_HEADER, REPORT_FOOTER);
  }

  private List<List<String>> summaryRows(CarbonReportSummaryResponse summary) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row("Generated at", Instant.now()));
    rows.add(row("Tenant id", summary.tenantId()));
    rows.add(List.of());
    rows.add(row("Metric", "Value"));
    rows.add(row("Farmers enrolled", summary.totalProfiles()));
    rows.add(row("Active farmers", summary.activeProfiles()));
    rows.add(row("Linked farmer logins", summary.linkedFarmerLogins()));
    rows.add(row("Total land holding acres", summary.totalLandHoldingAcres()));
    rows.add(row("Farm plots", summary.totalPlots()));
    rows.add(row("Active plots", summary.activePlots()));
    rows.add(row("Total plot area acres", summary.totalPlotAreaAcres()));
    rows.add(row("Soil profiles", summary.soilProfileCount()));
    rows.add(row("Verified soil profiles", summary.verifiedSoilProfiles()));
    rows.add(row("Pending soil profiles", summary.pendingSoilProfiles()));
    rows.add(row("Average SOC percent", summary.averageSoilOrganicCarbonPercent()));
    rows.add(row("Activity records", summary.activityCount()));
    rows.add(row("Verified activities", summary.verifiedActivities()));
    rows.add(row("Pending activities", summary.pendingActivities()));
    rows.add(row("Evidence count", summary.evidenceCount()));
    rows.add(List.of());
    rows.add(row("Note", "No carbon-credit or sequestration calculation is included."));
    return rows;
  }

  private List<List<String>> profileRows(List<CarbonProfileEntity> profiles) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Carbon ID",
        "Name",
        "Mobile",
        "Username",
        "Village",
        "Taluka",
        "District",
        "State",
        "Participant Type",
        "Total Land Holding Acres",
        "Coordinator",
        "Bank Status",
        "Document Status",
        "Record Status",
        "Created At"
    ));

    profiles.forEach(profile -> rows.add(row(
        profile.getCarbonIdentityId(),
        profile.getDisplayName(),
        profile.getMobileNumber(),
        profile.getUsername(),
        profile.getVillage(),
        profile.getTaluka(),
        profile.getDistrictName(),
        profile.getStateName(),
        profile.getParticipantType(),
        profile.getTotalLandHoldingAcres(),
        profile.getCoordinatorUser() == null ? null : profile.getCoordinatorUser().getDisplayName(),
        profile.getBankStatus(),
        profile.getDocumentStatus(),
        profile.getStatus(),
        profile.getCreatedAt()
    )));

    return rows;
  }

  private List<List<String>> plotRows(List<CarbonFarmPlotEntity> plots) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Carbon ID",
        "Farmer",
        "Farm / Block",
        "Survey Number",
        "Area Acres",
        "Latitude",
        "Longitude",
        "Primary Crop",
        "Variety",
        "Rootstock",
        "Planting Date",
        "Irrigation Source",
        "Tillage Status",
        "Record Status"
    ));

    plots.forEach(plot -> rows.add(row(
        plot.getCarbonProfile().getCarbonIdentityId(),
        plot.getCarbonProfile().getDisplayName(),
        label(plot.getBlockCode(), plot.getFarmName()),
        plot.getSurveyNumber(),
        plot.getAreaAcres(),
        plot.getLatitude(),
        plot.getLongitude(),
        plot.getPrimaryCrop(),
        plot.getVariety(),
        plot.getRootstock(),
        plot.getPlantingDate(),
        plot.getIrrigationSource(),
        plot.getTillageStatus(),
        plot.getStatus()
    )));

    return rows;
  }

  private List<List<String>> soilRows(List<CarbonSoilProfileEntity> soilProfiles) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Carbon ID",
        "Farmer",
        "Farm / Block",
        "Test Date",
        "Lab",
        "SOC %",
        "pH",
        "EC",
        "N kg/ha",
        "P kg/ha",
        "K kg/ha",
        "Texture",
        "Report File",
        "Verification Status",
        "Verified At",
        "Notes"
    ));

    soilProfiles.forEach(soil -> rows.add(row(
        soil.getCarbonProfile().getCarbonIdentityId(),
        soil.getCarbonProfile().getDisplayName(),
        soil.getCarbonFarmPlot() == null ? null : label(
            soil.getCarbonFarmPlot().getBlockCode(),
            soil.getCarbonFarmPlot().getFarmName()
        ),
        soil.getTestDate(),
        soil.getLabName(),
        soil.getSoilOrganicCarbonPercent(),
        soil.getPh(),
        soil.getElectricalConductivity(),
        soil.getNitrogenKgHa(),
        soil.getPhosphorusKgHa(),
        soil.getPotassiumKgHa(),
        soil.getTexture(),
        firstNonBlank(soil.getReportFileName(), soil.getReportUrl(), soil.getReportStorageKey()),
        soil.getVerificationStatus(),
        soil.getVerifiedAt(),
        soil.getVerificationNotes()
    )));

    return rows;
  }

  private List<List<String>> activityRows(List<CarbonActivityRecordEntity> activities) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Carbon ID",
        "Farmer",
        "Farm / Block",
        "Category",
        "Crop",
        "Activity Date",
        "Input Used",
        "Quantity",
        "Evidence Count",
        "Verification Status",
        "Record Status",
        "Remarks"
    ));

    activities.forEach(activity -> rows.add(row(
        activity.getCarbonProfile().getCarbonIdentityId(),
        activity.getCarbonProfile().getDisplayName(),
        activity.getCarbonFarmPlot() == null ? null : label(
            activity.getCarbonFarmPlot().getBlockCode(),
            activity.getCarbonFarmPlot().getFarmName()
        ),
        activity.getCategory().getName(),
        activity.getCropName(),
        activity.getActivityDate(),
        activity.getInputUsed(),
        quantity(activity),
        activity.getEvidenceCount(),
        activity.getVerificationStatus(),
        activity.getStatus(),
        activity.getRemarks()
    )));

    return rows;
  }

  private List<List<String>> villageRows(List<CarbonReportBreakdownResponse> breakdowns) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Village",
        "Farmers",
        "Plots",
        "Area Acres",
        "Soil Profiles",
        "Activities",
        "Pending Verifications"
    ));
    breakdowns.forEach(item -> rows.add(row(
        item.label(),
        item.profileCount(),
        item.plotCount(),
        item.areaAcres(),
        item.soilProfileCount(),
        item.activityCount(),
        item.pendingVerificationCount()
    )));
    return rows;
  }

  private List<List<String>> activityBreakdownRows(
      List<CarbonActivityReportBreakdownResponse> breakdowns
  ) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Category Code",
        "Category",
        "Activities",
        "Verified",
        "Pending",
        "Evidence Count"
    ));
    breakdowns.forEach(item -> rows.add(row(
        item.categoryCode(),
        item.categoryName(),
        item.activityCount(),
        item.verifiedActivities(),
        item.pendingActivities(),
        item.evidenceCount()
    )));
    return rows;
  }

  private String quantity(CarbonActivityRecordEntity activity) {
    if (activity.getQuantityValue() == null) {
      return "";
    }

    return activity.getQuantityUnit() == null || activity.getQuantityUnit().isBlank()
        ? activity.getQuantityValue().toString()
        : activity.getQuantityValue() + " " + activity.getQuantityUnit();
  }

  private String label(String preferred, String fallback) {
    return preferred == null || preferred.isBlank() ? fallback : preferred;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private List<String> row(Object... values) {
    List<String> row = new ArrayList<>();
    for (Object value : values) {
      row.add(value == null ? "" : value.toString());
    }
    return row;
  }
}
