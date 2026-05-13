package com.activityplatform.backend.platform.api;

import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record TenantModuleSubscriptionRequest(
    @NotNull
    TenantModuleSubscriptionStatus status,
    Instant expiresAt
) {
}
