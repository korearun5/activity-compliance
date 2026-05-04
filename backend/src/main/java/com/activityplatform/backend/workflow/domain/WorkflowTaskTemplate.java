package com.activityplatform.backend.workflow.domain;

public record WorkflowTaskTemplate(
    String code,
    String title,
    int sequenceNumber,
    int offsetDays,
    boolean evidenceRequired
) {
}

