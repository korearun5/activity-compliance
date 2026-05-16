package com.activityplatform.backend.carbon.repository;

import com.activityplatform.backend.carbon.domain.CarbonActivityCategoryEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonActivityCategoryRepository
    extends JpaRepository<CarbonActivityCategoryEntity, UUID> {
  List<CarbonActivityCategoryEntity> findByStatusOrderBySortOrderAsc(CarbonRecordStatus status);
}
