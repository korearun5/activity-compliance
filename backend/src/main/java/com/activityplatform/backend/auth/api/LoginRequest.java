package com.activityplatform.backend.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Tenant code contains invalid characters.")
    String tenantCode,
    @NotBlank
    @Size(min = 3, max = 120)
    @Pattern(regexp = "^[A-Za-z0-9._@-]+$", message = "Username contains invalid characters.")
    String username,
    @NotBlank
    @Size(max = 128)
    String password
) {
}
