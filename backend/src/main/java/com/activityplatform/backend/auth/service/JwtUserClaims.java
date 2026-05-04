package com.activityplatform.backend.auth.service;

import java.util.UUID;

public record JwtUserClaims(
    UUID userId,
    UUID tenantId,
    String username
) {
}

