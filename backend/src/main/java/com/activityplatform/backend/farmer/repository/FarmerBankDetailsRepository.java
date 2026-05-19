package com.activityplatform.backend.farmer.repository;

import com.activityplatform.backend.farmer.domain.FarmerBankDetailsEntity;
import com.activityplatform.backend.farmer.domain.FarmerBankDetailsStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerBankDetailsRepository
    extends JpaRepository<FarmerBankDetailsEntity, UUID> {
  List<FarmerBankDetailsEntity> findByTenantIdAndStatusOrderByUpdatedAtAsc(
      UUID tenantId,
      FarmerBankDetailsStatus status
  );

  Optional<FarmerBankDetailsEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<FarmerBankDetailsEntity> findByTenantIdAndFarmerProfileId(
      UUID tenantId,
      UUID farmerProfileId
  );

  Optional<FarmerBankDetailsEntity> findByIdAndTenantIdAndFarmerProfileId(
      UUID id,
      UUID tenantId,
      UUID farmerProfileId
  );
}
