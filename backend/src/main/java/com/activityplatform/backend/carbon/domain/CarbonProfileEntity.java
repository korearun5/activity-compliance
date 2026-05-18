package com.activityplatform.backend.carbon.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carbon_profiles")
public class CarbonProfileEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fpo_member_profile_id")
  private FpoMemberProfileEntity fpoMemberProfile;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "farmer_profile_id")
  private FarmerProfileEntity farmerProfile;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coordinator_user_id")
  private UserEntity coordinatorUser;

  @Column(name = "username")
  private String username;

  @Column(name = "member_number")
  private String memberNumber;

  @Column(name = "carbon_identity_id", nullable = false)
  private String carbonIdentityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "participant_type", nullable = false)
  private CarbonParticipantType participantType;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "mobile_number")
  private String mobileNumber;

  @Column(name = "alternate_mobile_number")
  private String alternateMobileNumber;

  @Column(name = "aadhaar_number")
  private String aadhaarNumber;

  private String village;

  private String taluka;

  @Column(name = "district_name")
  private String districtName;

  @Column(name = "state_name")
  private String stateName;

  private String gender;

  private Integer age;

  @Column(name = "farmer_category")
  private String farmerCategory;

  @Column(name = "gps_latitude")
  private BigDecimal gpsLatitude;

  @Column(name = "gps_longitude")
  private BigDecimal gpsLongitude;

  @Column(name = "total_land_holding_acres")
  private BigDecimal totalLandHoldingAcres;

  @Column(name = "cropping_pattern")
  private String croppingPattern;

  @Column(name = "livestock_count")
  private Integer livestockCount;

  @Column(name = "tillage_status")
  private String tillageStatus;

  @Column(name = "bank_status")
  private String bankStatus;

  @Column(name = "aadhaar_status")
  private String aadhaarStatus;

  @Column(name = "document_status")
  private String documentStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CarbonRecordStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CarbonProfileEntity() {
  }

  public CarbonProfileEntity(
      UUID id,
      TenantEntity tenant,
      UserEntity user,
      FpoMemberProfileEntity fpoMemberProfile,
      UserEntity coordinatorUser,
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
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.user = user;
    this.fpoMemberProfile = fpoMemberProfile;
    this.coordinatorUser = coordinatorUser;
    this.username = username;
    this.memberNumber = memberNumber;
    this.carbonIdentityId = carbonIdentityId;
    this.participantType = participantType;
    this.displayName = displayName;
    this.mobileNumber = mobileNumber;
    this.alternateMobileNumber = alternateMobileNumber;
    this.aadhaarNumber = aadhaarNumber;
    this.village = village;
    this.taluka = taluka;
    this.districtName = districtName;
    this.stateName = stateName;
    this.gender = gender;
    this.age = age;
    this.farmerCategory = farmerCategory;
    this.gpsLatitude = gpsLatitude;
    this.gpsLongitude = gpsLongitude;
    this.totalLandHoldingAcres = totalLandHoldingAcres;
    this.croppingPattern = croppingPattern;
    this.livestockCount = livestockCount;
    this.tillageStatus = tillageStatus;
    this.bankStatus = bankStatus;
    this.aadhaarStatus = aadhaarStatus;
    this.documentStatus = documentStatus;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public UserEntity getUser() {
    return user;
  }

  public FpoMemberProfileEntity getFpoMemberProfile() {
    return fpoMemberProfile;
  }

  public FarmerProfileEntity getFarmerProfile() {
    return farmerProfile;
  }

  public UUID getFarmerProfileId() {
    return farmerProfile == null ? null : farmerProfile.getId();
  }

  public UserEntity getCoordinatorUser() {
    return coordinatorUser;
  }

  public String getUsername() {
    return username;
  }

  public String getMemberNumber() {
    return memberNumber;
  }

  public String getCarbonIdentityId() {
    return carbonIdentityId;
  }

  public CarbonParticipantType getParticipantType() {
    return participantType;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public String getAlternateMobileNumber() {
    return alternateMobileNumber;
  }

  public String getAadhaarNumber() {
    return aadhaarNumber;
  }

  public String getVillage() {
    return village;
  }

  public String getTaluka() {
    return taluka;
  }

  public String getDistrictName() {
    return districtName;
  }

  public String getStateName() {
    return stateName;
  }

  public String getGender() {
    return gender;
  }

  public Integer getAge() {
    return age;
  }

  public String getFarmerCategory() {
    return farmerCategory;
  }

  public BigDecimal getGpsLatitude() {
    return gpsLatitude;
  }

  public BigDecimal getGpsLongitude() {
    return gpsLongitude;
  }

  public BigDecimal getTotalLandHoldingAcres() {
    return totalLandHoldingAcres;
  }

  public String getCroppingPattern() {
    return croppingPattern;
  }

  public Integer getLivestockCount() {
    return livestockCount;
  }

  public String getTillageStatus() {
    return tillageStatus;
  }

  public String getBankStatus() {
    return bankStatus;
  }

  public String getAadhaarStatus() {
    return aadhaarStatus;
  }

  public String getDocumentStatus() {
    return documentStatus;
  }

  public CarbonRecordStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateDetails(
      UserEntity user,
      FpoMemberProfileEntity fpoMemberProfile,
      UserEntity coordinatorUser,
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
      Instant now
  ) {
    this.user = user;
    this.fpoMemberProfile = fpoMemberProfile;
    this.coordinatorUser = coordinatorUser;
    this.username = username;
    this.memberNumber = memberNumber;
    this.carbonIdentityId = carbonIdentityId;
    this.participantType = participantType;
    this.displayName = displayName;
    this.mobileNumber = mobileNumber;
    this.alternateMobileNumber = alternateMobileNumber;
    this.aadhaarNumber = aadhaarNumber;
    this.village = village;
    this.taluka = taluka;
    this.districtName = districtName;
    this.stateName = stateName;
    this.gender = gender;
    this.age = age;
    this.farmerCategory = farmerCategory;
    this.gpsLatitude = gpsLatitude;
    this.gpsLongitude = gpsLongitude;
    this.totalLandHoldingAcres = totalLandHoldingAcres;
    this.croppingPattern = croppingPattern;
    this.livestockCount = livestockCount;
    this.tillageStatus = tillageStatus;
    this.bankStatus = bankStatus;
    this.aadhaarStatus = aadhaarStatus;
    this.documentStatus = documentStatus;
    this.status = status;
    this.updatedAt = now;
  }

  public void linkFarmerProfile(FarmerProfileEntity farmerProfile, Instant now) {
    this.farmerProfile = farmerProfile;
    this.updatedAt = now;
  }
}
