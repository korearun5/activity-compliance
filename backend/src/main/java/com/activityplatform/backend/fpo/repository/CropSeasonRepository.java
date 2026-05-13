package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.CropSeasonEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropSeasonRepository extends JpaRepository<CropSeasonEntity, UUID> {
  List<CropSeasonEntity> findByTenantIdOrderBySeasonYearDescNameAsc(UUID tenantId);

  Optional<CropSeasonEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<CropSeasonEntity> findByTenantIdAndCodeIgnoreCaseAndSeasonYear(
      UUID tenantId,
      String code,
      Integer seasonYear
  );
}
