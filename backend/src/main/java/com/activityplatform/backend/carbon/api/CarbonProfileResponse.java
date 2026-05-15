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
    String carbonIdentityId,
    CarbonParticipantType participantType,
    String displayName,
    String mobileNumber,
    String languagePreference,
    String village,
    String taluka,
    String districtName,
    String stateName,
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
        profile.getCarbonIdentityId(),
        profile.getParticipantType(),
        profile.getDisplayName(),
        profile.getMobileNumber(),
        profile.getLanguagePreference(),
        profile.getVillage(),
        profile.getTaluka(),
        profile.getDistrictName(),
        profile.getStateName(),
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
