package com.activityplatform.backend.carbon.repository;

import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonProfileRepository extends JpaRepository<CarbonProfileEntity, UUID> {
  Page<CarbonProfileEntity> findByTenantId(UUID tenantId, Pageable pageable);

  Page<CarbonProfileEntity> findByTenantIdAndStatus(
      UUID tenantId,
      CarbonRecordStatus status,
      Pageable pageable
  );

  Page<CarbonProfileEntity> findByTenantIdAndCoordinatorUserId(
      UUID tenantId,
      UUID coordinatorUserId,
      Pageable pageable
  );

  Page<CarbonProfileEntity> findByTenantIdAndCoordinatorUserIdAndStatus(
      UUID tenantId,
      UUID coordinatorUserId,
      CarbonRecordStatus status,
      Pageable pageable
  );

  Optional<CarbonProfileEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<CarbonProfileEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

  Optional<CarbonProfileEntity> findFirstByTenant_IdAndFarmerProfile_IdOrderByUpdatedAtDesc(
      UUID tenantId,
      UUID farmerProfileId
  );

  Optional<CarbonProfileEntity> findByTenantIdAndCarbonIdentityIdIgnoreCase(
      UUID tenantId,
      String carbonIdentityId
  );
}
