package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import java.time.Instant;
import java.util.UUID;

public record CropCatalogResponse(
    UUID id,
    UUID tenantId,
    String code,
    String name,
    String category,
    FarmRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CropCatalogResponse from(CropCatalogEntity crop) {
    return new CropCatalogResponse(
        crop.getId(),
        crop.getTenant().getId(),
        crop.getCode(),
        crop.getName(),
        crop.getCategory(),
        crop.getStatus(),
        crop.getCreatedAt(),
        crop.getUpdatedAt()
    );
  }
}
