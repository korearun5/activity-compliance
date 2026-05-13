package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.FarmerCropHistoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerCropHistoryRepository
    extends JpaRepository<FarmerCropHistoryEntity, UUID> {
  List<FarmerCropHistoryEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  List<FarmerCropHistoryEntity> findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID memberProfileId
  );

  Optional<FarmerCropHistoryEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
