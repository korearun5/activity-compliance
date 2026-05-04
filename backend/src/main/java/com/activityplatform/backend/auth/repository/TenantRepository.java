package com.activityplatform.backend.auth.repository;

import com.activityplatform.backend.auth.domain.TenantEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {
  Optional<TenantEntity> findByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCase(String code);
}

