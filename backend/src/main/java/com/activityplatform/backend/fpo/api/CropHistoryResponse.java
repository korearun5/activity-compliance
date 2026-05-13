package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmerCropHistoryEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CropHistoryResponse(
    UUID id,
    UUID tenantId,
    UUID memberId,
    String memberNumber,
    String memberName,
    UUID cropId,
    String cropCode,
    String cropName,
    UUID seasonId,
    String seasonCode,
    String seasonName,
    Integer cropYear,
    BigDecimal areaAcres,
    BigDecimal yieldQuantity,
    String yieldUnit,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {
  public static CropHistoryResponse from(FarmerCropHistoryEntity history) {
    CropSeasonEntity season = history.getSeason();
    return new CropHistoryResponse(
        history.getId(),
        history.getTenant().getId(),
        history.getMemberProfile().getId(),
        history.getMemberProfile().getMemberNumber(),
        history.getMemberProfile().getDisplayName(),
        history.getCrop().getId(),
        history.getCrop().getCode(),
        history.getCrop().getName(),
        season == null ? null : season.getId(),
        season == null ? null : season.getCode(),
        season == null ? null : season.getName(),
        history.getCropYear(),
        history.getAreaAcres(),
        history.getYieldQuantity(),
        history.getYieldUnit(),
        history.getNotes(),
        history.getCreatedAt(),
        history.getUpdatedAt()
    );
  }
}
