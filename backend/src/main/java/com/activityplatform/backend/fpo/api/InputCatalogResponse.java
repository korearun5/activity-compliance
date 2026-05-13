package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import java.time.Instant;
import java.util.UUID;

public record InputCatalogResponse(
    UUID id,
    UUID tenantId,
    String code,
    String name,
    String category,
    String unit,
    FarmRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static InputCatalogResponse from(InputCatalogEntity input) {
    return new InputCatalogResponse(
        input.getId(),
        input.getTenant().getId(),
        input.getCode(),
        input.getName(),
        input.getCategory(),
        input.getUnit(),
        input.getStatus(),
        input.getCreatedAt(),
        input.getUpdatedAt()
    );
  }
}
