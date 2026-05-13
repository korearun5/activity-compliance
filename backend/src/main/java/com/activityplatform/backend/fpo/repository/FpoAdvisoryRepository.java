package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.FpoAdvisoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FpoAdvisoryRepository extends JpaRepository<FpoAdvisoryEntity, UUID> {
  List<FpoAdvisoryEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  Optional<FpoAdvisoryEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
