import AsyncStorage from "@react-native-async-storage/async-storage";

import { AppError } from "../core/errors/AppError";
import { Account, UserProfileInput, UserRole } from "../core/model/types";
import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { registerUser } from "../data/adminRegistryStore";
import { saveUserProfile } from "../data/farmerProfileStore";

export type Role = UserRole;

const accounts: Account[] = [
  { username: "admin", password: "admin123", role: "admin" },
  { username: "user", password: "user123", role: "participant" }
];

export async function login(username: string, password: string): Promise<Role> {
  const localAccounts = await getLocalAccounts();
  const account = [...accounts, ...localAccounts].find(
    (item) =>
      item.username.toLowerCase() === username.trim().toLowerCase() &&
      item.password === password
  );

  if (!account) {
    throw new AppError(
      "AUTH_INVALID_CREDENTIALS",
      "Invalid username or password."
    );
  }

  await AsyncStorage.setItem(storageKeys.auth.sessionRole, account.role);
  await AsyncStorage.setItem(storageKeys.auth.sessionUsername, account.username);
  return account.role;
}

export async function signupParticipant({
  username,
  password,
  profile
}: {
  username: string;
  password: string;
  profile: UserProfileInput;
}): Promise<Role> {
  const trimmedUsername = username.trim();
  const allAccounts = [...accounts, ...(await getLocalAccounts())];
  const usernameExists = allAccounts.some(
    (account) =>
      account.username.toLowerCase() === trimmedUsername.toLowerCase()
  );

  if (usernameExists) {
    throw new AppError("AUTH_USERNAME_TAKEN", "This username is already taken.");
  }

  const nextAccounts = [
    ...(await getLocalAccounts()),
    { username: trimmedUsername, password, role: "participant" as const }
  ];

  await writeJson(storageKeys.auth.localAccounts, nextAccounts);
  await saveUserProfile(trimmedUsername, profile);
  await registerUser(trimmedUsername, profile);
  await AsyncStorage.setItem(storageKeys.auth.sessionRole, "participant");
  await AsyncStorage.setItem(storageKeys.auth.sessionUsername, trimmedUsername);

  return "participant";
}

export async function getSavedRole(): Promise<Role | null> {
  const savedRole =
    (await AsyncStorage.getItem(storageKeys.auth.sessionRole)) ??
    (await AsyncStorage.getItem(storageKeys.legacy.auth.sessionRole));

  return normalizeRole(savedRole);
}

export async function logout() {
  await Promise.all([
    AsyncStorage.removeItem(storageKeys.auth.sessionRole),
    AsyncStorage.removeItem(storageKeys.auth.sessionUsername),
    AsyncStorage.removeItem(storageKeys.legacy.auth.sessionRole),
    AsyncStorage.removeItem(storageKeys.legacy.auth.sessionUsername)
  ]);
}

export async function getSavedUsername() {
  return (
    (await AsyncStorage.getItem(storageKeys.auth.sessionUsername)) ??
    AsyncStorage.getItem(storageKeys.legacy.auth.sessionUsername)
  );
}

async function getLocalAccounts(): Promise<Account[]> {
  const savedAccounts = await readJsonArray<Partial<Account> & { role?: string }>(
    [storageKeys.auth.localAccounts, storageKeys.legacy.auth.localAccounts]
  );

  return savedAccounts
    .map((account) => {
      const role = normalizeRole(account.role ?? null);

      if (
        !role ||
        typeof account.username !== "string" ||
        typeof account.password !== "string"
      ) {
        return null;
      }

      return {
        password: account.password,
        role,
        username: account.username
      };
    })
    .filter((account): account is Account => Boolean(account));
}

function normalizeRole(role: string | null): Role | null {
  if (role === "admin" || role === "supervisor" || role === "participant") {
    return role;
  }

  if (role === "user") {
    return "participant";
  }

  return null;
}

export const signupFarmer = signupParticipant;
