package com.activityplatform.backend.farmer.service;

import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import java.util.UUID;

public record FarmerParticipant(
    UUID farmerProfileId,
    UUID userId,
    String username,
    String displayName,
    String mobileNumber,
    String village,
    String taluka,
    String districtName,
    String stateName,
    FarmerProfileStatus status
) {
}
