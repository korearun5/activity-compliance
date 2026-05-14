package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.repository.FarmLandholdingRepository;
import com.activityplatform.backend.fpo.repository.FpoMemberProfileRepository;
import com.activityplatform.backend.fpo.repository.InputDemandEstimateRepository;
import com.activityplatform.backend.fpo.repository.SeasonalCropPlanRepository;
import com.activityplatform.backend.reporting.service.SimpleXlsxWorkbookBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FpoReportWorkbookService {
  private final FarmLandholdingRepository landholdingRepository;
  private final FpoMemberProfileRepository memberRepository;
  private final InputDemandEstimateRepository demandEstimateRepository;
  private final SeasonalCropPlanRepository cropPlanRepository;

  public FpoReportWorkbookService(
      FarmLandholdingRepository landholdingRepository,
      FpoMemberProfileRepository memberRepository,
      InputDemandEstimateRepository demandEstimateRepository,
      SeasonalCropPlanRepository cropPlanRepository
  ) {
    this.landholdingRepository = landholdingRepository;
    this.memberRepository = memberRepository;
    this.demandEstimateRepository = demandEstimateRepository;
    this.cropPlanRepository = cropPlanRepository;
  }

  @Transactional(readOnly = true)
  public byte[] buildWorkbook(UUID tenantId) {
    return buildWorkbook(new FpoReportDataset(
        memberRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        landholdingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        demandEstimateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
    ));
  }

  byte[] buildWorkbook(FpoReportDataset dataset) {
    return new SimpleXlsxWorkbookBuilder().build(List.of(
        new SimpleXlsxWorkbookBuilder.Sheet(
            "Farmer Register",
            farmerRegisterRows(dataset.members(), dataset.landholdings())
        ),
        new SimpleXlsxWorkbookBuilder.Sheet(
            "Crop Plan Summary",
            cropPlanSummaryRows(dataset.cropPlans())
        ),
        new SimpleXlsxWorkbookBuilder.Sheet(
            "Input Demand",
            inputDemandRows(dataset.demandEstimates())
        )
    ));
  }

  private List<List<String>> farmerRegisterRows(
      List<FpoMemberProfileEntity> members,
      List<FarmLandholdingEntity> landholdings
  ) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Name",
        "Mobile",
        "Village",
        "Taluka",
        "District",
        "Survey No",
        "Area (acres)",
        "Category",
        "Status"
    ));

    Map<UUID, List<FarmLandholdingEntity>> landholdingsByMember = landholdings.stream()
        .collect(Collectors.groupingBy(
            landholding -> landholding.getMemberProfile().getId(),
            LinkedHashMap::new,
            Collectors.toList()
        ));

    members.stream()
        .sorted(Comparator.comparing(FpoMemberProfileEntity::getDisplayName))
        .forEach(member -> {
          List<FarmLandholdingEntity> memberLandholdings =
              landholdingsByMember.getOrDefault(member.getId(), List.of());

          if (memberLandholdings.isEmpty()) {
            rows.add(farmerRegisterRow(member, null));
            return;
          }

          memberLandholdings.stream()
              .sorted(Comparator.comparing(FarmLandholdingEntity::getSurveyNumber))
              .forEach(landholding -> rows.add(farmerRegisterRow(member, landholding)));
        });

    return rows;
  }

  private List<String> farmerRegisterRow(
      FpoMemberProfileEntity member,
      FarmLandholdingEntity landholding
  ) {
    return row(
        member.getDisplayName(),
        member.getMobileNumber(),
        member.getVillage(),
        member.getTaluka(),
        member.getDistrictName(),
        landholding == null ? null : landholding.getSurveyNumber(),
        landholding == null ? null : landholding.getTotalAreaAcres(),
        member.getFarmerCategory(),
        member.getStatus()
    );
  }

  private List<List<String>> cropPlanSummaryRows(List<SeasonalCropPlanEntity> plans) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Season",
        "Year",
        "Crop",
        "Village",
        "No. of Farmers",
        "Total Area (acres)",
        "Expected Yield (quintals)"
    ));

    plans.stream()
        .filter(this::isReportableCropPlan)
        .collect(Collectors.groupingBy(
            CropPlanGroup::from,
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> rows.add(cropPlanSummaryRow(entry.getKey(), entry.getValue())));

    return rows;
  }

  private List<String> cropPlanSummaryRow(
      CropPlanGroup group,
      List<SeasonalCropPlanEntity> plans
  ) {
    Set<UUID> farmerIds = plans.stream()
        .map(plan -> plan.getMemberProfile().getId())
        .collect(Collectors.toSet());
    return row(
        group.season(),
        group.year(),
        group.crop(),
        group.village(),
        farmerIds.size(),
        plans.stream()
            .map(SeasonalCropPlanEntity::getPlannedAreaAcres)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        plans.stream()
            .map(SeasonalCropPlanEntity::getExpectedYieldQuintals)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
    );
  }

  private List<List<String>> inputDemandRows(List<InputDemandEstimateEntity> estimates) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Input Type (Seed/Fertilizer)",
        "Crop",
        "Season",
        "Total Area (acres)",
        "Recommended Qty/acre",
        "Total Demand",
        "Buffer 5%",
        "Final Demand",
        "Unit"
    ));

    estimates.stream()
        .collect(Collectors.groupingBy(
            InputDemandGroup::from,
            LinkedHashMap::new,
            Collectors.toList()
        ))
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> rows.add(inputDemandRow(entry.getKey(), entry.getValue())));

    return rows;
  }

  private List<String> inputDemandRow(
      InputDemandGroup group,
      List<InputDemandEstimateEntity> estimates
  ) {
    Map<UUID, SeasonalCropPlanEntity> plansById = estimates.stream()
        .map(InputDemandEstimateEntity::getCropPlan)
        .collect(Collectors.toMap(
            SeasonalCropPlanEntity::getId,
            plan -> plan,
            (left, right) -> left,
            LinkedHashMap::new
        ));

    return row(
        group.inputType(),
        group.crop(),
        group.season(),
        plansById.values().stream()
            .map(SeasonalCropPlanEntity::getPlannedAreaAcres)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        estimates.getFirst().getRecommendedQuantityPerAcre(),
        estimates.stream()
            .map(InputDemandEstimateEntity::getTotalDemandQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        estimates.stream()
            .map(InputDemandEstimateEntity::getBufferQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        estimates.stream()
            .map(InputDemandEstimateEntity::getFinalDemandQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        group.unit()
    );
  }

  private boolean isReportableCropPlan(SeasonalCropPlanEntity plan) {
    return plan.getStatus() == CropPlanStatus.CONFIRMED
        || plan.getStatus() == CropPlanStatus.COMPLETED;
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
      List<SeasonalCropPlanEntity> cropPlans,
      List<InputDemandEstimateEntity> demandEstimates
  ) {
    FpoReportDataset {
      members = members == null ? List.of() : List.copyOf(members);
      landholdings = landholdings == null ? List.of() : List.copyOf(landholdings);
      cropPlans = cropPlans == null ? List.of() : List.copyOf(cropPlans);
      demandEstimates = demandEstimates == null ? List.of() : List.copyOf(demandEstimates);
    }
  }

  private record CropPlanGroup(
      String season,
      String year,
      String crop,
      String village
  ) implements Comparable<CropPlanGroup> {
    static CropPlanGroup from(SeasonalCropPlanEntity plan) {
      return new CropPlanGroup(
          plan.getSeason().getName(),
          plan.getCropYear(),
          plan.getCrop().getName(),
          plan.getMemberProfile().getVillage()
      );
    }

    @Override
    public int compareTo(CropPlanGroup other) {
      return Comparator
          .comparing(CropPlanGroup::year)
          .thenComparing(CropPlanGroup::season)
          .thenComparing(CropPlanGroup::crop)
          .thenComparing(CropPlanGroup::village)
          .compare(this, other);
    }
  }

  private record InputDemandGroup(
      String inputType,
      String crop,
      String season,
      String unit
  ) implements Comparable<InputDemandGroup> {
    static InputDemandGroup from(InputDemandEstimateEntity estimate) {
      return new InputDemandGroup(
          estimate.getInput().getCategory() == null ? "" : estimate.getInput().getCategory(),
          estimate.getCropPlan().getCrop().getName(),
          estimate.getCropPlan().getSeason().getName(),
          estimate.getUnit()
      );
    }

    @Override
    public int compareTo(InputDemandGroup other) {
      return Comparator
          .comparing(InputDemandGroup::crop)
          .thenComparing(InputDemandGroup::season)
          .thenComparing(InputDemandGroup::inputType)
          .thenComparing(InputDemandGroup::unit)
          .compare(this, other);
    }
  }
}
