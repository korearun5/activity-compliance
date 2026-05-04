package com.activityplatform.backend.auth.service;

import com.activityplatform.backend.auth.api.LoginResponse;
import com.activityplatform.backend.auth.config.AuthProperties;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final String TOKEN_TYPE_ACCESS = "access";
  private static final String TOKEN_TYPE_REFRESH = "refresh";

  private final AuthProperties properties;
  private final JwtDecoder jwtDecoder;
  private final JwtEncoder jwtEncoder;

  public JwtService(AuthProperties properties, JwtDecoder jwtDecoder, JwtEncoder jwtEncoder) {
    this.properties = properties;
    this.jwtDecoder = jwtDecoder;
    this.jwtEncoder = jwtEncoder;
  }

  public LoginResponse issueTokens(UserEntity user) {
    Instant issuedAt = Instant.now();
    Instant accessExpiresAt = issuedAt.plus(properties.getAccessTokenTtl());
    Set<Role> roles = rolesOf(user);

    String accessToken = encode(user, roles, TOKEN_TYPE_ACCESS, issuedAt, accessExpiresAt);
    String refreshToken = encode(
        user,
        roles,
        TOKEN_TYPE_REFRESH,
        issuedAt,
        issuedAt.plus(properties.getRefreshTokenTtl())
    );

    return new LoginResponse(
        accessToken,
        refreshToken,
        accessExpiresAt,
        user.getId(),
        user.getTenant().getId(),
        roles
    );
  }

  public JwtUserClaims requireRefreshToken(String token) {
    try {
      Jwt jwt = jwtDecoder.decode(token);
      if (!TOKEN_TYPE_REFRESH.equals(jwt.getClaimAsString("tokenType"))) {
        throw invalidToken();
      }

      return new JwtUserClaims(
          UUID.fromString(jwt.getSubject()),
          UUID.fromString(jwt.getClaimAsString("tenantId")),
          jwt.getClaimAsString("username")
      );
    } catch (RuntimeException exception) {
      throw invalidToken();
    }
  }

  private String encode(
      UserEntity user,
      Set<Role> roles,
      String tokenType,
      Instant issuedAt,
      Instant expiresAt
  ) {
    List<String> roleNames = roles.stream().map(Role::name).sorted().toList();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.getIssuer())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .subject(user.getId().toString())
        .claim("tenantId", user.getTenant().getId().toString())
        .claim("username", user.getUsername())
        .claim("displayName", user.getDisplayName())
        .claim("roles", roleNames)
        .claim("tokenType", tokenType)
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private Set<Role> rolesOf(UserEntity user) {
    return user.getRoles().stream()
        .map(role -> Role.valueOf(role.getCode()))
        .collect(Collectors.toUnmodifiableSet());
  }

  private ApplicationException invalidToken() {
    return new ApplicationException(
        ErrorCode.AUTH_INVALID_CREDENTIALS,
        "Invalid refresh token.",
        HttpStatus.UNAUTHORIZED
    );
  }
}
