import AsyncStorage from "@react-native-async-storage/async-storage";

import { ApiClientError } from "../core/api/client";
import { AppError } from "../core/errors/AppError";
import { logger } from "../core/logging/logger";
import { Account, UserRole } from "../core/model/types";
import { readJsonArray } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { loginWithBackend, toFrontendRole } from "./backendAuth";

export type Role = UserRole;

export async function login(username: string, password: string): Promise<Role> {
  const backendRole = await tryBackendLogin(username, password);

  if (backendRole) {
    return backendRole;
  }

  const localAccounts = await getLocalAccounts();
  const account = localAccounts.find(
    (item) =>
      item.username.toLowerCase() === username.trim().toLowerCase() &&
      item.password === password
  );

  if (!account) {
    throw new AppError("AUTH_INVALID_CREDENTIALS", "Invalid username or password.");
  }

  await AsyncStorage.setItem(storageKeys.auth.sessionRole, account.role);
  await AsyncStorage.setItem(storageKeys.auth.sessionUsername, account.username);
  return account.role;
}

export async function getSavedRole(): Promise<Role | null> {
  const savedRole =
    (await AsyncStorage.getItem(storageKeys.auth.sessionRole)) ??
    (await AsyncStorage.getItem(storageKeys.legacy.auth.sessionRole));

  return normalizeRole(savedRole);
}

export async function logout() {
  await Promise.all([
    AsyncStorage.removeItem(storageKeys.auth.accessToken),
    AsyncStorage.removeItem(storageKeys.auth.refreshToken),
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
  const savedAccounts = await readJsonArray<Partial<Account> & { role?: string }>([
    storageKeys.auth.localAccounts,
    storageKeys.legacy.auth.localAccounts
  ]);

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

async function tryBackendLogin(
  username: string,
  password: string
): Promise<Role | null> {
  try {
    const response = await loginWithBackend(username.trim(), password);
    const role = toFrontendRole(response.roles);

    await Promise.all([
      AsyncStorage.setItem(storageKeys.auth.accessToken, response.accessToken),
      AsyncStorage.setItem(storageKeys.auth.refreshToken, response.refreshToken),
      AsyncStorage.setItem(storageKeys.auth.sessionRole, role),
      AsyncStorage.setItem(storageKeys.auth.sessionUsername, username.trim())
    ]);

    return role;
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 401) {
      throw new AppError("AUTH_INVALID_CREDENTIALS", "Invalid username or password.");
    }

    logger.warn("Backend login unavailable; using local auth fallback.", {
      message: error instanceof Error ? error.message : "Unknown backend error"
    });
    return null;
  }
}
