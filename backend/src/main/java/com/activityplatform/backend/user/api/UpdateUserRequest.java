package com.activityplatform.backend.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
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
