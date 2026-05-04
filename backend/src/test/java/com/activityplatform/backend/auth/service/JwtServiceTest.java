package com.activityplatform.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.activityplatform.backend.auth.api.LoginResponse;
import com.activityplatform.backend.auth.config.AuthProperties;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.security.JwtConfig;
import com.activityplatform.backend.security.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class JwtServiceTest {
  private final AuthProperties properties = authProperties();
  private final JwtConfig jwtConfig = new JwtConfig();
  private final JwtDecoder jwtDecoder = jwtConfig.jwtDecoder(jwtConfig.jwtSecretKey(properties));
  private final JwtService jwtService = new JwtService(
      properties,
      jwtDecoder,
      jwtConfig.jwtEncoder(jwtConfig.jwtSecretKey(properties))
  );

  @Test
  void issuesAccessAndRefreshTokensWithReusableClaims() {
    UserEntity user = testUser();

    LoginResponse response = jwtService.issueTokens(user);

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.roles()).containsExactly(Role.ADMIN);

    var jwt = jwtDecoder.decode(response.accessToken());
    assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
    assertThat(jwt.getClaimAsString("tenantId")).isEqualTo(user.getTenant().getId().toString());
    assertThat(jwt.getClaimAsString("username")).isEqualTo("admin");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
    assertThat(jwt.getClaimAsString("tokenType")).isEqualTo("access");
  }

  @Test
  void acceptsRefreshTokensForNewTokenIssueFlow() {
    UserEntity user = testUser();
    LoginResponse response = jwtService.issueTokens(user);

    JwtUserClaims claims = jwtService.requireRefreshToken(response.refreshToken());

    assertThat(claims.userId()).isEqualTo(user.getId());
    assertThat(claims.tenantId()).isEqualTo(user.getTenant().getId());
    assertThat(claims.username()).isEqualTo("admin");
  }

  private AuthProperties authProperties() {
    AuthProperties props = new AuthProperties();
    props.setIssuer("activity-compliance-test");
    props.setAccessTokenTtl(Duration.ofMinutes(30));
    props.setRefreshTokenTtl(Duration.ofDays(7));
    props.setSecret("test-secret-key-with-enough-length-for-hmac");
    return props;
  }

  private UserEntity testUser() {
    Instant now = Instant.now();
    TenantEntity tenant = new TenantEntity(
        UUID.randomUUID(),
        "default",
        "Default Client",
        "ACTIVE",
        now
    );
    RoleEntity admin = new RoleEntity(
        UUID.randomUUID(),
        tenant,
        Role.ADMIN.name(),
        "Admin",
        now
    );
    UserEntity user = new UserEntity(
        UUID.randomUUID(),
        tenant,
        "admin",
        "hash",
        "Platform Admin",
        "+91 00000 00000",
        "Head Office",
        "Admin",
        "ACTIVE",
        now
    );
    user.addRole(admin);
    return user;
  }
}

