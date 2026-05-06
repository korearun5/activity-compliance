package com.activityplatform.backend.auth.repository;

import com.activityplatform.backend.auth.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Page<UserEntity> findByTenantId(UUID tenantId, Pageable pageable);

  Optional<UserEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<UserEntity> findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(
      String tenantCode,
      String username
  );

  boolean existsByTenantIdAndUsernameIgnoreCase(UUID tenantId, String username);

  boolean existsByTenantCodeIgnoreCaseAndUsernameIgnoreCase(String tenantCode, String username);

  @Query("""
      select count(distinct u.id)
      from UserEntity u
      join u.roles role
      where u.tenant.id = :tenantId
        and role.code = :roleCode
      """)
  long countByTenantIdAndRoleCode(
      @Param("tenantId") UUID tenantId,
      @Param("roleCode") String roleCode
  );
}
