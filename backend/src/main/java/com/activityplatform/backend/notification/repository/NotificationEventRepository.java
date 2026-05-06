package com.activityplatform.backend.notification.repository;

import com.activityplatform.backend.notification.domain.NotificationEventEntity;
import com.activityplatform.backend.notification.domain.NotificationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEventEntity, UUID> {
  Page<NotificationEventEntity> findByTenantId(UUID tenantId, Pageable pageable);

  Page<NotificationEventEntity> findByTenantIdAndStatus(
      UUID tenantId,
      NotificationStatus status,
      Pageable pageable
  );

  Optional<NotificationEventEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
