package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonParticipantType;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CarbonProfileRequest(
    UUID userId,
    UUID fpoMemberProfileId,
    UUID coordinatorUserId,
    @Size(max = 120)
    @Pattern(
        regexp = "^$|^[A-Za-z0-9][A-Za-z0-9._-]*$",
        message = "Username must use letters, numbers, dots, underscores, and hyphens."
    )
    String username,
    @Size(min = 8, max = 128)
    String password,
    @Size(max = 80)
    @Pattern(
        regexp = "^$|^[A-Za-z0-9][A-Za-z0-9._/-]*$",
        message = "Member number must use letters, numbers, dots, underscores, slashes, and hyphens."
    )
    String memberNumber,
    @Size(max = 80)
    String carbonIdentityId,
    CarbonParticipantType participantType,
    @Size(max = 180)
    String displayName,
    @Size(max = 40)
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
    @Size(max = 160)
    String village,
    @Size(max = 160)
    String taluka,
    @Size(max = 160)
    String districtName,
    @Size(max = 160)
    String stateName,
    @Size(max = 32)
    String gender,
    @Min(0)
    @Max(120)
    Integer age,
    @Size(max = 80)
    String farmerCategory,
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.")
    @Digits(integer = 3, fraction = 7)
    BigDecimal gpsLatitude,
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.")
    @Digits(integer = 3, fraction = 7)
    BigDecimal gpsLongitude,
    @DecimalMin(value = "0.0", message = "Total land holding cannot be negative.")
    @Digits(integer = 8, fraction = 4)
    BigDecimal totalLandHoldingAcres,
    @Size(max = 2000)
    String croppingPattern,
    @Min(value = 0, message = "Livestock count cannot be negative.")
    Integer livestockCount,
    @Size(max = 80)
    String tillageStatus,
    @Size(max = 80)
    String bankStatus,
    @Size(max = 80)
    String aadhaarStatus,
    @Size(max = 80)
    String documentStatus,
    CarbonRecordStatus status
) {
}
