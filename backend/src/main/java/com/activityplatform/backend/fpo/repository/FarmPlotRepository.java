package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.FarmPlotEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmPlotRepository extends JpaRepository<FarmPlotEntity, UUID> {
  List<FarmPlotEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  List<FarmPlotEntity> findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID memberProfileId
  );

  Optional<FarmPlotEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
