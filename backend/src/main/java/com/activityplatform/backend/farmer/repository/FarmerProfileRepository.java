package com.activityplatform.backend.farmer.repository;

import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerProfileRepository extends JpaRepository<FarmerProfileEntity, UUID> {
  Page<FarmerProfileEntity> findByTenantId(UUID tenantId, Pageable pageable);

  Page<FarmerProfileEntity> findByTenantIdAndStatus(
      UUID tenantId,
      FarmerProfileStatus status,
      Pageable pageable
  );

  List<FarmerProfileEntity> findByTenantIdOrderByDisplayNameAsc(UUID tenantId);

  List<FarmerProfileEntity> findByTenantIdAndStatusOrderByDisplayNameAsc(
      UUID tenantId,
      FarmerProfileStatus status
  );

  Optional<FarmerProfileEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<FarmerProfileEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

  Optional<FarmerProfileEntity> findByTenantIdAndAadhaarNumber(UUID tenantId, String aadhaarNumber);

  List<FarmerProfileEntity> findByTenantIdAndMobileNumberOrderByDisplayNameAsc(
      UUID tenantId,
      String mobileNumber
  );

  boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);
}
