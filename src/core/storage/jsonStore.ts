import AsyncStorage from "@react-native-async-storage/async-storage";

import { logger } from "../logging/logger";

type StorageKeyInput = string | string[];

function toKeyList(keys: StorageKeyInput) {
  return Array.isArray(keys) ? keys : [keys];
}

export async function readJson<T>(
  keys: StorageKeyInput,
  fallback: T
): Promise<T> {
  for (const key of toKeyList(keys)) {
    const savedValue = await AsyncStorage.getItem(key);

    if (!savedValue) {
      continue;
    }

    try {
      return JSON.parse(savedValue) as T;
    } catch {
      logger.warn("Failed to parse local storage JSON.", { key });
    }
  }

  return fallback;
}

export async function readJsonArray<T>(keys: StorageKeyInput): Promise<T[]> {
  const parsed = await readJson<unknown>(keys, []);
  return Array.isArray(parsed) ? (parsed as T[]) : [];
}

export async function writeJson<T>(key: string, value: T) {
  await AsyncStorage.setItem(key, JSON.stringify(value));
}

