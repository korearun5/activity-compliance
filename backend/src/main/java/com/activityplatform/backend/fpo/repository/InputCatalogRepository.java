package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.InputCatalogEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InputCatalogRepository extends JpaRepository<InputCatalogEntity, UUID> {
  List<InputCatalogEntity> findByTenantIdOrderByNameAsc(UUID tenantId);

  Optional<InputCatalogEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<InputCatalogEntity> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
}
