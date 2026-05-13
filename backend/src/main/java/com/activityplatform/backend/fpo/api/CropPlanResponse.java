package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CropPlanResponse(
    UUID id,
    UUID tenantId,
    UUID memberId,
    String memberNumber,
    String memberName,
    String memberVillage,
    UUID plotId,
    String plotName,
    UUID cropId,
    String cropCode,
    String cropName,
    UUID seasonId,
    String seasonCode,
    String seasonName,
    Integer seasonYear,
    BigDecimal plannedAreaAcres,
    LocalDate plannedSowingDate,
    LocalDate expectedHarvestDate,
    CropPlanStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CropPlanResponse from(SeasonalCropPlanEntity plan) {
    FarmPlotEntity plot = plan.getPlot();
    return new CropPlanResponse(
        plan.getId(),
        plan.getTenant().getId(),
        plan.getMemberProfile().getId(),
        plan.getMemberProfile().getMemberNumber(),
        plan.getMemberProfile().getDisplayName(),
        plan.getMemberProfile().getVillage(),
        plot == null ? null : plot.getId(),
        plot == null ? null : plot.getPlotName(),
        plan.getCrop().getId(),
        plan.getCrop().getCode(),
        plan.getCrop().getName(),
        plan.getSeason().getId(),
        plan.getSeason().getCode(),
        plan.getSeason().getName(),
        plan.getSeason().getSeasonYear(),
        plan.getPlannedAreaAcres(),
        plan.getPlannedSowingDate(),
        plan.getExpectedHarvestDate(),
        plan.getStatus(),
        plan.getCreatedAt(),
        plan.getUpdatedAt()
    );
  }
}
