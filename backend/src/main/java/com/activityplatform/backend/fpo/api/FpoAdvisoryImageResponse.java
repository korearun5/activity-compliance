package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FpoAdvisoryImageEntity;
import java.time.Instant;
import java.util.UUID;

public record FpoAdvisoryImageResponse(
    UUID id,
    String imageUrl,
    String storageKey,
    String originalFilename,
    String contentType,
    Integer sortOrder,
    Instant createdAt
) {
  static FpoAdvisoryImageResponse from(FpoAdvisoryImageEntity image) {
    return new FpoAdvisoryImageResponse(
        image.getId(),
        image.getImageUrl(),
        image.getStorageKey(),
        image.getOriginalFilename(),
        image.getContentType(),
        image.getSortOrder(),
        image.getCreatedAt()
    );
  }
}
