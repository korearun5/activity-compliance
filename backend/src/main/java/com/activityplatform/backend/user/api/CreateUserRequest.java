package com.activityplatform.backend.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank
    @Size(max = 120)
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
        message = "Username must use letters, numbers, dots, underscores, and hyphens."
    )
    String username,
    @NotBlank
    @Size(min = 8, max = 128)
    String password,
    @NotBlank
    @Size(max = 180)
    String displayName,
    @Size(max = 40)
    String phone,
    @Size(max = 160)
    String locationName,
    @Size(max = 160)
    String siteName
) {
}
