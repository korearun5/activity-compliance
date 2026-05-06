package com.activityplatform.backend.workflow.api;

import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WorkflowRequest(
    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-]*$", message = "Workflow code must use letters, numbers, and hyphens.")
    String code,
    @NotBlank
    @Size(max = 180)
    String name,
    @Size(max = 80)
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "Domain key must use letters, numbers, underscores, and hyphens.")
    String domainKey,
    @Min(1) int durationDays,
    @Min(1) int version,
    WorkflowDefinitionStatus status,
    @NotEmpty List<@Valid WorkflowTaskRequest> tasks
) {
}
