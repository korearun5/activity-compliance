package com.activityplatform.backend.workflow.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkflowTaskRequest(
    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-]*$", message = "Task code must use letters, numbers, and hyphens.")
    String code,
    @NotBlank
    @Size(max = 180)
    String title,
    @Min(1) int sequenceNumber,
    @Min(0) int offsetDays,
    boolean evidenceRequired
) {
}
