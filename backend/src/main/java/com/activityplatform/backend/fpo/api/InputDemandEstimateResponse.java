package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import com.activityplatform.backend.fpo.domain.InputDemandEstimateStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InputDemandEstimateResponse(
    UUID id,
    UUID tenantId,
    UUID cropPlanId,
    UUID memberId,
    String memberNumber,
    String memberName,
    String memberVillage,
    UUID cropId,
    String cropCode,
    String cropName,
    UUID seasonId,
    String seasonCode,
    String seasonName,
    Integer seasonYear,
    UUID inputId,
    String inputCode,
    String inputName,
    String inputCategory,
    BigDecimal estimatedQuantity,
    BigDecimal recommendedQuantityPerAcre,
    BigDecimal totalDemandQuantity,
    BigDecimal bufferPercent,
    BigDecimal bufferQuantity,
    BigDecimal finalDemandQuantity,
    String unit,
    InputDemandEstimateStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static InputDemandEstimateResponse from(InputDemandEstimateEntity estimate) {
    return new InputDemandEstimateResponse(
        estimate.getId(),
        estimate.getTenant().getId(),
        estimate.getCropPlan().getId(),
        estimate.getCropPlan().getMemberProfile().getId(),
        estimate.getCropPlan().getMemberProfile().getMemberNumber(),
        estimate.getCropPlan().getMemberProfile().getDisplayName(),
        estimate.getCropPlan().getMemberProfile().getVillage(),
        estimate.getCropPlan().getCrop().getId(),
        estimate.getCropPlan().getCrop().getCode(),
        estimate.getCropPlan().getCrop().getName(),
        estimate.getCropPlan().getSeason().getId(),
        estimate.getCropPlan().getSeason().getCode(),
        estimate.getCropPlan().getSeason().getName(),
        estimate.getCropPlan().getSeason().getSeasonYear(),
        estimate.getInput().getId(),
        estimate.getInput().getCode(),
        estimate.getInput().getName(),
        estimate.getInput().getCategory(),
        estimate.getEstimatedQuantity(),
        estimate.getRecommendedQuantityPerAcre(),
        estimate.getTotalDemandQuantity(),
        estimate.getBufferPercent(),
        estimate.getBufferQuantity(),
        estimate.getFinalDemandQuantity(),
        estimate.getUnit(),
        estimate.getStatus(),
        estimate.getCreatedAt(),
        estimate.getUpdatedAt()
    );
  }
}
