import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import { FpoMemberResponse } from "../core/api/fpoContracts";
import { BackendUserResponse } from "../core/api/userContracts";
import { logger } from "../core/logging/logger";
import { UserProfileField, UserProfileInput } from "../core/model/types";
import { readJson, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { profileTemplate } from "./agricultureConfig";

export type ProfileField = UserProfileField;
export type ProfileInput = UserProfileInput;

export async function saveUserProfile(username: string, profile: UserProfileInput) {
  const fields = mergeProfileFields(profile);
  await writeJson(storageKeys.profile.byUsername(username), fields);
  return fields;
}

export async function getUserProfile(username: string | null) {
  const accessToken = await AsyncStorage.getItem(storageKeys.auth.accessToken);

  if (accessToken) {
    try {
      const member = await apiClient.get<FpoMemberResponse>(endpoints.fpo.members.me, {
        accessToken
      });
      const fields = mergeMemberProfileFields(member);

      await writeJson(storageKeys.profile.byUsername(member.username), fields);
      return fields;
    } catch (error) {
      if (!(error instanceof ApiClientError) || error.status !== 404) {
        throw error;
      }
    }

    try {
      const user = await apiClient.get<BackendUserResponse>(endpoints.users.me, {
        accessToken
      });
      const fields = mergeProfileFields({
        displayName: user.displayName,
        locationName: user.locationName ?? "Not set",
        phone: user.phone ?? "Not set",
        siteName: user.siteName ?? "Not set",
        status: user.status === "ACTIVE" ? "Active" : "Inactive"
      });

      await writeJson(storageKeys.profile.byUsername(user.username), fields);
      return fields;
    } catch (error) {
      if (error instanceof ApiClientError) {
        throw error;
      }

      logger.warn("Backend profile unavailable; using local profile fallback.", {
        message: error instanceof Error ? error.message : "Unknown profile error"
      });
    }
  }

  if (!username) {
    return profileTemplate;
  }

  return readJson<UserProfileField[]>(
    [
      storageKeys.profile.byUsername(username),
      storageKeys.legacy.profile.byUsername(username)
    ],
    profileTemplate
  );
}

function mergeProfileFields(profile: UserProfileInput): UserProfileField[] {
  const overrides = new Map([
    ["Name", profile.displayName],
    ["Age", profile.age || "Not set"],
    ["Sex", profile.sex || "Not set"],
    ["Phone", profile.phone],
    ["Region", profile.locationName],
    ["Village", profile.siteName],
    ["Status", profile.status ?? "Active"]
  ]);

  return profileTemplate.map((field) => ({
    ...field,
    value: overrides.get(field.label) ?? field.value
  }));
}

function mergeMemberProfileFields(member: FpoMemberResponse): UserProfileField[] {
  const fields = mergeProfileFields({
    age: member.age?.toString(),
    displayName: member.displayName,
    locationName: member.village,
    phone: member.mobileNumber,
    sex: displayValue(member.gender),
    siteName: member.taluka,
    status: member.status === "ACTIVE"
      ? "Active"
      : member.status === "SUSPENDED"
        ? "Suspended"
        : "Inactive"
  });
  const overrides = new Map([
    ["Farmer group", displayValue(member.farmerCategory)],
    ["Status", member.status]
  ]);

  return [
    ...fields.map((field) => ({
      ...field,
      value: overrides.get(field.label) ?? field.value
    })),
    { label: "Member number", value: member.memberNumber },
    { label: "Taluka", value: member.taluka },
    { label: "District", value: displayValue(member.districtName) },
    { label: "State", value: member.stateName },
    { label: "Login username", value: member.username }
  ];
}

function displayValue(value: string | null | undefined) {
  return value && value.trim() ? value.trim() : "Not set";
}
