package com.activityplatform.backend.audit.domain;

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
@Table(name = "audit_events")
public class AuditEventEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id")
  private UserEntity actor;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuditAction action;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> metadata = new LinkedHashMap<>();

  @Column(name = "request_id")
  private String requestId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected AuditEventEntity() {
  }

  public AuditEventEntity(
      UUID id,
      TenantEntity tenant,
      UserEntity actor,
      String aggregateType,
      UUID aggregateId,
      AuditAction action,
      Map<String, Object> metadata,
      String requestId,
      Instant occurredAt
  ) {
    this.id = id;
    this.tenant = tenant;
    this.actor = actor;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.action = action;
    this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    this.requestId = requestId;
    this.occurredAt = occurredAt;
  }

  public UUID getId() {
    return id;
  }

  public AuditAction getAction() {
    return action;
  }
}
