package com.activityplatform.backend.fpo.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fpo_member_profiles")
public class FpoMemberProfileEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "member_number", nullable = false)
  private String memberNumber;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "mobile_number", nullable = false)
  private String mobileNumber;

  @Column(name = "alternate_mobile_number")
  private String alternateMobileNumber;

  @Column(nullable = false)
  private String village;

  @Column(name = "block_name")
  private String blockName;

  @Column(name = "district_name")
  private String districtName;

  private String gender;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  private Integer age;

  @Column(name = "farmer_category")
  private String farmerCategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coordinator_user_id")
  private UserEntity coordinatorUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FpoMemberStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FpoMemberProfileEntity() {
  }

  public FpoMemberProfileEntity(
      UUID id,
      TenantEntity tenant,
      UserEntity user,
      String memberNumber,
      String displayName,
      String mobileNumber,
      String alternateMobileNumber,
      String village,
      String blockName,
      String districtName,
      String gender,
      LocalDate dateOfBirth,
      Integer age,
      String farmerCategory,
      UserEntity coordinatorUser,
      FpoMemberStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.user = user;
    this.memberNumber = memberNumber;
    this.displayName = displayName;
    this.mobileNumber = mobileNumber;
    this.alternateMobileNumber = alternateMobileNumber;
    this.village = village;
    this.blockName = blockName;
    this.districtName = districtName;
    this.gender = gender;
    this.dateOfBirth = dateOfBirth;
    this.age = age;
    this.farmerCategory = farmerCategory;
    this.coordinatorUser = coordinatorUser;
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

  public String getMemberNumber() {
    return memberNumber;
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

  public String getVillage() {
    return village;
  }

  public String getBlockName() {
    return blockName;
  }

  public String getDistrictName() {
    return districtName;
  }

  public String getGender() {
    return gender;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public Integer getAge() {
    return age;
  }

  public String getFarmerCategory() {
    return farmerCategory;
  }

  public UserEntity getCoordinatorUser() {
    return coordinatorUser;
  }

  public FpoMemberStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateDetails(
      String memberNumber,
      String displayName,
      String mobileNumber,
      String alternateMobileNumber,
      String village,
      String blockName,
      String districtName,
      String gender,
      LocalDate dateOfBirth,
      Integer age,
      String farmerCategory,
      UserEntity coordinatorUser,
      FpoMemberStatus status,
      Instant now
  ) {
    this.memberNumber = memberNumber;
    this.displayName = displayName;
    this.mobileNumber = mobileNumber;
    this.alternateMobileNumber = alternateMobileNumber;
    this.village = village;
    this.blockName = blockName;
    this.districtName = districtName;
    this.gender = gender;
    this.dateOfBirth = dateOfBirth;
    this.age = age;
    this.farmerCategory = farmerCategory;
    this.coordinatorUser = coordinatorUser;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateStatus(FpoMemberStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }
}
