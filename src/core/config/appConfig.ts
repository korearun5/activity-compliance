declare const process:
  | {
      env?: Record<string, string | undefined>;
    }
  | undefined;

const env = typeof process === "undefined" ? {} : (process.env ?? {});

function parsePublicList(value: string | undefined, fallback: string[]) {
  if (value === undefined) {
    return fallback;
  }

  const parsed = value
    .split(",")
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);

  return parsed.length ? parsed : fallback;
}

export const appConfig = {
  appName: env.EXPO_PUBLIC_APP_NAME ?? "Activity Compliance",
  apiBaseUrl: env.EXPO_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  apiVersion: env.EXPO_PUBLIC_API_VERSION ?? "v1",
  defaultTenantCode: env.EXPO_PUBLIC_DEFAULT_TENANT_CODE ?? "default",
  defaultLocale: env.EXPO_PUBLIC_DEFAULT_LOCALE ?? "en-IN",
  enabledClientModules: parsePublicList(env.EXPO_PUBLIC_ENABLED_CLIENT_MODULES, [
    "carbon"
  ]),
  proofEditWindowMs: 24 * 60 * 60 * 1000,
  storageNamespace: env.EXPO_PUBLIC_STORAGE_NAMESPACE ?? "activity-platform"
} as const;
