package com.activityplatform.backend.activity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.activityplatform.backend.activity.domain.ActivityEntity;
import com.activityplatform.backend.activity.domain.ActivityStatus;

public interface ActivityRepository extends JpaRepository<ActivityEntity, UUID> {
    List<ActivityEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<ActivityEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId,
            ActivityStatus status);

    List<ActivityEntity> findByTenantIdAndParticipantIdOrderByCreatedAtDesc(
            UUID tenantId,
            UUID participantId);

    List<ActivityEntity> findByTenantIdAndParticipantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId,
            UUID participantId,
            ActivityStatus status);

    // Paginated queries
    Page<ActivityEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ActivityEntity> findByTenantIdAndStatus(UUID tenantId, ActivityStatus status, Pageable pageable);

    Page<ActivityEntity> findByTenantIdAndParticipantId(UUID tenantId, UUID participantId, Pageable pageable);

    Page<ActivityEntity> findByTenantIdAndParticipantIdAndStatus(
            UUID tenantId,
            UUID participantId,
            ActivityStatus status,
            Pageable pageable);

    Optional<ActivityEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByWorkflowDefinitionId(UUID workflowDefinitionId);
}
