package com.activityplatform.backend.farmer.api;

public record FarmerProfileCompletionStepResponse(
    String code,
    String label,
    String status,
    boolean complete,
    boolean required,
    boolean comingSoon,
    String description
) {
}
