import { apiClient } from "../core/api/client";
import { endpoints } from "../core/api/endpoints";
import { UserStatus } from "../core/model/types";
import {
  CarbonProfileRecord,
  listCarbonProfiles
} from "../modules/carbon/data/carbonProfileStore";
import { FpoMember, getFpoMembers } from "./fpoMemberStore";

export type ActivityParticipantSource = "carbon" | "fpo" | "platform";

export type ActivityParticipant = {
  displayName: string;
  id: string;
  locationName: string;
  memberNumber?: string;
  mobileNumber?: string;
  profileId: string;
  siteName: string;
  sources: ActivityParticipantSource[];
  status: UserStatus;
  tenantId?: string;
  username?: string;
};

export type LoadActivityParticipantsOptions = {
  fpoMembers?: FpoMember[];
  includeCarbon: boolean;
  includeFpo: boolean;
  useBackend?: boolean;
};

type BackendFarmerParticipantStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED";

type BackendFarmerParticipant = {
  districtName: string;
  displayName: string;
  farmerProfileId: string;
  mobileNumber: string;
  stateName: string;
  status: BackendFarmerParticipantStatus;
  taluka: string;
  userId: string;
  username: string;
  village: string;
};

export async function getActivityParticipants({
  fpoMembers,
  includeCarbon,
  includeFpo,
  useBackend = false
}: LoadActivityParticipantsOptions) {
  if (useBackend) {
    return getBackendActivityParticipants();
  }

  const [members, carbonProfiles] = await Promise.all([
    includeFpo ? Promise.resolve(fpoMembers ?? getFpoMembers()) : Promise.resolve([]),
    includeCarbon ? listCarbonProfiles() : Promise.resolve([])
  ]);

  return mergeActivityParticipants([
    ...members.map(toActivityParticipantFromFpoMember),
    ...carbonProfiles
      .map(toActivityParticipantFromCarbonProfile)
      .filter((participant): participant is ActivityParticipant => Boolean(participant))
  ]);
}

export function toActivityParticipantFromFpoMember(
  member: FpoMember
): ActivityParticipant {
  return {
    displayName: member.name,
    id: member.userId,
    locationName: member.locationName,
    memberNumber: member.memberNumber,
    mobileNumber: member.mobileNumber,
    profileId: member.memberId,
    siteName: member.siteName,
    sources: ["fpo"],
    status: member.status,
    tenantId: member.tenantId,
    username: member.username
  };
}

export async function getBackendActivityParticipants() {
  const participants = await apiClient.get<BackendFarmerParticipant[]>(
    endpoints.farmers.participants
  );

  return participants.map(toActivityParticipantFromBackendFarmer);
}

export function toActivityParticipantFromBackendFarmer(
  participant: BackendFarmerParticipant
): ActivityParticipant {
  return {
    displayName: participant.displayName,
    id: participant.userId,
    locationName: participant.village || participant.taluka || "Location not set",
    mobileNumber: participant.mobileNumber,
    profileId: participant.farmerProfileId,
    siteName:
      [participant.taluka, participant.districtName].filter(Boolean).join(" / ") ||
      "Farmer profile",
    sources: ["platform"],
    status: toUserStatus(participant.status),
    username: participant.username
  };
}

export function toActivityParticipantFromCarbonProfile(
  profile: CarbonProfileRecord
): ActivityParticipant | null {
  if (!profile.userId || profile.participantType !== "FARMER") {
    return null;
  }

  return {
    displayName: profile.displayName,
    id: profile.userId,
    locationName: profile.village ?? profile.taluka ?? "Location not set",
    memberNumber: profile.memberNumber,
    mobileNumber: profile.mobileNumber,
    profileId: profile.id,
    siteName:
      [profile.taluka, profile.districtName].filter(Boolean).join(" / ") ||
      "Carbon enrollment",
    sources: ["carbon"],
    status: toUserStatus(profile.status),
    tenantId: profile.tenantId,
    username: profile.username
  };
}

export function mergeActivityParticipants(participants: ActivityParticipant[]) {
  const byUserId = new Map<string, ActivityParticipant>();

  participants.forEach((participant) => {
    const existing = byUserId.get(participant.id);

    if (!existing) {
      byUserId.set(participant.id, participant);
      return;
    }

    byUserId.set(participant.id, {
      ...existing,
      displayName: existing.displayName || participant.displayName,
      locationName:
        existing.locationName === "Location not set"
          ? participant.locationName
          : existing.locationName,
      memberNumber: existing.memberNumber ?? participant.memberNumber,
      mobileNumber: existing.mobileNumber ?? participant.mobileNumber,
      siteName:
        existing.siteName === "Carbon enrollment"
          ? participant.siteName
          : existing.siteName,
      sources: uniqueSources([...existing.sources, ...participant.sources]),
      status: existing.status === "Active" ? existing.status : participant.status,
      username: existing.username ?? participant.username
    });
  });

  return [...byUserId.values()].sort((left, right) =>
    left.displayName.localeCompare(right.displayName)
  );
}

function uniqueSources(sources: ActivityParticipantSource[]) {
  return sources.filter((source, index) => sources.indexOf(source) === index);
}

function toUserStatus(
  status: CarbonProfileRecord["status"] | BackendFarmerParticipantStatus
): UserStatus {
  if (status === "ACTIVE") {
    return "Active";
  }

  return status === "SUSPENDED" ? "Suspended" : "Inactive";
}
