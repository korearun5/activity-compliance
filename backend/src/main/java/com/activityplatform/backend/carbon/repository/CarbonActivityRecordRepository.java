package com.activityplatform.backend.carbon.repository;

import com.activityplatform.backend.carbon.domain.CarbonActivityRecordEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonActivityRecordRepository
    extends JpaRepository<CarbonActivityRecordEntity, UUID> {
  Optional<CarbonActivityRecordEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  List<CarbonActivityRecordEntity> findByTenantIdOrderByActivityDateDescCreatedAtDesc(
      UUID tenantId
  );

  List<CarbonActivityRecordEntity> findByTenantIdAndCarbonProfileIdOrderByActivityDateDescCreatedAtDesc(
      UUID tenantId,
      UUID carbonProfileId
  );
}
