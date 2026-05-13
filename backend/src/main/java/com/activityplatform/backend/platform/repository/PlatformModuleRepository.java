package com.activityplatform.backend.platform.repository;

import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.domain.PlatformModuleEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformModuleRepository extends JpaRepository<PlatformModuleEntity, UUID> {
  Optional<PlatformModuleEntity> findByCode(ModuleCode code);

  List<PlatformModuleEntity> findAllByOrderByCodeAsc();
}
