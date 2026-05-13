package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.FarmLandholdingEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmLandholdingRepository extends JpaRepository<FarmLandholdingEntity, UUID> {
  List<FarmLandholdingEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  List<FarmLandholdingEntity> findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID memberProfileId
  );

  Optional<FarmLandholdingEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
