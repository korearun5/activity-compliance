package com.activityplatform.backend.auth.api;

import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Instant expiresAt,
    UUID userId,
    UUID tenantId,
    Set<Role> roles
) {
}

