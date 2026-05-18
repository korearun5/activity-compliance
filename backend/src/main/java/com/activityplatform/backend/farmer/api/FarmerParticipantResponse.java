package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import com.activityplatform.backend.farmer.service.FarmerParticipant;
import java.util.UUID;

public record FarmerParticipantResponse(
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
  public static FarmerParticipantResponse from(FarmerParticipant participant) {
    return new FarmerParticipantResponse(
        participant.farmerProfileId(),
        participant.userId(),
        participant.username(),
        participant.displayName(),
        participant.mobileNumber(),
        participant.village(),
        participant.taluka(),
        participant.districtName(),
        participant.stateName(),
        participant.status()
    );
  }
}
