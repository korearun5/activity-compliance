package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FpoMemberProfileRepository
    extends JpaRepository<FpoMemberProfileEntity, UUID> {
  List<FpoMemberProfileEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  Page<FpoMemberProfileEntity> findByTenantId(UUID tenantId, Pageable pageable);

  Page<FpoMemberProfileEntity> findByTenantIdAndStatus(
      UUID tenantId,
      FpoMemberStatus status,
      Pageable pageable
  );

  Optional<FpoMemberProfileEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<FpoMemberProfileEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

  boolean existsByTenantIdAndMemberNumberIgnoreCase(UUID tenantId, String memberNumber);

  boolean existsByTenantIdAndMobileNumber(UUID tenantId, String mobileNumber);

  boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);

  Optional<FpoMemberProfileEntity> findByTenantIdAndMemberNumberIgnoreCase(
      UUID tenantId,
      String memberNumber
  );

  Optional<FpoMemberProfileEntity> findByTenantIdAndMobileNumber(
      UUID tenantId,
      String mobileNumber
  );
}
