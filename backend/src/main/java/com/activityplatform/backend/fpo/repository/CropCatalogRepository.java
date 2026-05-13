package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.CropCatalogEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropCatalogRepository extends JpaRepository<CropCatalogEntity, UUID> {
  List<CropCatalogEntity> findByTenantIdOrderByNameAsc(UUID tenantId);

  Optional<CropCatalogEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<CropCatalogEntity> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
}
