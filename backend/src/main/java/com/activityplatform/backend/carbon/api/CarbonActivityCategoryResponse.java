package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonActivityCategoryEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import java.time.Instant;
import java.util.UUID;

public record CarbonActivityCategoryResponse(
    UUID id,
    String code,
    String name,
    String description,
    boolean evidenceRequired,
    int sortOrder,
    CarbonRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CarbonActivityCategoryResponse from(CarbonActivityCategoryEntity category) {
    return new CarbonActivityCategoryResponse(
        category.getId(),
        category.getCode(),
        category.getName(),
        category.getDescription(),
        category.isEvidenceRequired(),
        category.getSortOrder(),
        category.getStatus(),
        category.getCreatedAt(),
        category.getUpdatedAt()
    );
  }
}
