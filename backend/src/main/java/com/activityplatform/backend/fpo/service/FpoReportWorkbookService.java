package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FpoReportWorkbookService {
  private static final String REPORT_HEADER = "&RCarbon Farming Platform - FPO Digitization";
  private static final String REPORT_FOOTER = "&CConfidential - For internal FPO use";

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
    return buildWorkbook(tenantId, Map.of());
  }

  @Transactional(readOnly = true)
  public byte[] buildWorkbook(UUID tenantId, Map<String, Object> filters) {
    return buildWorkbook(new FpoReportDataset(
        memberRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        landholdingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        cropPlanRepository.findByTenantIdOrderByCreatedAtDesc(tenantId),
        demandEstimateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
    ), FpoReportFilters.from(filters));
  }

  byte[] buildWorkbook(FpoReportDataset dataset) {
    return buildWorkbook(dataset, FpoReportFilters.empty());
  }

  byte[] buildWorkbook(FpoReportDataset dataset, FpoReportFilters filters) {
    return new SimpleXlsxWorkbookBuilder().build(List.of(
        sheet(
            "Farmer Register",
            farmerRegisterRows(dataset, filters)
        ),
        sheet(
            "Crop Plan Summary",
            cropPlanSummaryRows(dataset.cropPlans(), filters)
        ),
        sheet(
            "Input Demand",
            inputDemandRows(dataset.demandEstimates(), filters)
        )
    ));
  }

  private SimpleXlsxWorkbookBuilder.Sheet sheet(String name, List<List<String>> rows) {
    return new SimpleXlsxWorkbookBuilder.Sheet(name, rows, REPORT_HEADER, REPORT_FOOTER);
  }

  private List<List<String>> farmerRegisterRows(FpoReportDataset dataset, FpoReportFilters filters) {
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

    Set<UUID> matchingCropPlanMemberIds = dataset.cropPlans().stream()
        .filter(plan -> matchesCropPlanDimensions(plan, filters))
        .map(plan -> plan.getMemberProfile().getId())
        .collect(Collectors.toSet());

    Map<UUID, List<FarmLandholdingEntity>> landholdingsByMember = dataset.landholdings().stream()
        .collect(Collectors.groupingBy(
            landholding -> landholding.getMemberProfile().getId(),
            LinkedHashMap::new,
            Collectors.toList()
        ));

    dataset.members().stream()
        .filter(member -> matchesMember(member, filters))
        .filter(member -> !filters.hasCropOrSeason()
            || matchingCropPlanMemberIds.contains(member.getId()))
        .filter(member -> dateMatches(member.getCreatedAt(), filters))
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

  private List<List<String>> cropPlanSummaryRows(
      List<SeasonalCropPlanEntity> plans,
      FpoReportFilters filters
  ) {
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
        .filter(plan -> matchesCropPlanDimensions(plan, filters))
        .filter(plan -> dateMatchesAny(filters, plan.getCreatedAt(), plan.getUpdatedAt()))
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

  private List<List<String>> inputDemandRows(
      List<InputDemandEstimateEntity> estimates,
      FpoReportFilters filters
  ) {
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
        .filter(estimate -> matchesCropPlanDimensions(estimate.getCropPlan(), filters))
        .filter(estimate -> dateMatchesConfirmedAt(estimate.getCropPlan(), filters))
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

  private boolean matchesCropPlanDimensions(
      SeasonalCropPlanEntity plan,
      FpoReportFilters filters
  ) {
    return matchesMember(plan.getMemberProfile(), filters)
        && matchesAny(filters.crop(), List.of(
            plan.getCrop().getId().toString(),
            plan.getCrop().getCode(),
            plan.getCrop().getName()
        ))
        && matchesAny(filters.season(), List.of(
            plan.getSeason().getId().toString(),
            plan.getSeason().getCode(),
            plan.getSeason().getName(),
            plan.getSeason().getName() + " " + plan.getSeason().getSeasonYear(),
            plan.getSeason().getName() + " " + plan.getCropYear(),
            plan.getCropYear()
        ));
  }

  private boolean matchesMember(FpoMemberProfileEntity member, FpoReportFilters filters) {
    return matchesAny(filters.village(), List.of(member.getVillage()))
        && matchesAny(filters.coordinator(), coordinatorLabels(member));
  }

  private List<String> coordinatorLabels(FpoMemberProfileEntity member) {
    if (member.getCoordinatorUser() == null) {
      return List.of();
    }
    return List.of(
        member.getCoordinatorUser().getId().toString(),
        member.getCoordinatorUser().getUsername(),
        member.getCoordinatorUser().getDisplayName()
    );
  }

  private boolean matchesAny(String filter, List<String> candidates) {
    if (filter == null || filter.isBlank()) {
      return true;
    }
    String normalizedFilter = normalize(filter);
    return candidates.stream()
        .filter(Objects::nonNull)
        .map(this::normalize)
        .anyMatch(candidate -> candidate.equals(normalizedFilter)
            || candidate.contains(normalizedFilter)
            || normalizedFilter.contains(candidate));
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  private boolean dateMatchesAny(FpoReportFilters filters, Instant... instants) {
    if (!filters.hasDateRange()) {
      return true;
    }
    for (Instant instant : instants) {
      if (dateMatches(instant, filters)) {
        return true;
      }
    }
    return false;
  }

  private boolean dateMatchesConfirmedAt(
      SeasonalCropPlanEntity plan,
      FpoReportFilters filters
  ) {
    return !filters.hasDateRange() || dateMatches(plan.getConfirmedAt(), filters);
  }

  private boolean dateMatches(Instant instant, FpoReportFilters filters) {
    if (!filters.hasDateRange()) {
      return true;
    }
    if (instant == null) {
      return false;
    }
    LocalDate date = LocalDate.ofInstant(instant, ZoneOffset.UTC);
    return (filters.dateFrom() == null || !date.isBefore(filters.dateFrom()))
        && (filters.dateTo() == null || !date.isAfter(filters.dateTo()));
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

  record FpoReportFilters(
      String village,
      String crop,
      String season,
      String coordinator,
      LocalDate dateFrom,
      LocalDate dateTo
  ) {
    static FpoReportFilters empty() {
      return new FpoReportFilters(null, null, null, null, null, null);
    }

    static FpoReportFilters from(Map<String, Object> values) {
      if (values == null || values.isEmpty()) {
        return empty();
      }

      return new FpoReportFilters(
          text(values, "village", "memberVillage"),
          text(values, "crop", "cropId", "cropName"),
          text(values, "season", "seasonId", "seasonName"),
          text(values, "coordinator", "coordinatorId", "coordinatorName"),
          date(values, "dateFrom", "from", "startDate"),
          date(values, "dateTo", "to", "endDate")
      );
    }

    boolean hasCropOrSeason() {
      return hasText(crop) || hasText(season);
    }

    boolean hasDateRange() {
      return dateFrom != null || dateTo != null;
    }

    private static String text(Map<String, Object> values, String... keys) {
      for (String key : keys) {
        Object value = values.get(key);
        if (value != null && !value.toString().isBlank()) {
          return value.toString().trim();
        }
      }
      return null;
    }

    private static LocalDate date(Map<String, Object> values, String... keys) {
      String value = text(values, keys);
      if (value == null) {
        return null;
      }
      try {
        return LocalDate.parse(value);
      } catch (RuntimeException exception) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_FAILED,
            "Report date filters must use YYYY-MM-DD format.",
            HttpStatus.BAD_REQUEST
        );
      }
    }

    private static boolean hasText(String value) {
      return value != null && !value.isBlank();
    }
  }
}
