package com.activityplatform.backend.fpo.repository;

import com.activityplatform.backend.fpo.domain.FpoSoilProfileEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FpoSoilProfileRepository extends JpaRepository<FpoSoilProfileEntity, UUID> {
  List<FpoSoilProfileEntity> findByTenantIdAndMemberProfileIdOrderByCreatedAtDesc(
      UUID tenantId,
      UUID memberProfileId
  );

  Optional<FpoSoilProfileEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
