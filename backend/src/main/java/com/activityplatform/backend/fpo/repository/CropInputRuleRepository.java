package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.CropInputRuleEntity;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropInputRuleRepository extends JpaRepository<CropInputRuleEntity, UUID> {
  List<CropInputRuleEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  List<CropInputRuleEntity> findByTenantIdAndCropIdAndStatus(
      UUID tenantId,
      UUID cropId,
      FarmRecordStatus status
  );

  List<CropInputRuleEntity> findByTenantIdAndCropIdAndInputId(
      UUID tenantId,
      UUID cropId,
      UUID inputId
  );

  Optional<CropInputRuleEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
