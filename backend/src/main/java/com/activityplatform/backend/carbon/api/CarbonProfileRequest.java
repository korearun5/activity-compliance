package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonParticipantType;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CarbonProfileRequest(
    UUID userId,
    UUID fpoMemberProfileId,
    UUID coordinatorUserId,
    @Size(max = 80)
    String carbonIdentityId,
    CarbonParticipantType participantType,
    @Size(max = 180)
    String displayName,
    @Size(max = 40)
    String mobileNumber,
    @Size(max = 40)
    String languagePreference,
    @Size(max = 160)
    String village,
    @Size(max = 160)
    String taluka,
    @Size(max = 160)
    String districtName,
    @Size(max = 160)
    String stateName,
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
