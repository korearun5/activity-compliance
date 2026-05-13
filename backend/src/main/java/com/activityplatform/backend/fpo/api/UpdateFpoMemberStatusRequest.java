package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFpoMemberStatusRequest(
    @NotNull
    FpoMemberStatus status
) {
}
