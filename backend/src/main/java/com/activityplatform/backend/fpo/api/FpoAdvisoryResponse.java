package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import com.activityplatform.backend.fpo.domain.AdvisoryTargetType;
import com.activityplatform.backend.fpo.domain.FpoAdvisoryEntity;
import com.activityplatform.backend.notification.domain.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

public record FpoAdvisoryResponse(
    UUID id,
    UUID tenantId,
    UUID cropId,
    String cropName,
    UUID seasonId,
    String seasonName,
    Integer seasonYear,
    AdvisoryTargetType targetType,
    String targetVillage,
    UUID targetMemberId,
    String targetMemberName,
    String title,
    String message,
    NotificationChannel channel,
    AdvisoryStatus status,
    UUID createdByUserId,
    String createdByName,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt
) {
  public static FpoAdvisoryResponse from(FpoAdvisoryEntity advisory) {
    return new FpoAdvisoryResponse(
        advisory.getId(),
        advisory.getTenant().getId(),
        advisory.getCrop() == null ? null : advisory.getCrop().getId(),
        advisory.getCrop() == null ? null : advisory.getCrop().getName(),
        advisory.getSeason() == null ? null : advisory.getSeason().getId(),
        advisory.getSeason() == null ? null : advisory.getSeason().getName(),
        advisory.getSeason() == null ? null : advisory.getSeason().getSeasonYear(),
        advisory.getTargetType(),
        advisory.getTargetVillage(),
        advisory.getTargetMember() == null ? null : advisory.getTargetMember().getId(),
        advisory.getTargetMember() == null ? null : advisory.getTargetMember().getDisplayName(),
        advisory.getTitle(),
        advisory.getMessage(),
        advisory.getChannel(),
        advisory.getStatus(),
        advisory.getCreatedBy() == null ? null : advisory.getCreatedBy().getId(),
        advisory.getCreatedBy() == null ? null : advisory.getCreatedBy().getDisplayName(),
        advisory.getPublishedAt(),
        advisory.getCreatedAt(),
        advisory.getUpdatedAt()
    );
  }
}
