package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.fpo.domain.FpoMemberProfileEntity;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import org.springframework.http.HttpStatus;

final class FpoAccessPolicy {
  private FpoAccessPolicy() {
  }

  static boolean isFpoManager(CurrentUser currentUser) {
    return currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER);
  }

  static boolean isPhaseOneStaff(CurrentUser currentUser) {
    return currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR);
  }

  static boolean canAccessMember(CurrentUser currentUser, FpoMemberProfileEntity member) {
    if (isFpoManager(currentUser)) {
      return true;
    }

    if (currentUser.hasAnyRole(Role.FIELD_COORDINATOR)) {
      return isAssignedCoordinator(currentUser, member);
    }

    return currentUser.hasAnyRole(Role.FARMER) && isLinkedFarmer(currentUser, member);
  }

  static boolean canMutateMemberData(CurrentUser currentUser, FpoMemberProfileEntity member) {
    if (isFpoManager(currentUser)) {
      return true;
    }

    return currentUser.hasAnyRole(Role.FIELD_COORDINATOR)
        && isAssignedCoordinator(currentUser, member);
  }

  static void requirePhaseOneStaff(CurrentUser currentUser, String message) {
    if (!isPhaseOneStaff(currentUser)) {
      throw accessDenied(message);
    }
  }

  static void requireFpoManager(CurrentUser currentUser, String message) {
    if (!isFpoManager(currentUser)) {
      throw accessDenied(message);
    }
  }

  static void requireMemberAccess(
      CurrentUser currentUser,
      FpoMemberProfileEntity member,
      String message
  ) {
    if (!canAccessMember(currentUser, member)) {
      throw accessDenied(message);
    }
  }

  static void requireMemberMutationAccess(
      CurrentUser currentUser,
      FpoMemberProfileEntity member,
      String message
  ) {
    if (!canMutateMemberData(currentUser, member)) {
      throw accessDenied(message);
    }
  }

  private static boolean isAssignedCoordinator(
      CurrentUser currentUser,
      FpoMemberProfileEntity member
  ) {
    return member.getCoordinatorUser() != null
        && member.getCoordinatorUser().getId().equals(currentUser.userId());
  }

  private static boolean isLinkedFarmer(CurrentUser currentUser, FpoMemberProfileEntity member) {
    return member.getUser().getId().equals(currentUser.userId());
  }

  private static ApplicationException accessDenied(String message) {
    return new ApplicationException(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
  }
}
