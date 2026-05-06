package com.activityplatform.backend.auth.service;

import com.activityplatform.backend.auth.api.LoginRequest;
import com.activityplatform.backend.auth.api.LoginResponse;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.security.CurrentUser;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final String DEFAULT_TENANT_CODE = "default";

  private final AuditEventService auditEventService;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  public AuthService(
      AuditEventService auditEventService,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      UserRepository userRepository) {
    this.auditEventService = auditEventService;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    String tenantCode = normalizeTenantCode(request.tenantCode());
    UserEntity user = userRepository
        .findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(tenantCode, request.username().trim())
        .orElseThrow(this::invalidCredentials);

    if (!"ACTIVE".equals(user.getStatus())
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw invalidCredentials();
    }

    auditEventService.record(
        user.getTenant(),
        user,
        "USER",
        user.getId(),
        AuditAction.AUTH_LOGIN,
        Map.of("username", user.getUsername()));

    return jwtService.issueTokens(user);
  }

  @Transactional(readOnly = true)
  public LoginResponse refresh(String refreshToken) {
    JwtUserClaims claims = jwtService.requireRefreshToken(refreshToken);
    UserEntity user = userRepository.findById(claims.userId())
        .orElseThrow(this::invalidCredentials);

    if (!"ACTIVE".equals(user.getStatus())) {
      throw invalidCredentials();
    }

    return jwtService.issueTokens(user);
  }

  @Transactional
  public void logout(CurrentUser currentUser) {
    UserEntity user = userRepository.findById(currentUser.userId())
        .orElseThrow(this::invalidCredentials);
    auditEventService.record(
        user.getTenant(),
        user,
        "USER",
        user.getId(),
        AuditAction.AUTH_LOGOUT,
        Map.of("username", user.getUsername()));
  }

  private String normalizeTenantCode(String tenantCode) {
    if (tenantCode == null || tenantCode.isBlank()) {
      return DEFAULT_TENANT_CODE;
    }

    return tenantCode.trim();
  }

  private ApplicationException invalidCredentials() {
    return new ApplicationException(
        ErrorCode.AUTH_INVALID_CREDENTIALS,
        "Invalid username or password.",
        HttpStatus.UNAUTHORIZED);
  }
}
