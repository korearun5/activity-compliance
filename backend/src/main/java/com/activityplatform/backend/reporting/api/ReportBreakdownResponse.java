package com.activityplatform.backend.reporting.api;

public record ReportBreakdownResponse(
    String label,
    long activities,
    long completedActivities,
    long evidenceRecords,
    long approvedEvidence,
    int taskCompletionPercent
) {
}
