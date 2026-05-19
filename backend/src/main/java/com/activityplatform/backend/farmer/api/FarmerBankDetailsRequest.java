package com.activityplatform.backend.farmer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FarmerBankDetailsRequest(
    @NotBlank
    @Size(max = 180)
    String accountHolderName,
    @NotBlank
    @Size(max = 64)
    String accountNumber,
    @NotBlank
    @Size(min = 11, max = 11)
    @Pattern(
        regexp = "(?i)^[A-Z]{4}0[A-Z0-9]{6}$",
        message = "IFSC code format is invalid."
    )
    String ifscCode,
    @Size(max = 120)
    String upiId,
    @NotBlank
    @Size(max = 180)
    String bankName
) {
}
