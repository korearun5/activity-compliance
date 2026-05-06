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

export type RegisteredParticipant = ManagedUser & {
  name: string;
  region: string;
  village: string;
};

export type CreateRegisteredParticipantInput = {
  password: string;
  profile: UserProfileInput;
  username: string;
};

export async function registerUser(username: string, profile: UserProfileInput) {
  const nextParticipant = toRegisteredParticipantFromProfile(username, profile);

  return upsertLocalParticipant(nextParticipant);
}

export async function getRegisteredParticipants(): Promise<RegisteredParticipant[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const page = await apiClient.getPaginated<PageResponse<BackendUserResponse>>(
        endpoints.users.list,
        { size: 100, sort: "displayName,asc" },
        { accessToken }
      );
      const participants = page.content
        .filter((user) => user.roles.includes("PARTICIPANT"))
        .map(toRegisteredParticipantFromBackend);

      await writeJson(storageKeys.registry.users, participants);
      return participants;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdminUserError(error);
      }

      logger.warn("Backend user list unavailable; using local registry.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalRegisteredParticipants();
}

export async function createRegisteredParticipant({
  password,
  profile,
  username
}: CreateRegisteredParticipantInput): Promise<RegisteredParticipant> {
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
          siteName: normalizedProfile.siteName,
          username: trimmedUsername
        },
        { accessToken }
      );
      const participant = toRegisteredParticipantFromBackend(user);

      await Promise.all([
        saveUserProfile(participant.username, profileFromParticipant(participant)),
        upsertLocalParticipant(participant)
      ]);

      return participant;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdminUserError(error);
      }

      logger.warn("Backend user creation unavailable; using local registry.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return createLocalParticipant({
    password,
    profile: normalizedProfile,
    username: trimmedUsername
  });
}

export async function updateRegisteredParticipant({
  profile,
  userId,
  username
}: {
  profile: UserProfileInput;
  userId?: string;
  username: string;
}): Promise<RegisteredParticipant> {
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
      const participant = toRegisteredParticipantFromBackend(user);

      await Promise.all([
        saveUserProfile(participant.username, profileFromParticipant(participant)),
        upsertLocalParticipant(participant)
      ]);

      return participant;
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

export async function updateRegisteredParticipantStatus(
  participant: RegisteredParticipant,
  status: Extract<UserStatus, "Active" | "Inactive">
): Promise<RegisteredParticipant> {
  const accessToken = await getAccessToken();

  if (accessToken && participant.id) {
    try {
      const user = await apiClient.patch<
        UpdateBackendUserStatusRequest,
        BackendUserResponse
      >(
        endpoints.users.status(participant.id),
        { status: toBackendStatus(status) },
        { accessToken }
      );
      const updatedParticipant = toRegisteredParticipantFromBackend(user);

      await upsertLocalParticipant(updatedParticipant);
      return updatedParticipant;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toAdminUserError(error);
      }

      logger.warn("Backend user status update unavailable; using local registry.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return updateLocalParticipantStatus(participant.username, status);
}

async function getLocalRegisteredParticipants(): Promise<RegisteredParticipant[]> {
  const savedParticipants = await readJsonArray<Partial<RegisteredParticipant>>([
    storageKeys.registry.users,
    storageKeys.legacy.registry.users
  ]);

  return savedParticipants
    .map(toRegisteredParticipant)
    .filter((participant): participant is RegisteredParticipant =>
      Boolean(participant)
    );
}

async function createLocalParticipant({
  password,
  profile,
  username
}: CreateRegisteredParticipantInput): Promise<RegisteredParticipant> {
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
    { password, role: "participant" as const, username }
  ];

  await writeJson(storageKeys.auth.localAccounts, nextAccounts);
  await saveUserProfile(username, profile);
  return registerUser(username, profile);
}

async function ensureLocalUsernameAvailable(username: string) {
  const [accounts, participants] = await Promise.all([
    readJsonArray<Partial<Account>>([
      storageKeys.auth.localAccounts,
      storageKeys.legacy.auth.localAccounts
    ]),
    getLocalRegisteredParticipants()
  ]);
  const usernameExists = [...accounts, ...participants].some(
    (item) =>
      typeof item.username === "string" &&
      item.username.toLowerCase() === username.toLowerCase()
  );

  if (usernameExists) {
    throw new AppError("AUTH_USERNAME_TAKEN", "This username is already taken.");
  }
}

async function upsertLocalParticipant(participant: RegisteredParticipant) {
  const currentParticipants = await getLocalRegisteredParticipants();
  const nextParticipants = [
    participant,
    ...currentParticipants.filter(
      (item) => item.username.toLowerCase() !== participant.username.toLowerCase()
    )
  ];

  await writeJson(storageKeys.registry.users, nextParticipants);
  return participant;
}

async function updateLocalParticipantStatus(
  username: string,
  status: Extract<UserStatus, "Active" | "Inactive">
) {
  const currentParticipants = await getLocalRegisteredParticipants();
  const participant = currentParticipants.find(
    (item) => item.username.toLowerCase() === username.toLowerCase()
  );

  if (!participant) {
    throw new AppError("VALIDATION_FAILED", "Participant profile not found.");
  }

  return upsertLocalParticipant({ ...participant, status });
}

export function countParticipantActivities(
  participant: RegisteredParticipant,
  cycles: CropCycle[],
  status: "running" | "completed"
) {
  return cycles.filter(
    (cycle) =>
      cycle.status === status &&
      ((cycle.farmerUsername &&
        cycle.farmerUsername.toLowerCase() === participant.username.toLowerCase()) ||
        cycle.participantUsername?.toLowerCase() ===
          participant.username.toLowerCase() ||
        cycle.region === participant.region)
  ).length;
}

export function countParticipantProofs(
  participant: RegisteredParticipant,
  proofs: ProofSubmission[]
) {
  return proofs.filter(
    (proof) =>
      proof.participantName === participant.name ||
      proof.farmer === participant.name ||
      proof.participantUsername?.toLowerCase() === participant.username.toLowerCase() ||
      proof.farmerUsername?.toLowerCase() === participant.username.toLowerCase() ||
      (proof.region === participant.region && proof.status === "done")
  ).length;
}

function toRegisteredParticipantFromBackend(
  user: BackendUserResponse
): RegisteredParticipant {
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

function toRegisteredParticipantFromProfile(
  username: string,
  profile: UserProfileInput
): RegisteredParticipant {
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

function toRegisteredParticipant(
  participant: Partial<RegisteredParticipant>
): RegisteredParticipant | null {
  const username = participant.username;
  const displayName = participant.displayName ?? participant.name;
  const locationName = participant.locationName ?? participant.region;
  const siteName = participant.siteName ?? participant.village;

  if (
    typeof username !== "string" ||
    typeof displayName !== "string" ||
    typeof locationName !== "string" ||
    typeof siteName !== "string" ||
    typeof participant.phone !== "string"
  ) {
    return null;
  }

  return {
    displayName,
    id: participant.id,
    locationName,
    name: displayName,
    phone: participant.phone,
    region: locationName,
    siteName,
    status: participant.status ?? "Profile pending",
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

function profileFromParticipant(participant: RegisteredParticipant): UserProfileInput {
  return {
    displayName: participant.displayName,
    locationName: participant.locationName,
    phone: participant.phone,
    siteName: participant.siteName
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
        error.message || "Review the participant profile fields."
      );
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError(
        "ACCESS_DENIED",
        error.message || "You do not have permission to manage participants."
      );
    }

    return new AppError(
      "API_REQUEST_FAILED",
      error.message || "Unable to manage participant profile."
    );
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage participant profile.");
}
