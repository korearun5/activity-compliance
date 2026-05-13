package com.activityplatform.backend.fpo.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.notification.domain.NotificationChannel;
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
import java.util.UUID;

@Entity
@Table(name = "fpo_advisories")
public class FpoAdvisoryEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crop_id")
  private CropCatalogEntity crop;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "season_id")
  private CropSeasonEntity season;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false)
  private AdvisoryTargetType targetType;

  @Column(name = "target_village")
  private String targetVillage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_member_id")
  private FpoMemberProfileEntity targetMember;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationChannel channel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AdvisoryStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  private UserEntity createdBy;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FpoAdvisoryEntity() {
  }

  public FpoAdvisoryEntity(
      UUID id,
      TenantEntity tenant,
      CropCatalogEntity crop,
      CropSeasonEntity season,
      AdvisoryTargetType targetType,
      String targetVillage,
      FpoMemberProfileEntity targetMember,
      String title,
      String message,
      NotificationChannel channel,
      AdvisoryStatus status,
      UserEntity createdBy,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.crop = crop;
    this.season = season;
    this.targetType = targetType;
    this.targetVillage = targetVillage;
    this.targetMember = targetMember;
    this.title = title;
    this.message = message;
    this.channel = channel;
    this.status = status;
    this.createdBy = createdBy;
    this.publishedAt = status == AdvisoryStatus.PUBLISHED ? now : null;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public CropCatalogEntity getCrop() {
    return crop;
  }

  public CropSeasonEntity getSeason() {
    return season;
  }

  public AdvisoryTargetType getTargetType() {
    return targetType;
  }

  public String getTargetVillage() {
    return targetVillage;
  }

  public FpoMemberProfileEntity getTargetMember() {
    return targetMember;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public AdvisoryStatus getStatus() {
    return status;
  }

  public UserEntity getCreatedBy() {
    return createdBy;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateStatus(AdvisoryStatus status, Instant now) {
    this.status = status;
    if (status == AdvisoryStatus.PUBLISHED && publishedAt == null) {
      this.publishedAt = now;
    }
    this.updatedAt = now;
  }
}
