package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FpoMemberResponse(
    UUID id,
    UUID tenantId,
    UUID userId,
    String username,
    String memberNumber,
    String displayName,
    String mobileNumber,
    String alternateMobileNumber,
    String aadhaarNumber,
    String village,
    String taluka,
    String districtName,
    String stateName,
    String gender,
    LocalDate dateOfBirth,
    Integer age,
    String farmerCategory,
    UUID coordinatorUserId,
    String coordinatorName,
    FpoMemberStatus status,
    Instant createdAt,
    Instant updatedAt
) {
  public static FpoMemberResponse from(FpoMemberProfileEntity member) {
    return new FpoMemberResponse(
        member.getId(),
        member.getTenant().getId(),
        member.getUser().getId(),
        member.getUser().getUsername(),
        member.getMemberNumber(),
        member.getDisplayName(),
        member.getMobileNumber(),
        member.getAlternateMobileNumber(),
        member.getAadhaarNumber(),
        member.getVillage(),
        member.getTaluka(),
        member.getDistrictName(),
        member.getStateName(),
        member.getGender(),
        member.getDateOfBirth(),
        member.getAge(),
        member.getFarmerCategory(),
        member.getCoordinatorUser() == null ? null : member.getCoordinatorUser().getId(),
        member.getCoordinatorUser() == null ? null : member.getCoordinatorUser().getDisplayName(),
        member.getStatus(),
        member.getCreatedAt(),
        member.getUpdatedAt()
    );
  }
}
