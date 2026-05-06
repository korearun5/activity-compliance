package com.activityplatform.backend.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @Column(nullable = false)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  private String phone;

  @Column(name = "location_name")
  private String locationName;

  @Column(name = "site_name")
  private String siteName;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<RoleEntity> roles = new LinkedHashSet<>();

  protected UserEntity() {
  }

  public UserEntity(
      UUID id,
      TenantEntity tenant,
      String username,
      String passwordHash,
      String displayName,
      String phone,
      String locationName,
      String siteName,
      String status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.username = username;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.phone = phone;
    this.locationName = locationName;
    this.siteName = siteName;
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

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPhone() {
    return phone;
  }

  public String getLocationName() {
    return locationName;
  }

  public String getSiteName() {
    return siteName;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Set<RoleEntity> getRoles() {
    return roles;
  }

  public void updateProfile(
      String displayName,
      String phone,
      String locationName,
      String siteName,
      Instant updatedAt
  ) {
    this.displayName = displayName;
    this.phone = phone;
    this.locationName = locationName;
    this.siteName = siteName;
    this.updatedAt = updatedAt;
  }

  public void updateStatus(String status, Instant updatedAt) {
    this.status = status;
    this.updatedAt = updatedAt;
  }

  public void addRole(RoleEntity role) {
    roles.add(role);
  }

  public void replaceRoles(Set<RoleEntity> roles, Instant updatedAt) {
    this.roles.clear();
    this.roles.addAll(roles);
    this.updatedAt = updatedAt;
  }
}
