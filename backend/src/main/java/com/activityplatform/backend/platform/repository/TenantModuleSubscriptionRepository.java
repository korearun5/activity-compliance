package com.activityplatform.backend.platform.repository;

import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantModuleSubscriptionRepository
    extends JpaRepository<TenantModuleSubscriptionEntity, UUID> {
  List<TenantModuleSubscriptionEntity> findByTenantId(UUID tenantId);

  @Query("""
      select subscription
      from TenantModuleSubscriptionEntity subscription
      join fetch subscription.module module
      where subscription.tenant.id = :tenantId
      order by module.code
      """)
  List<TenantModuleSubscriptionEntity> findByTenantIdOrderByModuleCode(
      @Param("tenantId") UUID tenantId
  );

  @Query("""
      select subscription
      from TenantModuleSubscriptionEntity subscription
      join fetch subscription.module module
      where subscription.tenant.id = :tenantId
        and module.code = :code
      """)
  Optional<TenantModuleSubscriptionEntity> findByTenantIdAndModuleCode(
      @Param("tenantId") UUID tenantId,
      @Param("code") ModuleCode code
  );
}
