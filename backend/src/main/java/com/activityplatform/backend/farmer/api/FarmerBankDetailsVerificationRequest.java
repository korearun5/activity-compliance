package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.farmer.domain.FarmerBankDetailsStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FarmerBankDetailsVerificationRequest(
    @NotNull
    FarmerBankDetailsStatus status,
    @Size(max = 1000)
    String notes
) {
}
