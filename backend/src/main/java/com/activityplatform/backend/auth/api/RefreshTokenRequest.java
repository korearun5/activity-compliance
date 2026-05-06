package com.activityplatform.backend.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
    @NotBlank
    @Size(max = 4096)
    String refreshToken
) {
}
