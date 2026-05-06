package com.activityplatform.backend.evidence.repository;

import com.activityplatform.backend.evidence.domain.EvidenceEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<EvidenceEntity, UUID> {
  List<EvidenceEntity> findByTenantIdOrderBySubmittedAtDesc(UUID tenantId);

  List<EvidenceEntity> findByTenantIdAndActivityTaskActivityIdOrderBySubmittedAtDesc(
      UUID tenantId,
      UUID activityId
  );

  Optional<EvidenceEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
