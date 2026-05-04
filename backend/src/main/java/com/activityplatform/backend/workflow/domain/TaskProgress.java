package com.activityplatform.backend.workflow.domain;

public record TaskProgress(
    String taskCode,
    int sequenceNumber,
    TaskStatus status
) {
}

