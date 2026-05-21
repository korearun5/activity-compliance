package com.activityplatform.backend.carbon.repository;

import com.activityplatform.backend.carbon.domain.CarbonSoilProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonVerificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonSoilProfileRepository extends JpaRepository<CarbonSoilProfileEntity, UUID> {
  List<CarbonSoilProfileEntity> findByTenantIdAndCarbonProfileIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID carbonProfileId
  );

  Optional<CarbonSoilProfileEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  List<CarbonSoilProfileEntity> findByTenantIdAndVerificationStatusOrderByUpdatedAtDesc(
      UUID tenantId,
      CarbonVerificationStatus verificationStatus
  );
}
