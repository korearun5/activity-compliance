package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FarmLandholdingResponse(
    UUID id,
    UUID tenantId,
    UUID memberId,
    String memberNumber,
    String surveyNumber,
    BigDecimal totalAreaAcres,
    BigDecimal cultivableAreaAcres,
    String ownershipType,
    String irrigationSource,
    FarmRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static FarmLandholdingResponse from(FarmLandholdingEntity landholding) {
    return new FarmLandholdingResponse(
        landholding.getId(),
        landholding.getTenant().getId(),
        landholding.getMemberProfile().getId(),
        landholding.getMemberProfile().getMemberNumber(),
        landholding.getSurveyNumber(),
        landholding.getTotalAreaAcres(),
        landholding.getCultivableAreaAcres(),
        landholding.getOwnershipType(),
        landholding.getIrrigationSource(),
        landholding.getStatus(),
        landholding.getCreatedAt(),
        landholding.getUpdatedAt()
    );
  }
}
