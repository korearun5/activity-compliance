package com.activityplatform.backend.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_modules")
public class PlatformModuleEntity {
  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true)
  private ModuleCode code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlatformModuleStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PlatformModuleEntity() {
  }

  public PlatformModuleEntity(
      UUID id,
      ModuleCode code,
      String name,
      String description,
      PlatformModuleStatus status,
      Instant now
  ) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.description = description;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public ModuleCode getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public PlatformModuleStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
