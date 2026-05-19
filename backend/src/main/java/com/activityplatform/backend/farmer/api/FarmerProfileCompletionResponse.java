package com.activityplatform.backend.farmer.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FarmerProfileCompletionResponse(
    UUID farmerProfileId,
    UUID userId,
    UUID carbonProfileId,
    int completionPercentage,
    int completedRequiredSteps,
    int totalRequiredSteps,
    List<FarmerProfileCompletionStepResponse> steps,
    Instant generatedAt
) {
}
