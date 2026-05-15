package com.activityplatform.backend.carbon.repository;

import com.activityplatform.backend.carbon.domain.CarbonFarmPlotEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonFarmPlotRepository extends JpaRepository<CarbonFarmPlotEntity, UUID> {
  List<CarbonFarmPlotEntity> findByTenantIdAndCarbonProfileIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID carbonProfileId
  );

  Optional<CarbonFarmPlotEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
