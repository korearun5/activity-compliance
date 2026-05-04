import { ManagedUser, UserProfileInput } from "../core/model/types";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { CropCycle, ProofSubmission } from "./farmDemoData";

export type RegisteredFarmer = ManagedUser & {
  name: string;
  region: string;
  village: string;
};

export async function registerUser(username: string, profile: UserProfileInput) {
  const currentFarmers = await getRegisteredFarmers();
  const nextFarmer: RegisteredFarmer = {
    displayName: profile.displayName,
    locationName: profile.locationName,
    name: profile.displayName,
    phone: profile.phone,
    region: profile.locationName,
    siteName: profile.siteName,
    status: "Active",
    username,
    village: profile.siteName
  };
  const nextFarmers = [
    nextFarmer,
    ...currentFarmers.filter(
      (farmer) => farmer.username.toLowerCase() !== username.toLowerCase()
    )
  ];

  await writeJson(storageKeys.registry.users, nextFarmers);
  return nextFarmers;
}

export async function getRegisteredFarmers(): Promise<RegisteredFarmer[]> {
  const savedFarmers = await readJsonArray<Partial<RegisteredFarmer>>([
    storageKeys.registry.users,
    storageKeys.legacy.registry.users
  ]);

  return savedFarmers
    .map(toRegisteredFarmer)
    .filter((farmer): farmer is RegisteredFarmer => Boolean(farmer));
}

export function countFarmerCycles(
  farmer: RegisteredFarmer,
  cycles: CropCycle[],
  status: "running" | "completed"
) {
  return cycles.filter(
    (cycle) =>
      cycle.status === status &&
      ((cycle.farmerUsername &&
        cycle.farmerUsername.toLowerCase() === farmer.username.toLowerCase()) ||
        cycle.region === farmer.region)
  ).length;
}

export function countFarmerProofs(
  farmer: RegisteredFarmer,
  proofs: ProofSubmission[]
) {
  return proofs.filter(
    (proof) =>
      proof.farmer === farmer.name ||
      proof.farmerUsername?.toLowerCase() === farmer.username.toLowerCase() ||
      (proof.region === farmer.region && proof.status === "done")
  ).length;
}

function toRegisteredFarmer(
  farmer: Partial<RegisteredFarmer>
): RegisteredFarmer | null {
  const username = farmer.username;
  const displayName = farmer.displayName ?? farmer.name;
  const locationName = farmer.locationName ?? farmer.region;
  const siteName = farmer.siteName ?? farmer.village;

  if (
    typeof username !== "string" ||
    typeof displayName !== "string" ||
    typeof locationName !== "string" ||
    typeof siteName !== "string" ||
    typeof farmer.phone !== "string"
  ) {
    return null;
  }

  return {
    displayName,
    locationName,
    name: displayName,
    phone: farmer.phone,
    region: locationName,
    siteName,
    status: farmer.status ?? "Profile pending",
    username,
    village: siteName
  };
}

export const registerFarmer = registerUser;

