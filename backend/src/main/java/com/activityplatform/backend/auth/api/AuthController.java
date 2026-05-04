package com.activityplatform.backend.auth.api;

import com.activityplatform.backend.auth.service.AuthService;
import com.activityplatform.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success(authService.login(request));
  }

  @PostMapping("/refresh")
  ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.success(authService.refresh(request.refreshToken()));
  }

  @GetMapping("/me")
  ApiResponse<CurrentUserResponse> me(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return ApiResponse.success(CurrentUserResponse.from(jwt));
  }
}

