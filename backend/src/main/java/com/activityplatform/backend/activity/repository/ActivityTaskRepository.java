package com.activityplatform.backend.activity.repository;

import com.activityplatform.backend.activity.domain.ActivityTaskEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityTaskRepository extends JpaRepository<ActivityTaskEntity, UUID> {
  List<ActivityTaskEntity> findByActivityIdOrderByDueOn(UUID activityId);

  Optional<ActivityTaskEntity> findByIdAndActivityId(UUID id, UUID activityId);
}
