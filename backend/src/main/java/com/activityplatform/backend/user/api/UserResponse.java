package com.activityplatform.backend.user.api;

import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    UUID tenantId,
    String username,
    String displayName,
    String phone,
    String locationName,
    String siteName,
    String status,
    List<Role> roles,
    Instant createdAt,
    Instant updatedAt
) {
  public static UserResponse from(UserEntity user) {
    return new UserResponse(
        user.getId(),
        user.getTenant().getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getPhone(),
        user.getLocationName(),
        user.getSiteName(),
        user.getStatus(),
        user.getRoles().stream()
            .map(role -> Role.valueOf(role.getCode()))
            .sorted()
            .toList(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );
  }
}
