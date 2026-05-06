package com.activityplatform.backend.role.api;

import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
    UUID id,
    UUID tenantId,
    Role code,
    String name,
    Instant createdAt
) {
  public static RoleResponse from(RoleEntity role) {
    return new RoleResponse(
        role.getId(),
        role.getTenant().getId(),
        Role.valueOf(role.getCode()),
        role.getName(),
        role.getCreatedAt()
    );
  }
}
