package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import org.springframework.http.HttpStatus;

final class CarbonAccessPolicy {
  private CarbonAccessPolicy() {
  }

  static boolean isCarbonStaff(CurrentUser currentUser) {
    return currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR);
  }

  static boolean canViewProfile(CurrentUser currentUser, CarbonProfileEntity profile) {
    if (currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      return true;
    }

    if (currentUser.hasAnyRole(Role.FIELD_COORDINATOR)) {
      return isAssignedCoordinator(currentUser, profile);
    }

    return currentUser.hasAnyRole(Role.FARMER) && isLinkedFarmer(currentUser, profile);
  }

  static boolean canMutateProfile(CurrentUser currentUser, CarbonProfileEntity profile) {
    if (currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      return true;
    }

    return currentUser.hasAnyRole(Role.FIELD_COORDINATOR)
        && isAssignedCoordinator(currentUser, profile);
  }

  static void requireCarbonStaff(CurrentUser currentUser, String message) {
    if (!isCarbonStaff(currentUser)) {
      throw accessDenied(message);
    }
  }

  static void requireProfileAccess(
      CurrentUser currentUser,
      CarbonProfileEntity profile,
      String message
  ) {
    if (!canViewProfile(currentUser, profile)) {
      throw accessDenied(message);
    }
  }

  static void requireProfileMutationAccess(
      CurrentUser currentUser,
      CarbonProfileEntity profile,
      String message
  ) {
    if (!canMutateProfile(currentUser, profile)) {
      throw accessDenied(message);
    }
  }

  private static boolean isAssignedCoordinator(
      CurrentUser currentUser,
      CarbonProfileEntity profile
  ) {
    return profile.getCoordinatorUser() != null
        && profile.getCoordinatorUser().getId().equals(currentUser.userId());
  }

  private static boolean isLinkedFarmer(CurrentUser currentUser, CarbonProfileEntity profile) {
    if (profile.getUser() != null && profile.getUser().getId().equals(currentUser.userId())) {
      return true;
    }

    return profile.getFpoMemberProfile() != null
        && profile.getFpoMemberProfile().getUser().getId().equals(currentUser.userId());
  }

  private static ApplicationException accessDenied(String message) {
    return new ApplicationException(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
  }
}
