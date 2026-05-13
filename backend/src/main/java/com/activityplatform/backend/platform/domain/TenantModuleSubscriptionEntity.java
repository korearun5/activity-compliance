package com.activityplatform.backend.platform.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
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
@Table(name = "tenant_module_subscriptions")
public class TenantModuleSubscriptionEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "module_id", nullable = false)
  private PlatformModuleEntity module;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TenantModuleSubscriptionStatus status;

  @Column(name = "enabled_at")
  private Instant enabledAt;

  @Column(name = "disabled_at")
  private Instant disabledAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TenantModuleSubscriptionEntity() {
  }

  public TenantModuleSubscriptionEntity(
      UUID id,
      TenantEntity tenant,
      PlatformModuleEntity module,
      TenantModuleSubscriptionStatus status,
      Instant enabledAt,
      Instant disabledAt,
      Instant expiresAt,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.module = module;
    this.status = status;
    this.enabledAt = enabledAt;
    this.disabledAt = disabledAt;
    this.expiresAt = expiresAt;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public PlatformModuleEntity getModule() {
    return module;
  }

  public TenantModuleSubscriptionStatus getStatus() {
    return status;
  }

  public Instant getEnabledAt() {
    return enabledAt;
  }

  public Instant getDisabledAt() {
    return disabledAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public boolean isEnabledAt(Instant now) {
    return status == TenantModuleSubscriptionStatus.ENABLED
        && (expiresAt == null || expiresAt.isAfter(now));
  }

  public void update(
      TenantModuleSubscriptionStatus status,
      Instant expiresAt,
      Instant now
  ) {
    this.status = status;
    this.expiresAt = expiresAt;
    this.updatedAt = now;
    if (status == TenantModuleSubscriptionStatus.ENABLED) {
      this.enabledAt = now;
      this.disabledAt = null;
    } else {
      this.disabledAt = now;
    }
  }
}
