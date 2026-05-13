package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateFpoMemberRequest(
    UUID userId,
    @Size(max = 120)
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
        message = "Username must use letters, numbers, dots, underscores, and hyphens."
    )
    String username,
    @Size(min = 8, max = 128)
    String password,
    @NotBlank
    @Size(max = 80)
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]*$",
        message = "Member number must use letters, numbers, dots, underscores, slashes, and hyphens."
    )
    String memberNumber,
    @NotBlank
    @Size(max = 180)
    String displayName,
    @NotBlank
    @Size(max = 40)
    @Pattern(
        regexp = "^\\+?[0-9][0-9\\s-]{6,30}$",
        message = "Mobile number format is invalid."
    )
    String mobileNumber,
    @Size(max = 40)
    @Pattern(
        regexp = "^\\s*$|^\\+?[0-9][0-9\\s-]{6,30}$",
        message = "Alternate mobile number format is invalid."
    )
    String alternateMobileNumber,
    @Size(max = 12)
    @Pattern(
        regexp = "^\\s*$|^[0-9]{12}$",
        message = "Aadhaar number must be 12 digits when provided."
    )
    String aadhaarNumber,
    @NotBlank
    @Size(max = 160)
    String village,
    @NotBlank
    @Size(max = 160)
    String taluka,
    @NotBlank
    @Size(max = 160)
    String districtName,
    @NotBlank
    @Size(max = 160)
    String stateName,
    @NotBlank
    @Size(max = 32)
    String gender,
    LocalDate dateOfBirth,
    @Min(0)
    @Max(120)
    Integer age,
    @NotBlank
    @Size(max = 80)
    String farmerCategory,
    UUID coordinatorUserId,
    FpoMemberStatus status
) {
}
