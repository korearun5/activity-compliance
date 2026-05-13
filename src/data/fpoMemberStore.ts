import AsyncStorage from "@react-native-async-storage/async-storage";

import { apiClient, ApiClientError } from "../core/api/client";
import { PageResponse } from "../core/api/contracts";
import { endpoints } from "../core/api/endpoints";
import {
  CreateFpoMemberRequest,
  FpoMemberResponse,
  FpoMemberStatus,
  UpdateFpoMemberRequest
} from "../core/api/fpoContracts";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import { ManagedUser, UserStatus } from "../core/model/types";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { CropCycle, ProofSubmission } from "./agricultureConfig";

export type FpoMember = ManagedUser & {
  aadhaarNumber?: string;
  age?: number;
  alternateMobileNumber?: string;
  districtName?: string;
  farmerCategory?: string;
  gender?: string;
  memberId: string;
  memberNumber: string;
  mobileNumber: string;
  name: string;
  region: string;
  stateName: string;
  taluka: string;
  userId: string;
  village: string;
};

export type CreateFpoMemberInput = {
  aadhaarNumber?: string;
  age?: string;
  alternateMobileNumber?: string;
  districtName?: string;
  displayName: string;
  farmerCategory?: string;
  gender?: string;
  memberNumber: string;
  mobileNumber: string;
  password: string;
  stateName: string;
  taluka: string;
  username: string;
  village: string;
};

export type UpdateFpoMemberInput = Omit<
  CreateFpoMemberInput,
  "password" | "username"
>;

export async function getFpoMembers(): Promise<FpoMember[]> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const page = await apiClient.getPaginated<PageResponse<FpoMemberResponse>>(
        endpoints.fpo.members.list,
        { size: 100, sort: "createdAt,desc" },
        { accessToken }
      );
      const members = page.content.map(toFpoMember);

      await writeJson(storageKeys.fpo.members, members);
      return members;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFpoMemberError(error);
      }

      logger.warn("Backend FPO member list unavailable; using cached members.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return getLocalFpoMembers();
}

export async function createFpoMember(input: CreateFpoMemberInput): Promise<FpoMember> {
  const normalizedInput = normalizeCreateInput(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const request = toCreateRequest(normalizedInput);
      const response = await apiClient.post<CreateFpoMemberRequest, FpoMemberResponse>(
        endpoints.fpo.members.create,
        request,
        { accessToken }
      );
      const member = toFpoMember(response);

      await upsertLocalFpoMember(member);
      return member;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFpoMemberError(error);
      }

      logger.warn("Backend FPO member creation unavailable; using cached members.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  await ensureLocalMemberAvailable(normalizedInput);
  return upsertLocalFpoMember({
    aadhaarNumber: normalizedInput.aadhaarNumber,
    age: normalizedInput.age ? Number(normalizedInput.age) : undefined,
    alternateMobileNumber: normalizedInput.alternateMobileNumber,
    displayName: normalizedInput.displayName,
    districtName: normalizedInput.districtName,
    farmerCategory: normalizedInput.farmerCategory,
    gender: normalizedInput.gender,
    id: normalizedInput.username,
    locationName: normalizedInput.village,
    memberId: `local-member-${Date.now()}`,
    memberNumber: normalizedInput.memberNumber,
    mobileNumber: normalizedInput.mobileNumber,
    name: normalizedInput.displayName,
    phone: normalizedInput.mobileNumber,
    region: normalizedInput.village,
    siteName: normalizedInput.taluka,
    status: "Active",
    stateName: normalizedInput.stateName,
    taluka: normalizedInput.taluka,
    userId: normalizedInput.username,
    username: normalizedInput.username,
    village: normalizedInput.village
  });
}

export async function updateFpoMemberStatus(
  member: FpoMember,
  status: Extract<UserStatus, "Active" | "Inactive" | "Suspended">
): Promise<FpoMember> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const response = await apiClient.patch<
        { status: FpoMemberStatus },
        FpoMemberResponse
      >(
        endpoints.fpo.members.status(member.memberId),
        { status: toBackendStatus(status) },
        { accessToken }
      );
      const updatedMember = toFpoMember(response);

      await upsertLocalFpoMember(updatedMember);
      return updatedMember;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFpoMemberError(error);
      }

      logger.warn("Backend FPO member status update unavailable; using cached members.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalFpoMember({ ...member, status });
}

