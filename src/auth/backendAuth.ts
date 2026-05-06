import { endpoints } from "../core/api/endpoints";
import { getJson, postJson } from "../core/api/client";
import { appConfig } from "../core/config/appConfig";
import { UserRole } from "../core/model/types";

export type BackendRole = "ADMIN" | "SUPERVISOR" | "PARTICIPANT";

export type BackendLoginRequest = {
  password: string;
  tenantCode: string;
  username: string;
};

export type BackendLoginResponse = {
  accessToken: string;
  expiresAt: string;
  refreshToken: string;
  roles: BackendRole[];
  tenantId: string;
  userId: string;
};

export type BackendCurrentUser = {
  displayName: string;
  roles: BackendRole[];
  tenantId: string;
  userId: string;
  username: string;
};

export async function loginWithBackend(
  username: string,
  password: string
): Promise<BackendLoginResponse> {
  return postJson<BackendLoginRequest, BackendLoginResponse>(
    endpoints.auth.login,
    {
      password,
      tenantCode: appConfig.defaultTenantCode,
      username
    }
  );
}

export async function getBackendCurrentUser(accessToken: string) {
  return getJson<BackendCurrentUser>(endpoints.auth.me, { accessToken });
}

export function toFrontendRole(roles: BackendRole[]): UserRole {
  if (roles.includes("ADMIN")) {
    return "admin";
  }

  if (roles.includes("SUPERVISOR")) {
    return "supervisor";
  }

  return "participant";
}
