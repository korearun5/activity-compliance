package com.activityplatform.backend.farmer.repository;

import com.activityplatform.backend.farmer.domain.FarmerDocumentEntity;
import com.activityplatform.backend.farmer.domain.FarmerDocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerDocumentRepository extends JpaRepository<FarmerDocumentEntity, UUID> {
  List<FarmerDocumentEntity> findByTenantIdAndFarmerProfileIdOrderByUploadedAtDesc(
      UUID tenantId,
      UUID farmerProfileId
  );

  List<FarmerDocumentEntity> findByTenantIdAndStatusOrderByUploadedAtAsc(
      UUID tenantId,
      FarmerDocumentStatus status
  );

  Optional<FarmerDocumentEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<FarmerDocumentEntity> findByIdAndTenantIdAndFarmerProfileId(
      UUID id,
      UUID tenantId,
      UUID farmerProfileId
  );

  boolean existsByTenantIdAndFarmerProfileId(UUID tenantId, UUID farmerProfileId);
}
