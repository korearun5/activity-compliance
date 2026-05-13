package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.FarmerCropHistoryEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
import com.activityplatform.backend.fpo.repository.FarmPlotRepository;
import com.activityplatform.backend.fpo.repository.FarmerCropHistoryRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.InputDemandEstimateRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.reporting.service.SimpleXlsxWorkbookBuilder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FpoReportWorkbookService {
  private final FarmLandholdingRepository landholdingRepository;
  private final FarmPlotRepository plotRepository;
  private final FarmerCropHistoryRepository cropHistoryRepository;
  private final FpoMemberProfileRepository memberRepository;
  private final InputDemandEstimateRepository demandEstimateRepository;
  private final SeasonalCropPlanRepository cropPlanRepository;

  public FpoReportWorkbookService(
      FarmLandholdingRepository landholdingRepository,
      FarmPlotRepository plotRepository,
      FarmerCropHistoryRepository cropHistoryRepository,
      FpoMemberProfileRepository memberRepository,
      InputDemandEstimateRepository demandEstimateRepository,
      SeasonalCropPlanRepository cropPlanRepository
  ) {
    this.landholdingRepository = landholdingRepository;
    this.plotRepository = plotRepository;
    this.cropHistoryRepository = cropHistoryRepository;
    this.memberRepository = memberRepository;
    this.demandEstimateRepository = demandEstimateRepository;
    this.cropPlanRepository = cropPlanRepository;
  }

  @Transactional(readOnly = true)
  public byte[] buildWorkbook(UUID tenantId) {
    return buildWorkbook(new FpoReportDataset(
        memberRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        landholdingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        plotRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        cropHistoryRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        demandEstimateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
    ));
  }

  byte[] buildWorkbook(FpoReportDataset dataset) {
    return new SimpleXlsxWorkbookBuilder().build(List.of(
        new SimpleXlsxWorkbookBuilder.Sheet("Farmer Master", farmerMasterRows(dataset.members())),
        new SimpleXlsxWorkbookBuilder.Sheet("Landholdings", landholdingRows(dataset.landholdings())),
        new SimpleXlsxWorkbookBuilder.Sheet("Farm Plots", plotRows(dataset.plots())),
        new SimpleXlsxWorkbookBuilder.Sheet(
            "Crop History",
            cropHistoryRows(dataset.cropHistory())
        ),
        new SimpleXlsxWorkbookBuilder.Sheet(
            "Seasonal Crop Plans",
            cropPlanRows(dataset.cropPlans())
        ),
        new SimpleXlsxWorkbookBuilder.Sheet(
            "Input Demand Summary",
            inputDemandSummaryRows(dataset.demandEstimates())
        ),
        new SimpleXlsxWorkbookBuilder.Sheet(
            "Farmer-wise Input Demand",
            farmerWiseDemandRows(dataset.demandEstimates())
        )
    ));
  }

  private List<List<String>> farmerMasterRows(List<FpoMemberProfileEntity> members) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Member number",
        "Farmer name",
        "Mobile number",
        "Alternate mobile",
        "Aadhaar number",
        "Village",
        "Taluka",
        "District",
        "State",
        "Gender",
        "Age",
        "Farmer category",
        "Coordinator",
        "Status",
        "Login username",
        "Created at",
        "Updated at"
    ));

    members.stream()
        .sorted(Comparator.comparing(FpoMemberProfileEntity::getMemberNumber))
        .forEach(member -> rows.add(row(
            member.getMemberNumber(),
            member.getDisplayName(),
            member.getMobileNumber(),
            member.getAlternateMobileNumber(),
            member.getAadhaarNumber(),
            member.getVillage(),
            member.getTaluka(),
            member.getDistrictName(),
            member.getStateName(),
            member.getGender(),
            member.getAge(),
            member.getFarmerCategory(),
            member.getCoordinatorUser() == null
                ? null
                : member.getCoordinatorUser().getDisplayName(),
            member.getStatus(),
            member.getUser().getUsername(),
            member.getCreatedAt(),
            member.getUpdatedAt()
        )));

    return rows;
  }

  private List<List<String>> landholdingRows(List<FarmLandholdingEntity> landholdings) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Member number",
        "Farmer name",
        "Village",
        "Survey number",
        "Total area acres",
        "Cultivable area acres",
        "Ownership type",
        "Irrigation source",
        "Status",
        "Created at",
        "Updated at"
    ));

    landholdings.stream()
        .sorted(Comparator
            .comparing((FarmLandholdingEntity item) -> item.getMemberProfile().getMemberNumber())
            .thenComparing(FarmLandholdingEntity::getCreatedAt))
        .forEach(landholding -> rows.add(row(
            landholding.getMemberProfile().getMemberNumber(),
            landholding.getMemberProfile().getDisplayName(),
            landholding.getMemberProfile().getVillage(),
            landholding.getSurveyNumber(),
            landholding.getTotalAreaAcres(),
            landholding.getCultivableAreaAcres(),
            landholding.getOwnershipType(),
            landholding.getIrrigationSource(),
            landholding.getStatus(),
            landholding.getCreatedAt(),
            landholding.getUpdatedAt()
        )));

    return rows;
  }

  private List<List<String>> plotRows(List<FarmPlotEntity> plots) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Member number",
        "Farmer name",
        "Village",
        "Plot name",
        "Linked survey number",
        "Area acres",
        "Latitude",
        "Longitude",
        "Soil type",
        "Status",
        "Created at",
        "Updated at"
    ));

    plots.stream()
        .sorted(Comparator
            .comparing((FarmPlotEntity item) -> item.getMemberProfile().getMemberNumber())
            .thenComparing(FarmPlotEntity::getPlotName))
        .forEach(plot -> rows.add(row(
            plot.getMemberProfile().getMemberNumber(),
            plot.getMemberProfile().getDisplayName(),
            plot.getMemberProfile().getVillage(),
            plot.getPlotName(),
            plot.getLandholding() == null ? null : plot.getLandholding().getSurveyNumber(),
            plot.getAreaAcres(),
            plot.getLatitude(),
            plot.getLongitude(),
            plot.getSoilType(),
            plot.getStatus(),
            plot.getCreatedAt(),
            plot.getUpdatedAt()
        )));

    return rows;
  }

  private List<List<String>> cropHistoryRows(List<FarmerCropHistoryEntity> historyItems) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Member number",
        "Farmer name",
        "Village",
        "Crop",
        "Crop code",
        "Season",
        "Season year",
        "Crop year",
        "Area acres",
        "Yield quantity",
        "Yield unit",
        "Notes",
        "Created at",
        "Updated at"
    ));

    historyItems.stream()
        .sorted(Comparator
            .comparing((FarmerCropHistoryEntity item) ->
                item.getMemberProfile().getMemberNumber())
            .thenComparing(item -> item.getCrop().getName())
            .thenComparing(item -> item.getCropYear() == null ? 0 : item.getCropYear()))
        .forEach(history -> rows.add(row(
            history.getMemberProfile().getMemberNumber(),
            history.getMemberProfile().getDisplayName(),
            history.getMemberProfile().getVillage(),
            history.getCrop().getName(),
            history.getCrop().getCode(),
            history.getSeason() == null ? null : history.getSeason().getName(),
            history.getSeason() == null ? null : history.getSeason().getSeasonYear(),
            history.getCropYear(),
            history.getAreaAcres(),
            history.getYieldQuantity(),
            history.getYieldUnit(),
            history.getNotes(),
            history.getCreatedAt(),
            history.getUpdatedAt()
        )));

    return rows;
  }

  private List<List<String>> cropPlanRows(List<SeasonalCropPlanEntity> plans) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Member number",
        "Farmer name",
        "Village",
        "Crop",
        "Crop code",
        "Season",
        "Season year",
        "Plot",
        "Planned area acres",
        "Planned sowing date",
        "Expected harvest date",
        "Status",
        "Created at",
        "Updated at"
    ));

    plans.stream()
        .sorted(Comparator
            .comparing((SeasonalCropPlanEntity item) -> item.getSeason().getSeasonYear())
            .thenComparing(item -> item.getCrop().getName())
            .thenComparing(item -> item.getMemberProfile().getMemberNumber()))
        .forEach(plan -> rows.add(row(
            plan.getMemberProfile().getMemberNumber(),
            plan.getMemberProfile().getDisplayName(),
            plan.getMemberProfile().getVillage(),
            plan.getCrop().getName(),
            plan.getCrop().getCode(),
            plan.getSeason().getName(),
            plan.getSeason().getSeasonYear(),
            plan.getPlot() == null ? null : plan.getPlot().getPlotName(),
            plan.getPlannedAreaAcres(),
            plan.getPlannedSowingDate(),
            plan.getExpectedHarvestDate(),
            plan.getStatus(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        )));

    return rows;
  }

  private List<List<String>> inputDemandSummaryRows(
      List<InputDemandEstimateEntity> estimates
  ) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Input code",
        "Input name",
        "Unit",
        "Estimated quantity",
        "Plan count",
        "Farmer count"
    ));

    estimates.stream()
        .collect(Collectors.groupingBy(
            estimate -> estimate.getInput().getId(),
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .values()
        .stream()
        .map(this::inputDemandSummaryRow)
        .sorted(Comparator.comparing(row -> row.get(1)))
        .forEach(rows::add);

    return rows;
  }

  private List<String> inputDemandSummaryRow(List<InputDemandEstimateEntity> estimates) {
    InputDemandEstimateEntity first = estimates.getFirst();
    Set<UUID> planIds = estimates.stream()
        .map(estimate -> estimate.getCropPlan().getId())
        .collect(Collectors.toSet());
    Set<UUID> memberIds = estimates.stream()
        .map(estimate -> estimate.getCropPlan().getMemberProfile().getId())
        .collect(Collectors.toSet());

    return row(
        first.getInput().getCode(),
        first.getInput().getName(),
        first.getUnit(),
        estimates.stream()
            .map(InputDemandEstimateEntity::getEstimatedQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        planIds.size(),
        memberIds.size()
    );
  }

  private List<List<String>> farmerWiseDemandRows(
      List<InputDemandEstimateEntity> estimates
  ) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Member number",
        "Farmer name",
        "Village",
        "Crop",
        "Season",
        "Season year",
        "Input code",
        "Input name",
        "Quantity",
        "Unit",
        "Estimate status",
        "Created at",
        "Updated at"
    ));

    estimates.stream()
        .sorted(Comparator
            .comparing((InputDemandEstimateEntity item) ->
                item.getCropPlan().getMemberProfile().getMemberNumber())
            .thenComparing(item -> item.getInput().getName()))
        .forEach(estimate -> rows.add(row(
            estimate.getCropPlan().getMemberProfile().getMemberNumber(),
            estimate.getCropPlan().getMemberProfile().getDisplayName(),
            estimate.getCropPlan().getMemberProfile().getVillage(),
            estimate.getCropPlan().getCrop().getName(),
            estimate.getCropPlan().getSeason().getName(),
            estimate.getCropPlan().getSeason().getSeasonYear(),
            estimate.getInput().getCode(),
            estimate.getInput().getName(),
            estimate.getEstimatedQuantity(),
            estimate.getUnit(),
            estimate.getStatus(),
            estimate.getCreatedAt(),
            estimate.getUpdatedAt()
        )));

    return rows;
  }

  private List<String> row(Object... values) {
    List<String> row = new ArrayList<>();
    for (Object value : values) {
      row.add(value == null ? "" : value.toString());
    }

    return row;
  }

  record FpoReportDataset(
      List<FpoMemberProfileEntity> members,
      List<FarmLandholdingEntity> landholdings,
      List<FarmPlotEntity> plots,
      List<FarmerCropHistoryEntity> cropHistory,
      List<SeasonalCropPlanEntity> cropPlans,
      List<InputDemandEstimateEntity> demandEstimates
  ) {
    FpoReportDataset {
      members = members == null ? List.of() : List.copyOf(members);
      landholdings = landholdings == null ? List.of() : List.copyOf(landholdings);
      plots = plots == null ? List.of() : List.copyOf(plots);
      cropHistory = cropHistory == null ? List.of() : List.copyOf(cropHistory);
      cropPlans = cropPlans == null ? List.of() : List.copyOf(cropPlans);
      demandEstimates = demandEstimates == null ? List.of() : List.copyOf(demandEstimates);
    }
  }
}
