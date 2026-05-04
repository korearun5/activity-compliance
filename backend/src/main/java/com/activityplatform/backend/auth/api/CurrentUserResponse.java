package com.activityplatform.backend.auth.api;

import com.activityplatform.backend.security.Role;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUserResponse(
    UUID userId,
    UUID tenantId,
    String username,
    String displayName,
    Set<Role> roles
) {
  public static CurrentUserResponse from(Jwt jwt) {
    return new CurrentUserResponse(
        UUID.fromString(jwt.getSubject()),
        UUID.fromString(jwt.getClaimAsString("tenantId")),
        jwt.getClaimAsString("username"),
        jwt.getClaimAsString("displayName"),
        jwt.getClaimAsStringList("roles").stream()
            .map(Role::valueOf)
            .collect(Collectors.toUnmodifiableSet())
    );
  }
}

