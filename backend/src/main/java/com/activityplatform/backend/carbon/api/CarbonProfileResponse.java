package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonParticipantType;
import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CarbonProfileResponse(
    UUID id,
    UUID tenantId,
    UUID userId,
    UUID fpoMemberProfileId,
    UUID coordinatorUserId,
    String username,
    String memberNumber,
    String carbonIdentityId,
    CarbonParticipantType participantType,
    String displayName,
    String mobileNumber,
    String alternateMobileNumber,
    String aadhaarNumber,
    String village,
    String taluka,
    String districtName,
    String stateName,
    String gender,
    Integer age,
    String farmerCategory,
    BigDecimal gpsLatitude,
    BigDecimal gpsLongitude,
    BigDecimal totalLandHoldingAcres,
    String croppingPattern,
    Integer livestockCount,
    String tillageStatus,
    String bankStatus,
    String aadhaarStatus,
    String documentStatus,
    CarbonRecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static CarbonProfileResponse from(CarbonProfileEntity profile) {
    return new CarbonProfileResponse(
        profile.getId(),
        profile.getTenant().getId(),
        profile.getUser() == null ? null : profile.getUser().getId(),
        profile.getFpoMemberProfile() == null ? null : profile.getFpoMemberProfile().getId(),
        profile.getCoordinatorUser() == null ? null : profile.getCoordinatorUser().getId(),
        profile.getUsername() == null && profile.getUser() != null
            ? profile.getUser().getUsername()
            : profile.getUsername(),
        profile.getMemberNumber() == null && profile.getFpoMemberProfile() != null
            ? profile.getFpoMemberProfile().getMemberNumber()
            : profile.getMemberNumber(),
        profile.getCarbonIdentityId(),
        profile.getParticipantType(),
        profile.getDisplayName(),
        profile.getMobileNumber(),
        profile.getAlternateMobileNumber(),
        profile.getAadhaarNumber(),
        profile.getVillage(),
        profile.getTaluka(),
        profile.getDistrictName(),
        profile.getStateName(),
        profile.getGender(),
        profile.getAge(),
        profile.getFarmerCategory(),
        profile.getGpsLatitude(),
        profile.getGpsLongitude(),
        profile.getTotalLandHoldingAcres(),
        profile.getCroppingPattern(),
        profile.getLivestockCount(),
        profile.getTillageStatus(),
        profile.getBankStatus(),
        profile.getAadhaarStatus(),
        profile.getDocumentStatus(),
        profile.getStatus(),
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
  }
}
