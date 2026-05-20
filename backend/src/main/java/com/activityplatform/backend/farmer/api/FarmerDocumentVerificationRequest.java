package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.farmer.domain.FarmerDocumentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FarmerDocumentVerificationRequest(
    @NotNull
    FarmerDocumentStatus status,
    @Size(max = 1000)
    String notes
) {
}
