package com.activityplatform.backend.reporting.api;

import java.util.List;
import java.util.UUID;

public record ReportSummaryResponse(
    UUID tenantId,
    long fieldCoordinatorCount,
    long totalActivities,
    long runningActivities,
    long completedActivities,
    long cancelledActivities,
    long totalTasks,
    long completedTasks,
    long evidenceRecords,
    long submittedEvidence,
    long pendingReviewEvidence,
    long approvedEvidence,
    long rejectedEvidence,
    int taskCompletionPercent,
    int approvedEvidencePercent,
    List<ReportBreakdownResponse> byWorkflow,
    List<ReportBreakdownResponse> byLocation
) {
}
