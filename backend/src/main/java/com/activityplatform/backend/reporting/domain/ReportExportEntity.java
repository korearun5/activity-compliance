package com.activityplatform.backend.reporting.domain;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "report_exports")
public class ReportExportEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by_user_id")
  private UserEntity requestedBy;

  @Column(name = "report_type", nullable = false)
  private String reportType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReportFormat format;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "filter_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> filters = new LinkedHashMap<>();

  @Column(name = "storage_key")
  private String storageKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReportStatus status;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected ReportExportEntity() {
  }

  public ReportExportEntity(
      UUID id,
      TenantEntity tenant,
      UserEntity requestedBy,
      String reportType,
      ReportFormat format,
      Map<String, Object> filters,
      Instant requestedAt
  ) {
    this.id = id;
    this.tenant = tenant;
    this.requestedBy = requestedBy;
    this.reportType = reportType;
    this.format = format;
    this.filters = filters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(filters);
    this.status = ReportStatus.QUEUED;
    this.requestedAt = requestedAt;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public UserEntity getRequestedBy() {
    return requestedBy;
  }

  public String getReportType() {
    return reportType;
  }

  public ReportFormat getFormat() {
    return format;
  }

  public Map<String, Object> getFilters() {
    return filters;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public ReportStatus getStatus() {
    return status;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void complete(String storageKey, Instant completedAt) {
    this.storageKey = storageKey;
    this.status = ReportStatus.COMPLETED;
    this.completedAt = completedAt;
  }
}
