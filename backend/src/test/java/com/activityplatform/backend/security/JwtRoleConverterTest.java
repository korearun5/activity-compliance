package com.activityplatform.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRoleConverterTest {
  private final JwtRoleConverter converter = new JwtRoleConverter();

  @Test
  void mapsRoleClaimsToSpringAuthorities() {
    Jwt jwt = new Jwt(
        "token",
        Instant.now(),
        Instant.now().plusSeconds(60),
        Map.of("alg", "HS256"),
        Map.of("username", "admin", "roles", List.of("ADMIN", "SUPERVISOR"))
    );

    var authentication = converter.convert(jwt);

    assertThat(authentication.getName()).isEqualTo("admin");
    assertThat(authentication.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_ADMIN", "ROLE_SUPERVISOR");
  }
}

