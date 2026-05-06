package com.activityplatform.backend.role.api;

import com.activityplatform.backend.security.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpdateUserRolesRequest(
    @NotEmpty Set<@NotNull Role> roles
) {
}
