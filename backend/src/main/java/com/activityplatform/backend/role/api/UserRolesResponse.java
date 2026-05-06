package com.activityplatform.backend.role.api;

import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserRolesResponse(
    UUID userId,
    UUID tenantId,
    String username,
    List<Role> roles,
    Instant updatedAt
) {
  public static UserRolesResponse from(UserEntity user) {
    return new UserRolesResponse(
        user.getId(),
        user.getTenant().getId(),
        user.getUsername(),
        user.getRoles().stream()
            .map(role -> Role.valueOf(role.getCode()))
            .sorted()
            .toList(),
        user.getUpdatedAt()
    );
  }
}
