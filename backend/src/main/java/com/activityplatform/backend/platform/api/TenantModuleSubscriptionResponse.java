package com.activityplatform.backend.platform.api;

import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionEntity;
import java.time.Instant;
import java.util.UUID;

public record TenantModuleSubscriptionResponse(
    UUID id,
    UUID tenantId,
    String code,
    String name,
    String description,
    String status,
    boolean enabled,
    Instant enabledAt,
    Instant disabledAt,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {
  public static TenantModuleSubscriptionResponse from(
      TenantModuleSubscriptionEntity subscription,
      Instant now
  ) {
    return new TenantModuleSubscriptionResponse(
        subscription.getId(),
        subscription.getTenant().getId(),
        subscription.getModule().getCode().name(),
        subscription.getModule().getName(),
        subscription.getModule().getDescription(),
        subscription.getStatus().name(),
        subscription.isEnabledAt(now),
        subscription.getEnabledAt(),
        subscription.getDisabledAt(),
        subscription.getExpiresAt(),
        subscription.getCreatedAt(),
        subscription.getUpdatedAt()
    );
  }
}
