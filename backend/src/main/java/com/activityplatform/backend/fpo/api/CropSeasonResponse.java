package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import java.time.Instant;
import java.util.UUID;

public record CropSeasonResponse(
    UUID id,
    UUID tenantId,
    String code,
    String name,
    Integer startMonth,
    Integer endMonth,
    Integer seasonYear,
    FarmRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CropSeasonResponse from(CropSeasonEntity season) {
    return new CropSeasonResponse(
        season.getId(),
        season.getTenant().getId(),
        season.getCode(),
        season.getName(),
        season.getStartMonth(),
        season.getEndMonth(),
        season.getSeasonYear(),
        season.getStatus(),
        season.getCreatedAt(),
        season.getUpdatedAt()
    );
  }
}
