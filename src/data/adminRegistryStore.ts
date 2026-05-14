import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { PageResponse } from "../core/api/contracts";
import { endpoints } from "../core/api/endpoints";
import {
  BackendUserStatus,
  BackendUserResponse,
  CreateBackendUserRequest,
  UpdateBackendUserRequest,
  UpdateBackendUserStatusRequest
} from "../core/api/userContracts";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import {
  Account,
  ManagedUser,
  UserProfileInput,
  UserStatus
} from "../core/model/types";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { saveUserProfile } from "./profileStore";
import { CropCycle, ProofSubmission } from "./agricultureConfig";

export type RegisteredFieldCoordinator = ManagedUser & {
  name: string;
  region: string;
  village: string;
};

export type CreateRegisteredFieldCoordinatorInput = {
  password: string;
  profile: UserProfileInput;
  username: string;
};

export async function registerUser(username: string, profile: UserProfileInput) {
  const nextFieldCoordinator = toRegisteredFieldCoordinatorFromProfile(username, profile);

  return upsertLocalFieldCoordinator(nextFieldCoordinator);
}

export async function getRegisteredFieldCoordinators(): Promise<RegisteredFieldCoordinator[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const page = await apiClient.getPaginated<PageResponse<BackendUserResponse>>(
        endpoints.users.list,
        { size: 100, sort: "displayName,asc" },
        { accessToken }
      );
      const fieldCoordinators = page.content
        .filter((user) => user.roles.includes("FIELD_COORDINATOR"))
        .map(toRegisteredFieldCoordinatorFromBackend);

      await writeJson(storageKeys.registry.users, fieldCoordinators);
      return fieldCoordinators;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdminUserError(error);
      }

      logger.warn("Backend user list unavailable; using local registry.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalRegisteredFieldCoordinators();
}

export async function createRegisteredFieldCoordinator({
  password,
  profile,
  username
}: CreateRegisteredFieldCoordinatorInput): Promise<RegisteredFieldCoordinator> {
  const trimmedUsername = username.trim();
  const normalizedProfile = normalizeProfile(profile);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const user = await apiClient.post<CreateBackendUserRequest, BackendUserResponse>(
        endpoints.users.create,
        {
          displayName: normalizedProfile.displayName,
          locationName: normalizedProfile.locationName,
          password,
          phone: normalizedProfile.phone,
          role: "FIELD_COORDINATOR",
          siteName: normalizedProfile.siteName,
          username: trimmedUsername
        },
        { accessToken }
      );
      const fieldCoordinator = toRegisteredFieldCoordinatorFromBackend(user);

      await Promise.all([
        saveUserProfile(fieldCoordinator.username, profileFromFieldCoordinator(fieldCoordinator)),
        upsertLocalFieldCoordinator(fieldCoordinator)
      ]);

      return fieldCoordinator;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdminUserError(error);
      }

      logger.warn("Backend user creation unavailable; using local registry.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return createLocalFieldCoordinator({
    password,
    profile: normalizedProfile,
    username: trimmedUsername
  });
}

