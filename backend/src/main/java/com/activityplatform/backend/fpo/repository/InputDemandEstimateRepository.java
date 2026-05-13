package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.InputDemandEstimateEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InputDemandEstimateRepository
    extends JpaRepository<InputDemandEstimateEntity, UUID> {
  List<InputDemandEstimateEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  Optional<InputDemandEstimateEntity> findByTenantIdAndCropPlanIdAndInputId(
      UUID tenantId,
      UUID cropPlanId,
      UUID inputId
  );
}