export async function updateFpoMember(
  member: FpoMember,
  input: UpdateFpoMemberInput
): Promise<FpoMember> {
  const normalizedInput = normalizeUpdateInput(input);
  const accessToken = await getAccessToken();

  if (accessToken) {
    try {
      const request = toUpdateRequest(member, normalizedInput);
      const response = await apiClient.put<UpdateFpoMemberRequest, FpoMemberResponse>(
        endpoints.fpo.members.byId(member.memberId),
        request,
        { accessToken }
      );
      const updatedMember = toFpoMember(response);

      await upsertLocalFpoMember(updatedMember);
      return updatedMember;
    } catch (error) {
      if (!canUseLocalFallback(error)) {
        throw toFpoMemberError(error);
      }

      logger.warn("Backend FPO member update unavailable; using cached members.", {
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  }

  return upsertLocalFpoMember({
    ...member,
    aadhaarNumber: normalizedInput.aadhaarNumber,
    age: normalizedInput.age ? Number(normalizedInput.age) : undefined,
    alternateMobileNumber: normalizedInput.alternateMobileNumber,
    displayName: normalizedInput.displayName,
    districtName: normalizedInput.districtName,
    farmerCategory: normalizedInput.farmerCategory,
    gender: normalizedInput.gender,
    locationName: normalizedInput.village,
    memberNumber: normalizedInput.memberNumber,
    mobileNumber: normalizedInput.mobileNumber,
    name: normalizedInput.displayName,
    phone: normalizedInput.mobileNumber,
    region: normalizedInput.village,
    siteName: normalizedInput.taluka,
    stateName: normalizedInput.stateName,
    taluka: normalizedInput.taluka,
    village: normalizedInput.village
  });
}

export function countMemberActivities(
  member: FpoMember,
  cycles: CropCycle[],
  status: "completed" | "running"
) {
  return cycles.filter(
    (cycle) =>
      cycle.status === status &&
      ((cycle.farmerUsername &&
        cycle.farmerUsername.toLowerCase() === member.username.toLowerCase()) ||
        cycle.participantUsername?.toLowerCase() === member.username.toLowerCase() ||
        cycle.region === member.region)
  ).length;
}

export function countMemberProofs(member: FpoMember, proofs: ProofSubmission[]) {
  return proofs.filter(
    (proof) =>
      proof.participantName === member.name ||
      proof.farmer === member.name ||
      proof.participantUsername?.toLowerCase() === member.username.toLowerCase() ||
      proof.farmerUsername?.toLowerCase() === member.username.toLowerCase() ||
      (proof.region === member.region && proof.status === "done")
  ).length;
}

async function getLocalFpoMembers(): Promise<FpoMember[]> {
  const savedMembers = await readJsonArray<Partial<FpoMember>>([
    storageKeys.fpo.members
  ]);

  return savedMembers.map(toStoredFpoMember).filter((member): member is FpoMember =>
    Boolean(member)
  );
}

async function upsertLocalFpoMember(member: FpoMember) {
  const currentMembers = await getLocalFpoMembers();
  const nextMembers = [
    member,
    ...currentMembers.filter((item) => item.memberId !== member.memberId)
  ];

  await writeJson(storageKeys.fpo.members, nextMembers);
  return member;
}

async function ensureLocalMemberAvailable(input: CreateFpoMemberInput) {
  const members = await getLocalFpoMembers();
  const duplicate = members.some(
    (member) =>
      member.memberNumber.toLowerCase() === input.memberNumber.toLowerCase() ||
      member.mobileNumber === input.mobileNumber ||
      member.username.toLowerCase() === input.username.toLowerCase()
  );

  if (duplicate) {
    throw new AppError(
      "DUPLICATE_RESOURCE",
      "Member number, mobile number, or username already exists."
    );
  }
}

function toFpoMember(response: FpoMemberResponse): FpoMember {
  const siteName = [response.taluka, response.districtName]
    .filter(Boolean)
    .join(" / ");

  return {
    aadhaarNumber: response.aadhaarNumber ?? undefined,
    age: response.age ?? undefined,
    alternateMobileNumber: response.alternateMobileNumber ?? undefined,
    displayName: response.displayName,
    districtName: response.districtName ?? undefined,
    farmerCategory: response.farmerCategory ?? undefined,
    gender: response.gender ?? undefined,
    id: response.userId,
    locationName: response.village,
    memberId: response.id,
    memberNumber: response.memberNumber,
    mobileNumber: response.mobileNumber,
    name: response.displayName,
    phone: response.mobileNumber,
    region: response.village,
    siteName: siteName || "Not set",
    status: toFrontendStatus(response.status),
    stateName: response.stateName,
    taluka: response.taluka,
    tenantId: response.tenantId,
    userId: response.userId,
    username: response.username,
    village: response.village
  };
}

function toStoredFpoMember(member: Partial<FpoMember>): FpoMember | null {
  if (
    typeof member.memberId !== "string" ||
    typeof member.memberNumber !== "string" ||
    typeof member.displayName !== "string" ||
    typeof member.mobileNumber !== "string" ||
    typeof member.stateName !== "string" ||
    typeof member.taluka !== "string" ||
    typeof member.username !== "string" ||
    typeof member.userId !== "string" ||
    typeof member.village !== "string"
  ) {
    return null;
  }

  return {
    aadhaarNumber: member.aadhaarNumber,
    alternateMobileNumber: member.alternateMobileNumber,
    age: member.age,
    displayName: member.displayName,
    districtName: member.districtName,
    farmerCategory: member.farmerCategory,
    gender: member.gender,
    id: member.id ?? member.userId,
    locationName: member.locationName ?? member.village,
    memberId: member.memberId,
    memberNumber: member.memberNumber,
    mobileNumber: member.mobileNumber,
    name: member.name ?? member.displayName,
    phone: member.phone ?? member.mobileNumber,
    region: member.region ?? member.village,
    siteName: member.siteName ?? member.taluka,
    status: member.status ?? "Inactive",
    stateName: member.stateName,
    taluka: member.taluka,
    tenantId: member.tenantId,
    userId: member.userId,
    username: member.username,
    village: member.village
  };
}

function normalizeCreateInput(input: CreateFpoMemberInput): CreateFpoMemberInput {
  return {
    ...input,
    aadhaarNumber: input.aadhaarNumber?.trim(),
    age: input.age?.trim(),
    alternateMobileNumber: input.alternateMobileNumber?.trim(),
    displayName: input.displayName.trim(),
    districtName: input.districtName?.trim(),
    farmerCategory: input.farmerCategory?.trim(),
    gender: input.gender?.trim(),
    memberNumber: input.memberNumber.trim(),
    mobileNumber: input.mobileNumber.trim(),
    password: input.password,
    stateName: input.stateName.trim(),
    taluka: input.taluka.trim(),
    username: input.username.trim(),
    village: input.village.trim()
  };
}

function normalizeUpdateInput(input: UpdateFpoMemberInput): UpdateFpoMemberInput {
  return {
    aadhaarNumber: input.aadhaarNumber?.trim(),
    age: input.age?.trim(),
    alternateMobileNumber: input.alternateMobileNumber?.trim(),
    displayName: input.displayName.trim(),
    districtName: input.districtName?.trim(),
    farmerCategory: input.farmerCategory?.trim(),
    gender: input.gender?.trim(),
    memberNumber: input.memberNumber.trim(),
    mobileNumber: input.mobileNumber.trim(),
    stateName: input.stateName.trim(),
    taluka: input.taluka.trim(),
    village: input.village.trim()
  };
}

function toCreateRequest(input: CreateFpoMemberInput): CreateFpoMemberRequest {
  return {
    aadhaarNumber: input.aadhaarNumber || undefined,
    age: input.age ? Number(input.age) : undefined,
    alternateMobileNumber: input.alternateMobileNumber || undefined,
    displayName: input.displayName,
    districtName: input.districtName || undefined,
    farmerCategory: input.farmerCategory || undefined,
    gender: input.gender || undefined,
    memberNumber: input.memberNumber,
    mobileNumber: input.mobileNumber,
    password: input.password,
    status: "ACTIVE",
    stateName: input.stateName,
    taluka: input.taluka,
    username: input.username,
    village: input.village
  };
}

function toUpdateRequest(
  member: FpoMember,
  input: UpdateFpoMemberInput
): UpdateFpoMemberRequest {
  return {
    aadhaarNumber: input.aadhaarNumber || undefined,
    age: input.age ? Number(input.age) : undefined,
    alternateMobileNumber: input.alternateMobileNumber || undefined,
    displayName: input.displayName,
    districtName: input.districtName || undefined,
    farmerCategory: input.farmerCategory || undefined,
    gender: input.gender || undefined,
    memberNumber: input.memberNumber,
    mobileNumber: input.mobileNumber,
    status: toBackendMemberStatus(member.status),
    stateName: input.stateName,
    taluka: input.taluka,
    village: input.village
  };
}

function toFrontendStatus(status: FpoMemberStatus): UserStatus {
  if (status === "ACTIVE") {
    return "Active";
  }

  return status === "SUSPENDED" ? "Suspended" : "Inactive";
}

function toBackendStatus(status: Extract<UserStatus, "Active" | "Inactive" | "Suspended">) {
  if (status === "Active") {
    return "ACTIVE";
  }

  return status === "Suspended" ? "SUSPENDED" : "INACTIVE";
}

function toBackendMemberStatus(status: UserStatus): FpoMemberStatus {
  if (status === "Active") {
    return "ACTIVE";
  }

  return status === "Suspended" ? "SUSPENDED" : "INACTIVE";
}

async function getAccessToken() {
  return AsyncStorage.getItem(storageKeys.auth.accessToken);
}

function canUseLocalFallback(error: unknown) {
  return !(error instanceof ApiClientError);
}

function toFpoMemberError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 409) {
      return new AppError("DUPLICATE_RESOURCE", error.message);
    }

    if (error.status === 400) {
      return new AppError("VALIDATION_FAILED", error.message);
    }

    if (error.status === 401 || error.status === 403) {
      return new AppError("ACCESS_DENIED", error.message);
    }

    return new AppError("API_REQUEST_FAILED", error.message);
  }

  return new AppError("API_REQUEST_FAILED", "Unable to manage FPO members.");
}
