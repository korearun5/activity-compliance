import { UserProfileField, UserProfileInput } from "../core/model/types";
import { readJson, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { farmerProfile } from "./farmDemoData";

export type FarmerProfileField = UserProfileField;
export type FarmerProfileInput = UserProfileInput;

export async function saveUserProfile(
  username: string,
  profile: UserProfileInput
) {
  const fields = mergeProfileFields(profile);
  await writeJson(storageKeys.profile.byUsername(username), fields);
  return fields;
}

export async function getUserProfile(username: string | null) {
  if (!username) {
    return farmerProfile;
  }

  return readJson<UserProfileField[]>(
    [
      storageKeys.profile.byUsername(username),
      storageKeys.legacy.profile.byUsername(username)
    ],
    farmerProfile
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
    ["Status", "Active"]
  ]);

  return farmerProfile.map((field) => ({
    ...field,
    value: overrides.get(field.label) ?? field.value
  }));
}

export const getFarmerProfile = getUserProfile;
export const saveFarmerProfile = saveUserProfile;
