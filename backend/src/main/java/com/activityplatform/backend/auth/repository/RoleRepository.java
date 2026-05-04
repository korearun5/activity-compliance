package com.activityplatform.backend.auth.repository;

import com.activityplatform.backend.auth.domain.RoleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
  Optional<RoleEntity> findByTenantCodeIgnoreCaseAndCodeIgnoreCase(String tenantCode, String code);
}

