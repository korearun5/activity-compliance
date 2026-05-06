package com.activityplatform.backend.activity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record StartActivityRequest(
    @NotNull UUID workflowDefinitionId,
    UUID participantUserId,
    @NotBlank
    @Size(max = 180)
    String unitName,
    @Size(max = 180)
    String locationName,
    LocalDate startedOn
) {
}
