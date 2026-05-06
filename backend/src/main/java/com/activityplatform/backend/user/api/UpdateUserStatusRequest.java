package com.activityplatform.backend.user.api;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull
    UserStatus status
) {
}
