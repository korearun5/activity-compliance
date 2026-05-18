package com.activityplatform.backend.farmer.service;

import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmerService {
  FarmerProfileEntity createFarmerProfile(
      CurrentUser currentUser,
      UserEntity farmerUser,
      FarmerProfileCommand command
  );

  FarmerProfileEntity ensureFarmerProfileForUser(
      CurrentUser currentUser,
      UserEntity farmerUser,
      FarmerProfileCommand command
  );

  FarmerProfileEntity updateFarmerProfile(
      CurrentUser currentUser,
      UUID farmerProfileId,
      FarmerProfileCommand command
  );

  Optional<FarmerProfileEntity> findByUserId(UUID tenantId, UUID userId);

  FarmerProfileEntity requireById(UUID tenantId, UUID farmerProfileId);

  FarmerProfileEntity requireByUserId(UUID tenantId, UUID userId);

  List<FarmerParticipant> findParticipants(UUID tenantId);
}
