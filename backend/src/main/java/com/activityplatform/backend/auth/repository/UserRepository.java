package com.activityplatform.backend.auth.repository;

import com.activityplatform.backend.auth.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(
      String tenantCode,
      String username
  );

  boolean existsByTenantCodeIgnoreCaseAndUsernameIgnoreCase(String tenantCode, String username);
}

