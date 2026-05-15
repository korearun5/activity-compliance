package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.SeasonalCropPlanEntity;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
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

  List<SeasonalCropPlanEntity> findByTenantIdAndMemberProfileCoordinatorUserIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID coordinatorUserId
  );

  List<SeasonalCropPlanEntity> findByTenantIdAndMemberProfileUserIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID userId
  );

  boolean existsByTenantIdAndMemberProfileIdAndCropIdAndStatus(
      UUID tenantId,
      UUID memberProfileId,
      UUID cropId,
      CropPlanStatus status
  );

  Optional<SeasonalCropPlanEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