export async function updateRegisteredFieldCoordinator({
  profile,
  userId,
  username
}: {
  profile: UserProfileInput;
  userId?: string;
  username: string;
}): Promise<RegisteredFieldCoordinator> {
  const normalizedProfile = normalizeProfile(profile);
  const accessToken = await getAccessToken();

  if (accessToken && userId) {
    try {
      const user = await apiClient.put<UpdateBackendUserRequest, BackendUserResponse>(
        endpoints.users.byId(userId),
        {
          displayName: normalizedProfile.displayName,
          locationName: normalizedProfile.locationName,
          phone: normalizedProfile.phone,
          siteName: normalizedProfile.siteName
        },
        { accessToken }
      );
      const fieldCoordinator = toRegisteredFieldCoordinatorFromBackend(user);

      await Promise.all([
        saveUserProfile(fieldCoordinator.username, profileFromFieldCoordinator(fieldCoordinator)),
        upsertLocalFieldCoordinator(fieldCoordinator)
      ]);

      return fieldCoordinator;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdminUserError(error);
      }

      logger.warn("Backend user update unavailable; using local registry.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  await saveUserProfile(username, normalizedProfile);
  return registerUser(username, normalizedProfile);
}

export async function updateRegisteredFieldCoordinatorStatus(
  fieldCoordinator: RegisteredFieldCoordinator,
  status: Extract<UserStatus, "Active" | "Inactive">
): Promise<RegisteredFieldCoordinator> {
  const accessToken = await getAccessToken();

  if (accessToken && fieldCoordinator.id) {
    try {
      const user = await apiClient.patch<
        UpdateBackendUserStatusRequest,
        BackendUserResponse
      >(
        endpoints.users.status(fieldCoordinator.id),
        { status: toBackendStatus(status) },
        { accessToken }
      );
      const updatedFieldCoordinator = toRegisteredFieldCoordinatorFromBackend(user);

      await upsertLocalFieldCoordinator(updatedFieldCoordinator);
      return updatedFieldCoordinator;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdminUserError(error);
      }

      logger.warn("Backend user status update unavailable; using local registry.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return updateLocalFieldCoordinatorStatus(fieldCoordinator.username, status);
}

async function getLocalRegisteredFieldCoordinators(): Promise<RegisteredFieldCoordinator[]> {
  const savedFieldCoordinators = await readJsonArray<Partial<RegisteredFieldCoordinator>>([
    storageKeys.registry.users,
    storageKeys.legacy.registry.users
  ]);

  return savedFieldCoordinators
    .map(toRegisteredFieldCoordinator)
    .filter((fieldCoordinator): fieldCoordinator is RegisteredFieldCoordinator =>
      Boolean(fieldCoordinator)
    );
}

async function createLocalFieldCoordinator({
  password,
  profile,
  username
}: CreateRegisteredFieldCoordinatorInput): Promise<RegisteredFieldCoordinator> {
  await ensureLocalUsernameAvailable(username);
  const accounts = await readJsonArray<Partial<Account>>([
    storageKeys.auth.localAccounts,
    storageKeys.legacy.auth.localAccounts
  ]);
  const nextAccounts = [
    ...accounts.filter(
      (account) =>
        typeof account.username !== "string" ||
        account.username.toLowerCase() !== username.toLowerCase()
    ),
    { password, role: "fieldCoordinator" as const, username }
  ];

  await writeJson(storageKeys.auth.localAccounts, nextAccounts);
  await saveUserProfile(username, profile);
  return registerUser(username, profile);
}

async function ensureLocalUsernameAvailable(username: string) {
  const [accounts, fieldCoordinators] = await Promise.all([
    readJsonArray<Partial<Account>>([
      storageKeys.auth.localAccounts,
      storageKeys.legacy.auth.localAccounts
    ]),
    getLocalRegisteredFieldCoordinators()
  ]);
  const usernameExists = [...accounts, ...fieldCoordinators].some(
    (item) =>
      typeof item.username === "string" &&
      item.username.toLowerCase() === username.toLowerCase()
  );

  if (usernameExists) {
    throw new AppError("AUTH_USERNAME_TAKEN", "This username is already taken.");
  }
}

async function upsertLocalFieldCoordinator(fieldCoordinator: RegisteredFieldCoordinator) {
  const currentFieldCoordinators = await getLocalRegisteredFieldCoordinators();
  const nextFieldCoordinators = [
    fieldCoordinator,
    ...currentFieldCoordinators.filter(
      (item) => item.username.toLowerCase() !== fieldCoordinator.username.toLowerCase()
    )
  ];

  await writeJson(storageKeys.registry.users, nextFieldCoordinators);
  return fieldCoordinator;
}

async function updateLocalFieldCoordinatorStatus(
  username: string,
  status: Extract<UserStatus, "Active" | "Inactive">
) {
  const currentFieldCoordinators = await getLocalRegisteredFieldCoordinators();
  const fieldCoordinator = currentFieldCoordinators.find(
    (item) => item.username.toLowerCase() === username.toLowerCase()
  );

  if (!fieldCoordinator) {
    throw new AppError("VALIDATION_FAILED", "Field coordinator profile not found.");
  }

  return upsertLocalFieldCoordinator({ ...fieldCoordinator, status });
}

export function countFieldCoordinatorActivities(
  fieldCoordinator: RegisteredFieldCoordinator,
  cycles: CropCycle[],
  status: "running" | "completed"
) {
  return cycles.filter(
    (cycle) =>
      cycle.status === status &&
      ((cycle.farmerUsername &&
        cycle.farmerUsername.toLowerCase() === fieldCoordinator.username.toLowerCase()) ||
        cycle.participantUsername?.toLowerCase() ===
          fieldCoordinator.username.toLowerCase() ||
        cycle.region === fieldCoordinator.region)
  ).length;
}

export function countFieldCoordinatorProofs(
  fieldCoordinator: RegisteredFieldCoordinator,
  proofs: ProofSubmission[]
) {
  return proofs.filter(
    (proof) =>
      proof.participantName === fieldCoordinator.name ||
      proof.farmer === fieldCoordinator.name ||
      proof.participantUsername?.toLowerCase() === fieldCoordinator.username.toLowerCase() ||
      proof.farmerUsername?.toLowerCase() === fieldCoordinator.username.toLowerCase() ||
      (proof.region === fieldCoordinator.region && proof.status === "done")
  ).length;
}

function toRegisteredFieldCoordinatorFromBackend(
  user: BackendUserResponse
): RegisteredFieldCoordinator {
  const displayName = user.displayName;
  const locationName = user.locationName ?? "Not set";
  const siteName = user.siteName ?? "Not set";

  return {
    displayName,
    id: user.id,
    locationName,
    name: displayName,
    phone: user.phone ?? "Not set",
    region: locationName,
    siteName,
    status: toFrontendStatus(user.status),
    tenantId: user.tenantId,
    username: user.username,
    village: siteName
  };
}

function toRegisteredFieldCoordinatorFromProfile(
  username: string,
  profile: UserProfileInput
): RegisteredFieldCoordinator {
  const normalizedProfile = normalizeProfile(profile);

  return {
    displayName: normalizedProfile.displayName,
    locationName: normalizedProfile.locationName,
    name: normalizedProfile.displayName,
    phone: normalizedProfile.phone,
    region: normalizedProfile.locationName,
    siteName: normalizedProfile.siteName,
    status: "Active",
    username,
    village: normalizedProfile.siteName
  };
}

function toRegisteredFieldCoordinator(
  fieldCoordinator: Partial<RegisteredFieldCoordinator>
): RegisteredFieldCoordinator | null {
  const username = fieldCoordinator.username;
  const displayName = fieldCoordinator.displayName ?? fieldCoordinator.name;
  const locationName = fieldCoordinator.locationName ?? fieldCoordinator.region;
  const siteName = fieldCoordinator.siteName ?? fieldCoordinator.village;

  if (
    typeof username !== "string" ||
    typeof displayName !== "string" ||
    typeof locationName !== "string" ||
    typeof siteName !== "string" ||
    typeof fieldCoordinator.phone !== "string"
  ) {
    return null;
  }

  return {
    displayName,
    id: fieldCoordinator.id,
    locationName,
    name: displayName,
    phone: fieldCoordinator.phone,
    region: locationName,
    siteName,
    status: fieldCoordinator.status ?? "Profile pending",
    username,
    village: siteName
  };
}

function normalizeProfile(profile: UserProfileInput): UserProfileInput {
  return {
    ...profile,
    displayName: profile.displayName.trim(),
    locationName: profile.locationName.trim(),
    phone: profile.phone.trim(),
    siteName: profile.siteName.trim()
  };
}

function profileFromFieldCoordinator(fieldCoordinator: RegisteredFieldCoordinator): UserProfileInput {
  return {
    displayName: fieldCoordinator.displayName,
    locationName: fieldCoordinator.locationName,
    phone: fieldCoordinator.phone,
    siteName: fieldCoordinator.siteName
  };
}

function toFrontendStatus(status: BackendUserStatus): UserStatus {
  return status === "ACTIVE" ? "Active" : "Inactive";
}

function toBackendStatus(status: Extract<UserStatus, "Active" | "Inactive">) {
  return status === "Active" ? "ACTIVE" : "INACTIVE";
}

async function getAccessToken() {
  return AsyncStorage.getItem(storageKeys.auth.accessToken);
}

function canUseLocalFallback(error: unknown) {
  return !(error instanceof ApiClientError);
}

function toAdminUserError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 409) {
      return new AppError(
        "AUTH_USERNAME_TAKEN",
        error.message || "This username is already taken."
      );
    }

    if (error.status === 400) {
      return new AppError(
        "VALIDATION_FAILED",
        error.message || "Review the field coordinator profile fields."
      );
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError(
        "ACCESS_DENIED",
        error.message || "You do not have permission to manage field coordinators."
      );
    }

    return new AppError(
      "API_REQUEST_FAILED",
      error.message || "Unable to manage field coordinator profile."
    );
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage field coordinator profile.");
}
