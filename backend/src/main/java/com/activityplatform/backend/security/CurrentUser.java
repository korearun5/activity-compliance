package com.activityplatform.backend.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(
    UUID userId,
    UUID tenantId,
    String username,
    Set<Role> roles
) {
  public static CurrentUser from(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    List<String> roleNames = jwt.getClaimAsStringList("roles");
    Set<Role> roles = roleNames == null
        ? Set.of()
        : roleNames.stream().map(Role::valueOf).collect(Collectors.toUnmodifiableSet());

    return new CurrentUser(
        UUID.fromString(jwt.getSubject()),
        UUID.fromString(jwt.getClaimAsString("tenantId")),
        jwt.getClaimAsString("username"),
        roles
    );
  }

  public boolean hasAnyRole(Role... expectedRoles) {
    for (Role role : expectedRoles) {
      if (roles.contains(role)) {
        return true;
      }
    }
    return false;
  }
}
