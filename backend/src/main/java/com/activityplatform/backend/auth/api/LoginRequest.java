package com.activityplatform.backend.auth.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    String tenantCode,
    @NotBlank String username,
    @NotBlank String password
) {
}
