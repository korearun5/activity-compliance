package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonalCropPlanRepository
    extends JpaRepository<SeasonalCropPlanEntity, UUID> {
  List<SeasonalCropPlanEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  List<SeasonalCropPlanEntity> findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID memberProfileId
  );

  Optional<SeasonalCropPlanEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
